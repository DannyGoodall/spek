## Context

`SpekToolWindowFactory.createToolWindowContent()` 目前把 `SpekTreePanel` 與 `SpekBrowserPanel.component` 塞進一個 `JSplitPane(VERTICAL_SPLIT)`，`resizeWeight = 0.3`，外面再包一層 `JPanel(BorderLayout())`。這段程式每個專案只跑一次，沒有任何狀態被保存，因此：

- 使用者無法隱藏樹狀面板，只能把分隔線拖到頂端。
- 拖曳結果不會被記住，重開專案就回到 30%。

有幾個既有結構會約束設計：

- **`SpekProjectState` 是純記憶體 `@Service`**，只有 `hasOpenSpec` 一個欄位，由 `SpekStartupActivity` 在每次專案開啟時寫入，並被 `OpenSpekAction.update()` 讀取來決定 action 是否可用。這個欄位**必須維持 transient** — 把它寫進磁碟，會讓一個已經移除 `openspec/` 的專案在重開後仍然以為自己有 OpenSpec 內容。
- **`SpekTreePanel.refresh()` 不是從 EDT 被呼叫的**。呼叫鏈是 `SpekBrowserPanel.scheduleRefresh()` → `java.util.Timer` 執行緒 → `notifyWebviewFileChanged()` → `onFileChanged?.invoke()` → `treePanel.refresh()`。任何「樹是否可見」的判斷若在這條路徑上讀 Swing 狀態，就是跨執行緒讀 EDT 狀態。
- **`SpekTreePanel.init` 在 EDT 上同步呼叫 `SpekTreeModel.build(projectPath)`**，也就是 Tool Window 建立時會在 EDT 掃一次磁碟。
- **JCEF browser 是重量級原生元件**。任何會把 `jcefBrowser.component` 從一個 container 搬到另一個 container 的做法都有閃爍或重新載入的風險。

## Goals / Non-Goals

**Goals:**

- 使用者能以一次點擊隱藏 / 顯示樹狀面板，且該選擇跨 IDE 重啟保留。
- 樹隱藏時，webview 佔滿整個 Tool Window，且分隔線消失。
- 保留樹的使用者，其分隔線比例也被記住。
- 樹隱藏時不付出建立 tree model 的成本；重新顯示時內容不得過時。
- 預設行為不變（樹可見），既有使用者不需要做任何事。

**Non-Goals:**

- 不改變樹本身的內容、排序或 double-click 導覽行為。
- 不動 VS Code extension — 它的 TreeView 已可由 VS Code 原生隱藏。
- 不提供「隱藏 webview、只留樹」的反向配置。webview 是主要介面。
- 不把偏好同步進版控。這是個人的視窗佈局偏好。
- 不重構 `SpekBrowserPanel` 的 file watcher。

## Decisions

### 決策 1：以 `JBSplitter` 取代 `JSplitPane`，用 `isVisible` 隱藏樹

`JSplitPane` 在子元件 `isVisible = false` 時**不會**把空間讓給另一個子元件，分隔線也還在。IntelliJ 的 `Splitter`（`JBSplitter` 的基底）則在 `doLayout()` 中檢查子元件的可見性，只有一個可見時就讓它佔滿並隱藏 divider。這正是我們要的語意，所以隱藏動作就只是 `treePanel.isVisible = false`。

同時 `JBSplitter(vertical = true, proportionKey, defaultProportion)` 會自動載入 / 儲存分隔線比例，順手解掉「比例不持久化」那一半的問題。

**替代方案**：把 Tool Window content 的元件在「splitter」與「裸 webview」之間抽換。否決 — 這會把 `jcefBrowser.component` 重新 parent，對重量級原生 surface 有閃爍甚至重新載入的風險，而且要處理 `Disposer` 的註冊關係。用 `isVisible` 完全不碰 JCEF 元件的 container。

**替代方案**：保留 `JSplitPane`，隱藏時把 divider location 設為 0。否決 — divider 仍在、仍可被拖回來，而且樹雖然看不見卻還是活著（仍會被 refresh），等於沒解決問題。

### 決策 2：`SpekProjectState` 實作 `PersistentStateComponent`，儲存位置為 workspace 檔

以巢狀 state class 承載持久化欄位，讓 `hasOpenSpec` 留在 service 本體上、天然排除於序列化之外：

