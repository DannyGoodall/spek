## Why

spek 目前的跨工作目錄聚合只認得 **git worktree**（`git worktree list --porcelain`）。愈來愈多團隊在 git 之上 co-located 使用 **Jujutsu（jj）**，把平行進行的工作放在 *jj workspace*（`jj workspace add`）裡。實測（jj 0.42.0）確認：jj workspace **對 git 完全隱形**——`git worktree list` 不會列出它們，因此 spek 會默默漏掉這些 workspace 裡的 OpenSpec change。

此外，jj 提供 git 沒有的能力：working-copy commit `@` 永遠指向「使用者此刻正在編輯的 change」。對一個唯讀的 OpenSpec 檢視器來說，這是能直接標示「正在編輯哪個 change」的獨特訊號。

## What Changes

- `@spek/core` 新增 `listJjWorkspaces(dir)`：以 `jj workspace list -T <template>` 列出同 repo 的所有 jj workspace，回傳與 `WorktreeInfo` 相同形狀的資料；非 jj repo、無 `jj`、或失敗時回 `[]`（與 `listWorktrees` 完全相同的優雅退場）
- 新增 `listWorkspaces(dir, { includeJj })`：合併 git worktree 與 jj workspace，**依路徑去重**（colocated 主目錄同時是 git main 與 jj `default`，路徑相同 → key 相同 → git 勝以保留 branch），main 置頂
- 聚合掃描改用 `listWorkspaces`，jj workspace 的 active change 一併聚合並附 `source`（含 `vcs: "jj"`）
- **jj 內容去重**：jj workspace 共用同一個 commit graph，每個 workspace 都 materialise 整份 trunk，故同一個進行中的 change 會在每個 workspace 重複出現。git worktree 維持「聯集不去重」；jj workspace 的 active change 改以「`slug` + 內容指紋」對基準（main/default）與彼此去重——內容相同者丟棄、內容分歧者保留並標 `conflictsWith`（指向衝突來源，如 `main`）、該 workspace 獨有者保留。關聯圖套用相同去重，避免重複節點灌水 spec 的 historyCount。新增 `changeContentFingerprint(dir, slug)` 計算指紋、`ChangeInfo` 加選用 `conflictsWith`
- **jj `@` 高亮**：`jjCurrentChangeSlugs(dir)` 以 `jj diff --ignore-working-copy --name-only -r @` 找出該 workspace working copy 正在改動的 `openspec/changes/<slug>/`，在對應 active change 上標記 `isCurrent`
- jj 聚合由**獨立開關**控制（與 git worktree 聚合分離）：VS Code 設定 `spek.aggregateJjWorkspaces`（預設開）、Web `jj` query param + 前端 checkbox；git worktree 聚合行為維持現況不變
- 範圍：`@spek/core` + Web 版 + VS Code extension。**IntelliJ 暫不納入**（其 Kotlin core 目前完全沒有 worktree 聚合基礎，jj 對等需另案先補齊整層聚合）；Demo（靜態、無 VCS）不受影響

## Capabilities

### New Capabilities

- `jj-workspace-aggregation`: 探索同 repo 的 jj workspace、與 git worktree 合併去重、jj workspace 的 change 以內容指紋去重（相同丟棄、分歧標 `conflictsWith`）、jj `@` 當前 change 高亮、獨立的 jj 聚合開關、`jj` 不可用時優雅退場

### Modified Capabilities

- `openspec-scanner`: `scanOpenSpecAggregated` / `buildGraphDataAggregated` 改以 `listWorkspaces` 列舉工作目錄並接受 `includeJj` 選項；jj workspace 的 active change 以內容指紋去重並標記 `isCurrent` / `conflictsWith`
- `openspec-api`: changes / overview / graph API 接受 `jj`（是否納入 jj workspace）參數；change 項目新增 `source.vcs`、`isCurrent` 與 `conflictsWith`
- `change-browsing`: change 列表顯示 jj 來源標記、「正在編輯」標記與「conflicts with …」分歧標記，並提供獨立的 jj 聚合開關
- `graph-view`: 關聯圖的 change 節點涵蓋 jj workspace，節點 id 沿用 `change:<key>:<slug>` 命名避免碰撞，並對 jj 重複節點以內容指紋去重
- `live-reload`: 聚合啟用且納入 jj 時，file watcher 監看 jj workspace 的 `openspec/` 目錄
- `vscode-sidebar`: 新增 `spek.aggregateJjWorkspaces` 設定，sidebar 標示 jj 來源、「正在編輯」與「conflicts with …」

## Impact

- `@spek/core`：新增 `jj-workspaces.ts`（`listJjWorkspaces`、`parseJjWorkspaceList`、`jjCurrentChangeSlugs`）與 `worktrees.ts` 的 `listWorkspaces`；`scanner.ts` 新增 `changeContentFingerprint` 與 jj 內容去重；`WorktreeInfo` / `WorktreeSource` 加 `vcs`，`ChangeInfo` 加選用 `isCurrent` 與 `conflictsWith`；`index.ts` 匯出新函式
- `@spek/web` server：`/changes`、`/overview`、`/graph`、`/watch` 接受 `jj` 參數；`/watch`、`/changes/:slug` 改用 `listWorkspaces`
- `@spek/web` 前端：jj 來源 / 正在編輯 / conflicts 標記（`WorktreeBadge`、`ChangeList`）、jj 聚合 checkbox、`jjWorkspacePref`、`ApiAdapter` / `FetchAdapter` / `MessageAdapter` / `useOpenSpec` 帶 `includeJj`
- VS Code extension：新增 `spek.aggregateJjWorkspaces` 設定，handler / tree-provider 讀設定傳 `includeJj` 並渲染 jj / editing / conflicts 標記，panel watcher 改用 `listWorkspaces`
- 執行環境**不要求** `jj`；無 jj / 非 jj repo 時行為與現況逐位元相同
- 不影響 IntelliJ plugin 與 Demo
