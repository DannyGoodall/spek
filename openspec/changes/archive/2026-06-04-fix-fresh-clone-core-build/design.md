## Context

`@spek/core` 是 monorepo 的共用套件,`package.json` 的 `main` / `exports` 指向編譯產物 `dist/`,但 `dist/` 由 `tsc` 產生且不納入 git。fresh clone 後 `npm install` 不會編譯 core,`npm run dev` 的鏈路(`npm run dev -w @spek/web` → `concurrently "vite" "tsx watch server/index.ts"`)也沒有 build 步驟,導致 web server 與 vite 解析 `@spek/core` / `@spek/core/headings` 時 module not found(issue #2)。本機開發者因早已 build 過 `dist/` 而不會遇到。

## Goals / Non-Goals

**Goals:**
- 讓 fresh clone 在 `npm install` 後 `@spek/core` 即為已編譯狀態。
- 讓 `npm run dev` 在乾淨環境下可成功啟動。
- 文件描述與實際可行流程一致。

**Non-Goals:**
- 不改動 `@spek/core` 的 runtime API 或任何前端/後端執行邏輯。
- 不導入 dev 階段 core source 的 hot reload(`tsx watch` 仍消費 `dist/`,core 改動需重新 build,屬既有行為,不在此 change 範圍)。
- 不把 `dist/` commit 進 git。

## Decisions

**決策 1:在 `packages/core` 加 `prepare` script(`tsc`)。**
npm 7+ 在 root `npm install` 時會自動替各 workspace 執行 `prepare`,因此安裝即編譯 core,`dist/` 在安裝後存在。
- 替代方案:把 `dist/` commit 進 git → 否決,產物入版控易髒、易過期。
- 替代方案:只靠 root `dev` script build → 不足以涵蓋 `build:web`、`type-check` 等同樣假設 core 已 build 的入口;`prepare` 是根治。

**決策 2:root `dev` script 改為 `npm run build:core && npm run dev -w @spek/web`。**
作為 `prepare` 之外的第二道保險:即使 `dist/` 被清掉或過期,`npm run dev` 仍會先重建 core 再啟動,確保「dev 一定能跑」。

**決策 3:更新 README Quick Start。**
維持 `npm install` → `npm run dev` 兩步驟,但現在兩步驟實際可行;必要時補一句 troubleshooting。

## Risks / Trade-offs

- [`prepare` 在某些 CI/安裝情境(如 `--ignore-scripts`)不會執行] → root `dev` 的 `build:core` 作為第二道保險覆蓋此情況。
- [`npm run dev` 每次啟動多一次 core build,略增啟動時間] → core 體積小,`tsc` 數秒內完成,可接受;換取「保證能跑」。
- [`prepare` 在發佈情境也會跑] → 本套件為 `private`,不發佈 npm,無影響。
