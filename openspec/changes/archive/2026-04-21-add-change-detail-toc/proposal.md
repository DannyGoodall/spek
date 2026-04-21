## Why

Change detail 頁面（`/changes/:slug`）在 Proposal / Design / Specs 三個 tab 下常會有長篇 markdown 內容，使用者想快速定位到 `## Why`、`## Decisions`、`### Requirement: ...` 等章節時只能持續捲動。上一個 change `2026-04-21-add-spec-detail-toc` 已為 spec detail 加入 sticky TOC + scrollspy + hash 錨點，並在 Non-Goals 明確列出「不為 change detail 頁加 TOC（後續若有需求可獨立 change）」；本 change 即是延續，把相同的閱讀體驗補齊到 change detail。

## What Changes

### Webview / SPA 端（Web、VS Code Webview、IntelliJ Webview、Demo 共用）
- `ChangeDetail` 頁面為 Proposal / Design / Specs 三個 markdown tab 分別計算 headings 並顯示右側 sticky TOC 側欄
- Tasks tab 不顯示 TOC（內容為結構化 section 渲染，非 markdown，已有 progress bar + section title 足夠導覽）
- Tab 切換時 TOC 內容隨之更新，scrollspy 與 hash 行為重新套用到當前 tab
- URL query + hash 組合支援 deep link：`/changes/<slug>?tab=design#decision-1` 開啟後自動切到 design tab 並捲動到對應 heading
- 沿用既有 `SpecToc` 元件、`useScrollspy` hook、`rehypeSpekHeadingIds` rehype plugin、hash scroll effect
- TOC 顯示門檻沿用 spec TOC：heading 數量 ≥ 3 才顯示；視窗寬度 < 1280px 收合
- Specs tab 為多個 delta spec 合併顯示，各 spec 的 heading slug 以 `{topic}--{slug}` 前綴避免不同 spec 間 slug 衝突

### Non-scope
- VS Code Sidebar 的 Changes TreeView 不展開 heading 子節點（change 含 4 個 tab，展開後層級過深，UX 不佳）
- 不改動 IntelliJ plugin 端 tree view
- 不為 Tasks tab 加 TOC

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `change-browsing`: 新增 change detail TOC 導覽需求（每個 markdown tab 獨立 TOC、scrollspy、tab + hash 雙參數 deep link、Specs tab slug prefix 策略）

## Impact

- **Webview / SPA**:
  - `packages/web/src/pages/ChangeDetail.tsx`：改為感知當前 tab，對 Proposal / Design / Specs 各自算 headings；grid layout 加入 TOC 側欄；加入 tab + hash URL 同步與還原
  - `packages/web/src/components/TabView.tsx`：支援受控模式（`activeId` + `onChange`）讓外層同步 URL query
  - 可能新增：`packages/web/src/components/SpecsTabContent.tsx`（把 Specs tab 多 spec 合併 + slug 前綴邏輯抽出）
  - 沿用既有 `SpecToc`、`useScrollspy`、`MarkdownRenderer`（含 `rehypeSpekHeadingIds`）
- **Core**:
  - `@spek/core/headings` 既有 `extractHeadings` API 沿用；Specs tab slug prefix 在 webview 端處理，不改 core
- **API / Backend**: 無變更
- **VS Code Extension**: 不變（不展開 Changes TreeView heading）
- **IntelliJ**: 不變
- **Demo**: 靜態版自動套用（純前端邏輯）
- **Dependencies**: 不需新增 npm 套件