```kotlin
@Service(Service.Level.PROJECT)
@State(name = "SpekProjectState", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class SpekProjectState : PersistentStateComponent<SpekProjectState.State> {
    data class State(var treeVisible: Boolean = true)
    private var state = State()
    var hasOpenSpec: Boolean = false   // transient，不進 state
    ...
}
```

用 `StoragePathMacros.WORKSPACE_FILE`（`.idea/workspace.xml`）而非自訂的 `spek.xml`：視窗佈局偏好是個人的，`workspace.xml` 慣例上被 gitignore，而 `.idea/` 底下的自訂檔案在某些 repo 會被 commit，導致一個人隱藏樹就強加給全隊。

**替代方案**：`PropertiesComponent.getInstance(project)`。否決 — 雖然一行就能寫完，但它是無型別的字串 key-value bag；`SpekProjectState` 已經是這個 plugin 的專案狀態的自然歸屬，用型別化的 state class 也讓預設值有單一來源。

**替代方案**：存成 application 層級（一次關閉、所有專案都關）。否決 — 「這個專案我用 webview 導覽就夠了」是隨專案而異的判斷，不是使用者的全域習慣；per-project 也讓一個以 spec 為主的專案可以繼續留著樹。

### 決策 3：把「延後刷新」抽成純邏輯類別 `TreeRefreshGate`

`refresh()` 從 Timer 執行緒進來，不能讀 Swing 的 `isVisible`。因此可見性以一個獨立的、執行緒安全的閘門表達：

```kotlin
class TreeRefreshGate(visible: Boolean) {
    fun requestRefresh(): Boolean      // true = 現在就刷新；false = 已記錄為 pending
    fun setVisible(visible: Boolean): Boolean  // true = 需要補一次刷新
}
```

`SpekTreePanel.refresh()` 先問閘門要不要真的做；`setTreeVisible()` 轉發給閘門，若回報有 pending 就補刷一次。這條規則（隱藏時記 dirty、顯示時補刷）是這個 change 唯一有實質行為的邏輯，抽出來就能在沒有 IntelliJ platform test fixture 的情況下做 unit test。這也和 repo 既有的模式一致 — `WatchPolling.decidePolling` 與 `SchemaOrder.parseOrderFromStatus` 都是為了可測性而抽出的純函式。

順帶處理啟動成本：若偏好是隱藏，`SpekTreePanel` 建構時就不呼叫 `SpekTreeModel.build()`，直接把閘門初始化為「不可見 + 已 dirty」。使用者第一次打開樹時才掃磁碟。這順便消掉 Tool Window 建立時 EDT 上的那次同步磁碟掃描。

**替代方案**：在 `SpekToolWindowFactory` 裡用 `if (visible) treePanel.refresh()` 包住 `onFileChanged`。否決 — 把狀態散到 factory，且 factory 沒有辦法知道「隱藏期間錯過了幾次刷新」。

### 決策 4：單一 `ToggleAction`，同時掛在標題列與 ⋮ gear 選單

同一個 `ToggleTreePanelAction` 實例註冊兩處：

```kotlin
val toggle = ToggleTreePanelAction(project) { visible -> treePanel.setTreeVisible(visible) }
toolWindow.setTitleActions(listOf(toggle))
toolWindow.setAdditionalGearActions(DefaultActionGroup(toggle))
```

標題列給的是永遠可見、一次點擊、以按下狀態顯示目前設定的圖示；gear 選單給的是有文字標籤（`Show Specs and Changes Tree`）、附勾選狀態的條目，讓不確定那個圖示是什麼的人也找得到。兩處共用同一個 action 實例是安全的 — `AnAction` 的 template presentation 共用，但每個 place 會各自複製一份 `Presentation`。

`isSelected` / `setSelected` 直接讀寫 `SpekProjectState`，再呼叫建構時傳入的 callback 套用到 `SpekTreePanel`。必須覆寫 `getActionUpdateThread()` 回傳 `ActionUpdateThread.BGT`（2023.3+ 未覆寫會被 Plugin Verifier 標記；`isSelected` 只讀 service 不碰 Swing，BGT 安全）。

圖示採用 `AllIcons.Actions.ShowAsTree` — 語意最貼近「這顆按鈕控制的是那棵樹」，且是 platform 內建圖示，自動跟隨 IDE 主題與 HiDPI。

