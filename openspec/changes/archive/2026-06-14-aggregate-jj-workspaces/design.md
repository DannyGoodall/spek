## Context

跨 worktree 聚合的整個資料流都建立在 `WorktreeInfo[]` 之上：`listWorktrees` 列舉 → `scanOpenSpecAggregated` / `buildGraphDataAggregated` 以 `wt.path` 掃描、以 `wt.key`（路徑雜湊）命名節點、以 `toWorktreeSource` 標記來源。因此把 jj workspace 接進來最小侵入的做法，是讓 jj workspace 也產出同形狀的 `WorktreeInfo`，再從單一列舉點合流。

## Goals / Non-Goals

- **Goals**：jj workspace 的 OpenSpec change 一併聚合；jj `@` 高亮；獨立 jj 開關；`jj` 不可用時優雅退場且與現況一致。
- **Non-Goals**：不改 git worktree 聚合的既有行為與預設；不為 jj 重寫掃描/圖譜邏輯；IntelliJ Kotlin 與 Demo 不在範圍。
- **範圍界定（specifications）**：本變更在 **change** 的粒度做跨 workspace 聚合與去重；「在 workspace 中發現的 specification」指的是**隨 change 一起攜帶的 delta spec**（`openspec/changes/<slug>/specs/**`）——當某 change 被保留（獨有或分歧），其 delta spec 自然隨之呈現。**頂層 `openspec/specs/` 一律取主工作目錄**，不在本次跨 workspace 聚合/比對頂層 spec 的新增或分歧；若日後需要 spec 粒度的跨 workspace 呈現，應另開 change。

## Decisions

### 以 `WorktreeInfo` 為共用形狀，新增 `vcs` 判別欄位

jj workspace 與 git worktree 共用 `WorktreeInfo`（兩者都是「掛在同 repo 上的工作目錄」），新增必填 `vcs: "git" | "jj"` 區分來源，供 UI 標記。`WorktreeSource` 同步加 `vcs`。`ChangeInfo` 加選用 `isCurrent?: boolean`。
- jj workspace 無 git branch → 以 workspace name 填 `branch`（沿用既有 branch 徽章渲染）；`head` 取 `@` 的 short change id；`isMain = name === "default"`；`isBare = false`。

### 單一列舉合流點 `listWorkspaces`

```
listWorkspaces(dir, { includeJj = true }) =
  dedupeByKey([ ...await listWorktrees(dir),            // git 先放，碰撞時 git 勝（保留 branch）
                ...(includeJj ? await listJjWorkspaces(dir) : []) ])
  並把 isMain 排到最前
```
- **去重以 `key`（= sha1(resolvedPath)）**：colocated 主目錄在 git 與 jj 兩邊路徑相同 → key 相同 → 只留 git 那筆。新增的 jj workspace 路徑相異 → key 相異 → 納入。
- jj 依 `name` 字母排序輸出，`default` 不保證在前 → parser 與 `listWorkspaces` 都明確把 `isMain` 提到最前。
- 下游 `scanOpenSpecAggregated` / `buildGraphDataAggregated` 把 `listWorktrees` 換成 `listWorkspaces`，列舉與骨架（平行掃描、`length <= 1` fallback、`isBare` 過濾、`change:<key>:<slug>` 命名）沿用（jj 永不 bare；單一 colocated 主目錄去重後長度為 1 → 走單目錄 fallback）。**唯一需要為 jj 改寫的下游邏輯是 active change 的合併方式**——git worktree 維持聯集，jj workspace 改為內容去重（見下節），因為 jj 的工作目錄模型與 git worktree 不同。

### jj `@` 高亮（side-effect-free）

`jjCurrentChangeSlugs(dir)` 跑 `jj diff --ignore-working-copy --name-only -r @`，從 `openspec/changes/<slug>/…`（沿用 `git-cache.ts` 的 slug 取法，含 archive 判斷）收集 slug 集合。
- 用 `--ignore-working-copy` 維持 spek 唯讀、不觸發 jj 快照（符合 jj 對唯讀操作的建議）；代價是極新的、尚未被 jj 快照的編輯可能短暫未高亮，但檔案本身仍會被掃描列出，jj 下次快照即自動修正。
- 在 `scanOpenSpecAggregated` 平行掃描階段，對 `vcs === "jj"` 的 workspace 取其集合，命中的 active change 設 `isCurrent: true`。

### jj workspace 的 active change 以內容去重（與 git worktree 行為不同）

git worktree 是**分支範圍**的：每個 worktree 的檔案系統只含該分支的 change，故聯集（不去重）正確。jj workspace **不是**——所有 workspace 共用同一個 commit graph，每個 workspace 都 materialise 整份 trunk，所以 trunk 上一個進行中的 change 會在**每個** workspace 的 `openspec/` 各出現一次。沿用 git 的聯集會讓 n 個 workspace 看到 n 份相同的 change（實測：4 個 workspace → 同一 change 出現 4 次）。

