## 1. 持久化偏好

- [x] 1.1 將 `SpekProjectState` 改為 `PersistentStateComponent<SpekProjectState.State>`，加上 `@State(name = "SpekProjectState", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])`
- [x] 1.2 定義巢狀 `data class State(var treeVisible: Boolean = true)`，並確保 `hasOpenSpec` 留在 service 本體上（不進 `State`，不被序列化）
- [x] 1.3 提供 `treeVisible` 的讀寫存取子，轉發到內部 `state`
- [x] 1.4 加 unit test：`State()` 預設 `treeVisible == true`；`State` 的可序列化欄位不含 `hasOpenSpec`

## 2. TreeRefreshGate 純邏輯

- [x] 2.1 新增 `packages/intellij/src/main/kotlin/com/spek/intellij/tree/TreeRefreshGate.kt`，含 `requestRefresh(): Boolean` 與 `setVisible(visible: Boolean): Boolean`，內部以 `synchronized` 保護 dirty 旗標
- [x] 2.2 建構子接受初始可見性；初始為隱藏時，dirty 旗標起始為 true（首次顯示必刷）
- [x] 2.3 加 unit test（`src/test/kotlin/com/spek/intellij/tree/TreeRefreshGateTest.kt`）：可見時 `requestRefresh()` 回 true；隱藏時回 false 且記為 pending；隱藏期間多次 `requestRefresh()` 只在顯示時補刷一次；無 pending 時 `setVisible(true)` 回 false；`setVisible(false)` 一律回 false；初始隱藏時首次 `setVisible(true)` 回 true

## 3. SpekTreePanel 接上閘門

- [x] 3.1 `SpekTreePanel` 建構子新增 `initiallyVisible: Boolean` 參數，內部持有 `TreeRefreshGate`
- [x] 3.2 初始為隱藏時跳過 `SpekTreeModel.build(projectPath)`，以空 model 建立 `Tree`，並設 `isVisible = false`
- [x] 3.3 `refresh()` 改為先問 `gate.requestRefresh()`，回 false 就直接 return（此方法由 file watcher 的 Timer 執行緒呼叫，不得讀 Swing 狀態）
- [x] 3.4 新增 `setTreeVisible(visible: Boolean)`：在 EDT 設 `isVisible`，呼叫 `gate.setVisible(visible)`，回報需要補刷時執行一次 model 重建

## 4. Toggle action

- [x] 4.1 新增 `packages/intellij/src/main/kotlin/com/spek/intellij/actions/ToggleTreePanelAction.kt`，繼承 `ToggleAction`，建構子接受 `Project` 與 `(Boolean) -> Unit` 套用 callback
- [x] 4.2 `isSelected` / `setSelected` 讀寫 `SpekProjectState.treeVisible`，`setSelected` 額外呼叫套用 callback
- [x] 4.3 覆寫 `getActionUpdateThread()` 回傳 `ActionUpdateThread.BGT`
- [x] 4.4 設定 template presentation：文字 `Show Specs and Changes Tree`（不可含 `&`，會被 Presentation 當成 mnemonic 標記吃掉）、圖示 `AllIcons.Actions.ShowAsTree`

## 5. Tool Window 佈局

- [x] 5.1 `SpekToolWindowFactory` 以 `JBSplitter(vertical = true, proportionKey = "spek.toolwindow.splitter", defaultProportion = 0.3f)` 取代 `JSplitPane`，移除多餘的 `JPanel(BorderLayout())` wrapper（`JBSplitter` 本身即可作為 content component）
- [x] 5.2 讀取 `SpekProjectState.treeVisible` 作為 `SpekTreePanel` 的 `initiallyVisible`，並據此設定其 `isVisible`
- [x] 5.3 建立單一 `ToggleTreePanelAction` 實例，同時傳給 `toolWindow.setTitleActions(listOf(toggle))` 與 `toolWindow.setAdditionalGearActions(DefaultActionGroup(toggle))`
- [x] 5.4 確認 `browserPanel.onFileChanged = { treePanel.refresh() }` 維持原樣（可見性判斷全數收斂在 `SpekTreePanel` 內）

## 6. 驗證

- [x] 6.1 `./gradlew test` 通過（含新增的 `TreeRefreshGateTest` 與 `SpekProjectState` 預設值測試）
- [x] 6.2 `./gradlew buildPlugin` 通過，無 deprecated / Plugin Verifier 相關編譯警告
- [x] 6.3 `./gradlew runIde` 手動驗：標題列圖示與 ⋮ 選單條目都能切換樹；隱藏時 webview 佔滿 Tool Window 且無 divider；切換時 webview 不重新載入
- [x] 6.4 `./gradlew runIde` 手動驗持久化：隱藏樹 → 關閉專案 → 重開，樹仍隱藏；拖曳 divider → 重開，比例保留
- [x] 6.5 `./gradlew runIde` 手動驗延後刷新：隱藏樹 → 於磁碟新增一個 change → 展開樹，新 change 出現
- [x] 6.6 在最低支援版本（2023.3）確認 `Splitter` 對不可見子元件確實把空間全數讓出；若否，改為隱藏時設 `splitter.firstComponent = null`、顯示時設回（不重建元件）
- [x] 6.7 手動驗 `hasOpenSpec` 未被持久化：於測試專案移除 `openspec/` → 重開專案 → "Open spek" action 應為停用

## 7. 文件

> CHANGELOG 三份與 `plugin.xml` 的 `<change-notes>` 皆由 `/release` skill 於發版時從 archived changes 統一產生，故不列入本 change。

- [x] 7.1 於 `packages/intellij/src/main/resources/META-INF/plugin.xml` 的 description 新增一則 feature 條目，說明樹狀導覽面板可隱藏且偏好持久化
- [x] 7.2 更新 `CLAUDE.md` 的 IntelliJ Plugin 章節，補上樹狀面板可由標題列 / gear 選單切換、偏好與分隔線比例持久化、隱藏期間延後刷新
