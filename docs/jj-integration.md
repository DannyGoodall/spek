# Jujutsu (jj) Integration — Research Report

> 範圍：`@spek/core` + Web 版 + VS Code extension。IntelliJ 暫緩（見文末）。

## 背景與問題

spek 的跨工作目錄聚合原本只認得 **git worktree**（`git worktree list --porcelain`）。實測（jj 0.42.0）確認：**jj workspace 對 git 完全隱形**——在 colocated（`.jj` 與 `.git` 並存）的 repo 中，`jj workspace add` 建立的 workspace 不會出現在 `git worktree list`，因此 spek 會默默漏掉這些 workspace 裡進行中的 OpenSpec change。

此外，jj 提供 git 沒有的能力：working-copy commit `@` 永遠是一個一級 commit，指向「使用者此刻正在編輯的內容」。對唯讀的 OpenSpec 檢視器而言，這是能直接標示「正在編輯哪個 change」的獨特訊號。

## 已實作的方案

### 1. jj workspace 列舉（`@spek/core`）

- `listJjWorkspaces(dir)`：`jj workspace list -T '<template>'` 列出同 repo 的所有 jj workspace，回傳與 `WorktreeInfo` 相同形狀（`vcs: "jj"`）。jj 依 name 字母排序，故把 `default` 置頂。
- `listWorkspaces(dir, { includeJj })`：合併 git worktree 與 jj workspace，**依 key（= sha1(實體路徑) 前 8 碼）去重**。colocated 主目錄同時是 git main 與 jj `default`、路徑相同 → key 相同 → 保留 git 那筆（維持 branch）。
- 聚合掃描（`scanOpenSpecAggregated` / `buildGraphDataAggregated`）改以 `listWorkspaces` 列舉。

### 1b. jj workspace 的內容去重（與 git worktree 行為不同）

jj workspace 與 git worktree 的本質不同：git worktree 是**分支範圍**的（只含該分支的 change），故聯集即可；jj workspace **共用同一個 commit graph**，每個 workspace 的 working copy 都materialise 整份 trunk，因此 trunk 上一個進行中的 change 會在**每個** workspace 的 `openspec/` 各出現一次。若沿用 git 的「聯集不去重」，n 個 workspace 就會看到 n 份相同的 change（實測 4 個 workspace → 同一 change 出現 4 次）。

因此 active changes 對 jj workspace **改以內容去重**：

- 以 main（基準 / checked-out 狀態）為基準，逐一比對其他 jj workspace 的同名 change。
- 比對鍵 = `slug` + **內容指紋**（change 目錄下所有檔案的相對路徑＋內容的 sha1）。
- 內容**完全相同** → 丟棄（消除 n 份重複）。
- slug 相同但**內容分歧**（某 workspace 正在編輯）→ 保留為獨立項，標 `conflictsWith`（指向基準來源，如 `main`），UI 顯示「conflicts with main」；若正是 `@` 編輯中亦同時標「editing」。
- slug 全新（該 workspace 獨有）→ 保留並標來源。

git worktree 行為完全不變（仍聯集、同 slug 各自並存）。關聯圖（graph）對 jj change 節點套用相同的內容去重，避免同一 change 因 `change:<key>:<slug>` 命名而變成多個節點、灌水 spec 的 historyCount。specs 取主工作目錄、archived 依 slug 去重，本就不會重複。

> 設計取捨：採內容指紋而非「依 slug 去重」，是因為純 slug 去重會把某 workspace 正在編輯的分歧版本藏進基準版本——而那正是使用者最在意、`@` 高亮要凸顯的進行中工作。也未採 jj revset 計算 delta（需定義 trunk/base、面對 stacked workspace 較脆弱），內容指紋與 VCS 語意無關且穩健，直接對應「change 目錄是否相同」。

### 2. jj `@` 當前 change 高亮

