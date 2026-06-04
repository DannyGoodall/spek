## Why

Fresh clone 後照 README Quick Start 跑 `npm install` → `npm run dev` 會直接失敗(issue #2):web server(`tsx watch server/index.ts`)與 vite 都報 `@spek/core` 與 `@spek/core/headings` module not found。根因是 `@spek/core` 的進入點指向編譯產物 `dist/`(不在 git 內),但 `npm install` 與 `npm run dev` 的鏈路都沒有先 build core,新環境從未產生 `dist/`。文件承諾的兩步驟啟動實際上是壞的。

## What Changes

- 在 `packages/core` 加 `prepare` script(`tsc`),讓 root `npm install` 透過 npm workspaces 自動編譯 core,使 `dist/` 在安裝後即存在。
- 強化 root `dev` script,改為先 build core 再啟動 web(`npm run build:core && npm run dev -w @spek/web`),避免 `dist/` 過期或被清除時 `npm run dev` 再次失敗。
- 更新 README Quick Start,使文件描述與實際可行的啟動流程一致。

## Capabilities

### New Capabilities
- `dev-environment-setup`: 規範 monorepo 從 fresh clone 到可執行 dev 環境的建置流程 — `npm install` 後 `@spek/core` 必須已編譯,且 `npm run dev` 能在乾淨環境下成功啟動 web 版。

### Modified Capabilities
<!-- 無既有 capability 的 spec-level 行為變更。core-module 規範的是 @spek/core 的 runtime API,不涉及安裝/建置流程。 -->

## Impact

- `packages/core/package.json`:新增 `prepare` script。
- 根 `package.json`:調整 `dev` script。
- `README.md`:更新 Quick Start 啟動說明。
- 影響範圍僅限開發/安裝體驗,不變更任何 runtime 程式碼或 API 行為。