action 文字**不得含 `&`**：`Presentation.setText(String)` 預設會把 `&` 當成 mnemonic 標記解析掉。`"Show Specs & Changes Tree"` 會變成 `"Show Specs  Changes Tree"`，還會把後面的空白（ASCII 32）綁成快捷鍵，選單裡因此出現一個游離的底線。故 label 用 `and` 而非 `&`（`&&` 雖可跳脫，但留下一個需要註解才看得懂的字串）。此規則以 `ToggleTreePanelActionTest` 迴歸測試釘住。

**替代方案**：只放 gear 選單。否決 — 藏在兩層點擊後面，正是使用者「找不到怎麼關掉它」的路徑。**替代方案**：只放標題列。否決 — 純圖示的可發現性依賴 tooltip；gear 選單的文字條目補上這個缺口，成本只有一行。

**替代方案**：在 `plugin.xml` 註冊為具名 action 以支援快捷鍵 / Tools 選單。不在此 change 範圍，但 action class 本身不阻擋日後加上去。

action 直接持有套用用的 callback（於 `createToolWindowContent` 內建構，閉包捕捉 `SpekTreePanel`）。這個參考的生命週期綁在 Tool Window，而 Tool Window 隨專案關閉而釋放，因此不需要額外的 disposer。**替代方案**是讓 action 只寫 state 並在專案 message bus 上發佈事件，由 factory 訂閱後套用；那樣更解耦，但為了一個 boolean 引入一個 topic 不划算。

## Risks / Trade-offs

- **`JBSplitter` 的 `splitterProportionKey` 是 application 層級的**（比例存在 `PropertiesComponent.getInstance()`，不是 project 層級）→ 分隔線比例會跨專案共用。這與 platform 內建多數 tool window 的行為一致，且比例是「我喜歡樹佔多高」而非專案屬性，因此接受。若日後有人抱怨，改成自己把 proportion 存進 `SpekProjectState.State` 即可，不影響本設計其餘部分。

- **依賴 `Splitter` 對不可見子元件的處理行為** → 若某個 IDE 版本的 `Splitter.doLayout()` 沒有把空間全數讓給可見的子元件，樹隱藏後會留下一塊空白。以 `./gradlew runIde` 在最低支援版本（2023.3）與最新版各驗一次；後備做法是隱藏時改設 `splitter.firstComponent = null`、顯示時再設回去（元件本身仍不重建，JCEF 不受影響）。

- **`hasOpenSpec` 被誤序列化** → 一個已移除 `openspec/` 的專案在重開後仍讓 "Open spek" action 可用。以巢狀 state class 從結構上防止（只有 `State` 內的欄位會被序列化），並在 code review 時明確檢查。

- **隱藏期間的 dirty 標記與顯示動作的競態** → `requestRefresh()` 在 Timer 執行緒、`setVisible()` 在 EDT，兩者都碰同一個旗標。`TreeRefreshGate` 內部以 `synchronized` 保護，且 `setVisible(true)` 回傳「是否需要補刷」而不是讓呼叫端自己讀旗標再決定，避免 check-then-act。

- **樹隱藏時失去 double-click 導覽** → 使用者改用 webview 內的清單導覽，這正是 issue #12 的前提（"richer functionality from the section below it"）。無需緩解。

- **偏好存在 `workspace.xml`，不隨 VCS 分享** → 這是刻意的。代價是同一位使用者在新機器 clone 專案後要再關一次樹。

## Migration Plan

沒有資料遷移。舊的 `workspace.xml` 不含 `SpekProjectState` 元件，反序列化時 `treeVisible` 取預設值 `true`，行為與現況完全相同。

Rollback 即還原 commit。殘留在 `workspace.xml` 的 `<component name="SpekProjectState">` 區塊會被舊版忽略，無副作用。

## Open Questions

無。原先的三個問題已裁示，結論寫入上方決策：

- **toggle 圖示** → `AllIcons.Actions.ShowAsTree`（決策 4）。實機視覺重量仍待 `./gradlew runIde` 確認，若過重再換一顆 platform 圖示，不影響其餘設計。
- **是否加進 ⋮ gear 選單** → 加。標題列圖示與 gear 選單條目共用同一個 action 實例（決策 4）。
- **偏好層級** → per-project，存於 `.idea/workspace.xml`（決策 2）。