因此 active change 的合併**依來源分流**：

- git worktree（含 main）：維持聯集、不去重（同 slug 在不同 worktree 各自並存，維持既有 specced 行為）。
- jj workspace：以「`slug` + 內容指紋」對基準（main/default）與彼此去重。
  - 指紋 = `changeContentFingerprint(repoDir, slug)`：change 目錄下所有檔案的相對路徑 + 內容的 sha1。
  - 內容**相同** → 丟棄（消除 n 份重複）。
  - slug 相同但**內容分歧**（某 workspace 編輯中）→ 保留為獨立項，標 `conflictsWith = <衝突來源標籤>`（通常為 `main`）；若同時是 `@` 編輯中亦標 `isCurrent`。
  - slug **全新**（workspace 獨有）→ 保留並標來源。
- 關聯圖（`buildGraphDataAggregated`）以同樣指紋對 jj active change 節點去重；跳過重複者連帶略過其 edges，避免灌水 spec 的 `historyCount`。
- **僅在 repo 含 jj workspace 時計算指紋**（`repoUsesJj`），純 git repo 零額外 I/O。

設計取捨：
- **採內容指紋，而非單純「依 slug 去重」**——純 slug 去重會把某 workspace 正在編輯的分歧版本藏進基準版本，而那正是使用者最在意、`@` 高亮要凸顯的進行中工作。
- **未採 jj revset 計算 delta**（如 `trunk()..@`）——需定義 base、面對 stacked workspace 較脆弱；內容指紋與 VCS 語意無關、穩健，直接對應「change 目錄是否相同」，也正好複用既有的檔案系統掃描。
- specs 取主工作目錄、archived 依 slug 去重（jj 各 workspace 的 archived 內容相同 → 已自然收斂），故只有 active change 需要此內容去重。

### 獨立 jj 開關（與 git 分離）

git worktree 聚合維持「偵測到多個即自動聚合」的現況；jj 納入與否由獨立旗標 `includeJj`（預設 true）控制：
- VS Code：新增 `contributes.configuration` `spek.aggregateJjWorkspaces`（boolean, default true），handler / tree-provider 讀取後傳 `includeJj`。
- Web：`?jj=false` 關閉；前端 `jjWorkspacePref`（localStorage `spek:aggregate-jj`，預設開）+ checkbox。
- `includeJj` 只在聚合啟用時有意義（`aggregate=false` 走單目錄，本就不列舉任何工作目錄）。

## Risks / Trade-offs

- **timestamp**：`git-cache.ts` 由 `git log` 取時間，jj-only / 未提交的 change 取不到 → `timestamp` 為 null，排序回退到 slug 日期或 null（與現況對非 git 內容一致）。可接受，未在本次補 jj 時間來源。
- **效能**：每次聚合掃描多一個 `execFile("jj", …)`，與 `listWorktrees` 以 `Promise.all` 平行；`jj` 不在 PATH 時 ENOENT 立即返回，無實質延遲。
- **工作目錄去重正確性**：列舉層依路徑雜湊去重；只要 jj `default` 與 git main 指向同一實體路徑（colocated 的常態）即正確收斂為一筆。git 與 jj 皆回傳解析過 symlink 的實體路徑，故以實體路徑為基準。
- **內容去重正確性**：active change 的 jj 去重依「slug + 內容指紋」；指紋涵蓋 change 目錄全部檔案內容，故只在內容真正相同時才丟棄，分歧者必定保留並標 `conflictsWith`，不會誤藏進行中的編輯。
- **內容指紋效能**：僅在 repo 含 jj workspace 時讀檔計算；OpenSpec change 多為小型 markdown，成本可忽略，純 git repo 完全不觸發。

## Migration

無資料遷移。`vcs` 為新增必填欄位 → `parseWorktreePorcelain` 與 `toWorktreeSource` 需補 `vcs: "git"`，否則 TypeScript 編譯失敗（這是 git 路徑唯一需要動到的地方）。`isCurrent`、`conflictsWith` 為選用欄位，舊資料/單目錄掃描為 undefined，不影響既有消費端。

測試環境註記：既有 `worktrees.test.ts` 一筆斷言比對 `path.resolve(repo)`，但 git/jj 回傳的是解析過 symlink 的實體路徑（macOS `TMPDIR` 在 `/var → /private/var` 下），故改以 `fs.realpathSync` 比對；此為環境性修正，與功能行為無關。jj 相關測試需 `jj` 可用，故以 `HAS_JJ` 守衛並在缺少 `jj` 時 `skip`。
