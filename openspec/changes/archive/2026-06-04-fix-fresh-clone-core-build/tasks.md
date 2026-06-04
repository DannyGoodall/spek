## 1. Build 設定

- [x] 1.1 在 `packages/core/package.json` 新增 `prepare` script(`tsc`),讓 root `npm install` 自動編譯 core
- [x] 1.2 將根 `package.json` 的 `dev` script 改為 `npm run build:core && npm run dev -w @spek/web`

## 2. 文件

- [x] 2.1 更新 `README.md` Quick Start,確保描述的啟動流程與實際可行流程一致(必要時補 troubleshooting)

## 3. 驗證

- [x] 3.1 模擬 fresh clone:刪除 `packages/core/dist/` 後跑 `npm install`,確認 `dist/index.js` 與 `dist/headings.js` 重新產生
- [x] 3.2 刪除 `packages/core/dist/` 後跑 `npm run dev`,確認 Express API 與 Vite 皆啟動且無 `@spek/core` / `@spek/core/headings` module not found
