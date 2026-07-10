## Why

IntelliJ plugin 的 spek Tool Window 是寫死的 `JSplitPane`：Specs/Changes 樹狀面板永遠佔據上方約 30%，JCEF webview 只能用剩下的空間。使用者沒有任何方式可以隱藏樹，而且分隔線位置從未被持久化 — `createToolWindowContent` 在每次開啟專案時從頭重建整個 split，所以就算把分隔線拖走，下次開專案樹又回到 30%。

Issue #12（回報環境為 WebStorm、spek 1.5.0）講的正是這件事：*"I'm always collapsing it away, and I am afforded richer functionality from the section below it."* webview 本身已經提供 spec 瀏覽、change 詳情、搜尋、timeline 與關聯圖，對於習慣從 webview 導覽的使用者來說，樹狀面板在本來就狹窄的 Tool Window 裡是一筆永久的垂直空間開銷。

VS Code extension 沒有這個問題 — `spek.specsView` 與 `spek.changesView` 是原生 TreeView，VS Code 允許使用者從 view container 的右鍵選單直接隱藏。缺少這個操作方式的只有 IntelliJ。

## What Changes

- 在 spek Tool Window 標題列加上**顯示／隱藏樹狀面板的 toggle action**（透過 `ToolWindow.setTitleActions`）。樹被隱藏時，webview 面板佔滿整個 Tool Window 高度。
- **持久化樹的顯示偏好**（以專案為單位），讓選擇隱藏的使用者在重新開啟前都不會再看到樹。這才是 issue #12 真正的解法 — 目前那種「收合一次、重開又跑回來」的行為正是使用者疲乏的來源。
- **持久化分隔線比例**：把 `JSplitPane` 換成 IntelliJ 自家的 `JBSplitter` 並給定 `splitterProportionKey`。選擇保留樹的使用者，其自訂比例也會被記住，不會每次重設回 `resizeWeight = 0.3`。
- 將 `SpekProjectState` 從單純的 `@Service` 升級為 `PersistentStateComponent`，讓顯示偏好能跨 IDE 重啟保留。它目前只持有記憶體中的 `hasOpenSpec`。
- 樹**預設維持顯示**，既有使用者在主動關閉之前不會感受到任何變化。不是 breaking change。
- 樹被隱藏時跳過 tree model 重建：檔案監看的 refresh callback 不該為了一個沒人在看的面板付出成本；同時樹重新顯示時必須刷新，以免內容過時。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities

- `intellij-tree-view`：**Split pane layout** requirement 擴充 — Tool Window 新增標題列 toggle 用於隱藏／顯示樹狀面板，顯示偏好與分隔線比例皆跨 IDE 重啟持久化，樹隱藏時 webview 佔滿 Tool Window。**Tree auto-refresh on file changes** requirement 新增條件：樹隱藏期間延後刷新，於重新顯示時執行。
- `intellij-plugin-host`：**Tool Window registration** requirement 目前硬性宣告 Tool Window "SHALL display a vertical split pane containing the OpenSpec tree navigator on top and the JCEF webview content on the bottom"。需放寬為：webview 恆在，樹則依持久化的顯示偏好決定是否存在。

## Impact

- **`packages/intellij/src/main/kotlin/com/spek/intellij/SpekToolWindowFactory.kt`**：`JSplitPane` 換成帶 `splitterProportionKey` 的 `JBSplitter`；將 toggle action 接到 `toolWindow.setTitleActions(...)`；首次渲染時套用持久化的顯示偏好。
- **`packages/intellij/src/main/kotlin/com/spek/intellij/SpekProjectState.kt`**：以 `@State` / `@Storage` 實作 `PersistentStateComponent`，新增 `treeVisible: Boolean = true` 欄位。`hasOpenSpec` 維持 transient。
- **新增 `packages/intellij/src/main/kotlin/com/spek/intellij/actions/ToggleTreePanelAction.kt`**：一個讀寫 `SpekProjectState` 的 `ToggleAction`（或 `DumbAwareToggleAction`），以程式方式註冊為 Tool Window 標題列 action — 除非也要放進選單，否則不需要 `plugin.xml` 的 `<action>` 條目。
- **`packages/intellij/src/main/kotlin/com/spek/intellij/tree/SpekTreePanel.kt`**：新增「刷新待處理」旗標，讓隱藏中的樹延後重建 model。
- **`packages/intellij/src/main/resources/META-INF/plugin.xml`**：發版用的 `change-notes`；描述中的 `Specs Browser` 條目可提及此 toggle。
- **測試**：在既有的 `src/test/kotlin` 測試群中加上 Kotlin unit test，涵蓋持久化狀態的預設值與延後刷新旗標。Swing / Tool Window 的接線本身沒有 platform test fixture 就無法做 unit test，因此 toggle 以沙箱 IDE（`./gradlew runIde`）手動驗證。
- **文件**：root `CHANGELOG.md`（超集）與 `packages/intellij/CHANGELOG.md`。**不寫進 `packages/vscode/CHANGELOG.md`** — 依專案慣例，三份 CHANGELOG 共享同一份版本歷史但各自過濾掉與該發行通道無關的條目，而這是純 IntelliJ 的變更。`CLAUDE.md` 的 IntelliJ Plugin 章節補一句 toggle。`README.md` 未描述 Tool Window 的面板結構，不需更動。
- 不動 `@spekjs/core`、web app、VS Code extension、REST API 或前端 bundle。純 Kotlin、純新增，此 change 本身不隱含版本號變動。
