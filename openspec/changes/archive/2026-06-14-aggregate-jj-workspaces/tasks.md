## 1. Core — jj workspace 列舉與型別

- [x] 1.1 `types.ts`：`WorktreeInfo` / `WorktreeSource` 新增必填 `vcs: "git" | "jj"`；`ChangeInfo` 新增選用 `isCurrent?: boolean`
- [x] 1.2 `worktrees.ts`：`parseWorktreePorcelain` 與 `toWorktreeSource` 補 `vcs: "git"`
- [x] 1.3 新增 `jj-workspaces.ts`：`JJ_TEMPLATE`、`parseJjWorkspaceList(stdout)`（純函式、`default` 置頂、`vcs: "jj"`、reuse `worktreeKey`）、`listJjWorkspaces(dir)`（`execFile`、error→`[]`）
- [x] 1.4 `jj-workspaces.ts`：`jjCurrentChangeSlugs(dir)`（`jj diff --ignore-working-copy --name-only -r @`，回 `Set<string>`，error→空集合）
- [x] 1.5 `worktrees.ts`：`listWorkspaces(dir, { includeJj })`，合併 git+jj、依 key 去重（git 勝）、main 置頂
- [x] 1.6 `index.ts` 匯出 `listWorkspaces`、`listJjWorkspaces`、`parseJjWorkspaceList`、`jjCurrentChangeSlugs`

## 2. Core — 聚合掃描與圖資料

- [x] 2.1 `scanOpenSpecAggregated(dir, { aggregate, includeJj })`：`listWorktrees` → `listWorkspaces(dir, { includeJj })`
- [x] 2.2 平行掃描後，對 `vcs === "jj"` 的 workspace 取 `jjCurrentChangeSlugs`，命中的 active change 設 `isCurrent: true`
- [x] 2.3 `buildGraphDataAggregated(dir, { aggregate, includeJj })`：同樣改用 `listWorkspaces`

## 3. Core — 測試

- [x] 3.1 新增 `jj-workspaces.test.ts`：`parseJjWorkspaceList` 純函式案例（解析、空 change id、default 置頂、跳過畸形行、key 一致、vcs）
- [x] 3.2 `jj-workspaces.test.ts`：live 案例（`HAS_JJ` guard + `node:test` skip）—列出 default、含新增 workspace、非 jj 回 `[]`、`listWorkspaces` colocated 去重（git 勝）、jj-only workspace 與 git worktree 並存
- [x] 3.3 `jj-workspaces.test.ts`：`jjCurrentChangeSlugs` 命中 `openspec/changes/<slug>`
- [x] 3.4 擴充 `aggregate.test.ts`：jj-only workspace 的 change 以 `source.vcs === "jj"` 呈現、colocated 主目錄不重複計、`@` 命中者 `isCurrent === true`

## 4. Web server + 前端

- [x] 4.1 `openspec.ts`：`/overview`、`/changes`、`/graph`、`/watch` 接受 `jj` query param（`!== "false"`），傳 `{ aggregate, includeJj }`；`/watch` 改用 `listWorkspaces`
- [x] 4.2 新增 `jjWorkspacePref.ts`（localStorage `spek:aggregate-jj`，預設開）
- [x] 4.3 `ApiAdapter` / `FetchAdapter` / `MessageAdapter` / `StaticAdapter`：changes / overview / graph 帶 `includeJj`（Fetch → `&jj=false`；Static 忽略）
- [x] 4.4 `useOpenSpec.ts` hooks 由 pref 帶入 `includeJj`
- [x] 4.5 `ChangeList.tsx`：jj 來源出現時顯示「Include jj workspaces」checkbox；change 卡片標 jj 來源與「正在編輯」

## 5. VS Code extension

- [x] 5.1 `package.json`：新增 `contributes.configuration` `spek.aggregateJjWorkspaces`（boolean, default true）
- [x] 5.2 `handler.ts` / `tree-provider.ts`：讀 `spek.aggregateJjWorkspaces`，傳 `{ includeJj }` 給 core
- [x] 5.3 `panel.ts` `addWorktreeWatchers`：`listWorktrees` → `listWorkspaces`
- [x] 5.4 `tree-provider.ts`：sidebar 標示 jj 來源與「正在編輯」

## 6. 驗證與文件

- [x] 6.1 `npm run type-check` 與 `npm run test -w @spek/core` 通過
- [x] 6.2 `/tmp` colocated repo 實測：`jj git init --colocate`、`jj workspace add`、jj workspace 放 change → `scanOpenSpecAggregated` 出現且 `source.vcs==="jj"`、`@` 高亮
- [x] 6.3 更新 `CLAUDE.md`（core 模組、API、VS Code 設定）與三份 CHANGELOG（root / vscode / intellij，並註明 IntelliJ 暫緩）
- [x] 6.4 將研究報告存為 `docs/jj-integration.md`

## 7. jj 內容去重（修正：jj workspace materialise 整份 trunk 導致重複）

- [x] 7.1 `types.ts`：`ChangeInfo` 新增選用 `conflictsWith?: string`
- [x] 7.2 `scanner.ts`：新增 `changeContentFingerprint(repoDir, slug)`（change 目錄檔案路徑＋內容 sha1）
- [x] 7.3 `scanOpenSpecAggregated`：git worktree 維持聯集；jj workspace 以「slug + 內容指紋」對 main 基準與彼此去重，相同丟棄、分歧保留並標 `conflictsWith`（僅含 jj 時才計算指紋，git-only repo 零額外 I/O）
- [x] 7.4 `buildGraphDataAggregated`：jj active change 節點套用相同內容去重（跳過重複連帶略過 edges，不灌水 historyCount）
- [x] 7.5 `aggregate.test.ts`：shared change 跨多 workspace 只出現一次、分歧版保留並標 `conflictsWith === "main"`、git 同 slug 仍各自並存
- [x] 7.6 UI：`ChangeList`（web）與 `tree-provider`（vscode）顯示「conflicts with …」標記
- [x] 7.7 更新 `jj-workspace-aggregation` spec（內容去重需求）、`docs/jj-integration.md`、三份 CHANGELOG