- `jjCurrentChangeSlugs(dir)`：`jj diff --ignore-working-copy --name-only -r @` 取得 `@` 改動的檔案，從 `openspec/changes/<slug>/` 擷取 slug。用 `--ignore-working-copy` 維持 spek **唯讀、不觸發 jj 快照**。
- 聚合掃描在 repo 含 jj workspace 時，對各工作目錄查 `@`，命中的 active change 標 `isCurrent`，UI 顯示「editing」標記。

### 3. 獨立的 jj 開關（與 git worktree 聚合分離）

- git worktree 聚合維持現況（偵測到多個即自動聚合）。
- jj 納入由獨立旗標 `includeJj`（預設開）控制：
  - **VS Code**：設定 `spek.aggregateJjWorkspaces`（boolean, default true）。
  - **Web**：`?jj=false` 關閉；前端 `spek:aggregate-jj`（localStorage）+「Include jj workspaces」checkbox。

### 4. 優雅退場

每個 jj 指令都包成 `error → []`／空集合，與 `listWorktrees` 對 git 的處理完全一致：非 jj repo、`jj` 不在 PATH、或指令失敗時，行為與未導入 jj 前**逐位元相同**。執行環境**不要求** `jj`。

## 風險與取捨（已處理）

- **timestamp**：`git-cache.ts` 由 `git log` 取時間，jj-only / 未提交的 change 取不到 → `timestamp` 為 null，排序回退到 slug 日期或 null（與現況對非 git 內容一致）。
- **效能**：每次聚合掃描多一個平行的 `jj` 子行程；`jj` 不在 PATH 時 ENOENT 立即返回。
- **macOS symlink**：git/jj 皆回傳解析過 symlink 的實體路徑（`/var` → `/private/var`），去重以實體路徑為準，故 colocated 主目錄正確收斂為一筆。

## 後續增強機會（依價值排序，尚未實作）

1. **`@` 高亮**（已實作）：git 無此等價訊號，jj `@` 提供之。
2. **各 workspace working-copy 摘要**：`target.description().first_line()` 與 `target.empty()` 已在 workspace template 取得範圍內，可顯示每個 jj workspace 當前 change 描述／「乾淨 vs 進行中」。成本低。
3. **以 bookmark 取代 branch 名**：jj 來源改顯示最近的 bookmark（`target.bookmarks()`），更貼近 jj 使用者心智。成本低。
4. **operation log「最近活動」**：`jj op log` 可建立「OpenSpec change 何時出現／封存」時間軸，甚至安全的「自我上次檢視以來變了什麼」。成本中、與檢視器核心職責略偏。
5. **revset 驅動的探索（大方向）**：不只掃各 workspace 的 checked-out 檔案，改以 revset（如觸及 `openspec/changes/` 的 commit）找出沒被 check out 的 sibling commit 上的 proposal。威力大，但會改變 spek「檔案系統檢視器」的模型、複雜度高，列為未來研究項，非速贏。
6. **jj-aware timestamps**：以 `jj log` 補 jj-only / 未提交 change 的時間。成本中、效益普通。

## VS Code jj 擴充偵測（評估後不採用）

社群有 jj 的 VS Code 擴充（如 Jujutsu Kaleidoscope/jjk、vscode-jj）。spek 可探測 `vscode.extensions.getExtension(<id>)` 並在其存在＋啟用時提供交接（如 `spek.openInJj` 指令）。**評估結論：** 對唯讀檢視器價值有限，且把 spek 耦合到易變的第三方 ID／API。spek 自身的 jj 感知（本次方案）已交付使用者面向的價值而無此耦合；待出現具體交接流程再評估。

## 不在範圍（後續）

- **IntelliJ plugin**：其 Kotlin core（`packages/intellij/.../core/`）目前**完全沒有** worktree 聚合基礎——沒有 `listWorktrees`、沒有 `source`、API 沒有 `aggregate` 旗標、也沒有測試。要在 IntelliJ 達到 jj 對等，得先把整層 git-worktree 聚合在 Kotlin 重建一遍，屬另案、規模大得多。
