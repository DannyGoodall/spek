## Context

`ChangeDetail` 頁面目前用 `TabView` 組件承載 Proposal / Design / Specs / Tasks 四個分頁，`TabView` 是非受控（內部 state `activeId`），且 URL 不反映當前 tab。Proposal / Design / Specs 三個 tab 是純 markdown 內容（透過 `MarkdownRenderer` render），上一個 change 已給 `MarkdownRenderer` 加上 `rehypeSpekHeadingIds` 讓 h2/h3 自動產生 id；Tasks tab 則是把 `data.tasks.sections` 結構化渲染成 custom 列表，不是 markdown。

上一個 change（`2026-04-21-add-spec-detail-toc`）已產出：
- `@spek/core/headings` 的 `extractHeadings` / `slugifyHeading`
- `packages/web/src/hooks/useScrollspy.ts`（scroll listener + `getBoundingClientRect`）
- `packages/web/src/components/SpecToc.tsx`（sticky TOC + scrollspy + click-to-scroll）
- `packages/web/src/components/MarkdownRenderer.tsx` 內的 `rehypeSpekHeadingIds`（確保每次 render 都重新指派 id 且支援 strict-mode double render）
- SpecDetail 的 hash-scroll `useEffect`（retry 到元素 render 完）

本 change 要把同一組 primitive 套用到 change detail，最大的不同是：**多個 tab、Tabs 切換時 TOC 要跟著換、Specs tab 是多個 spec 合併**。Tab state 也要跟 URL 雙向同步，才能讓 `?tab=design#decision-1` 這種 deep link 有實際效果。

## Goals / Non-Goals

**Goals:**
- Proposal / Design / Specs 每個 tab 都有自己的 TOC，內容正確反映該 tab markdown
- Tab 切換時 TOC 立即更新，scrollspy 與 hash 行為重新套用
- URL 同時支援 `?tab=<id>` 與 `#<heading-slug>`，重整或貼網址都能還原到同一位置
- Specs tab 多 spec 合併時，不同 spec 間 heading slug 不會衝突
- Webview（VS Code、IntelliJ、Demo）一併受益，不需額外改 extension host

**Non-Goals:**
- Tasks tab 不做 TOC
- VS Code sidebar Changes TreeView 不展開 heading 子節點
- 不改動 IntelliJ plugin
- 不重寫 `TabView` 為 `react-router` nested route（範圍太大）
- 不動 core 的 `extractHeadings` 簽名（slug prefix 在 webview 端處理）

## Decisions

### Decision 1: TabView 改受控
- **選擇**：`TabView` 加入可選 `activeId` + `onChange` props；當兩者都提供時走受控模式，否則保留既有內部 state 行為（向後相容 SpecList 等使用者）
- **替代方案**：
  - (A) 改用 `react-router` nested route，每個 tab 一條 route：變動面太大，且需要拆 ChangeDetail
  - (B) 把 URL 同步邏輯塞進 `TabView` 內部：違反元件職責，`TabView` 不應感知 router
- **理由**：受控模式改動最小、影響範圍最窄；URL 同步只在 `ChangeDetail` 處理

### Decision 2: Tab state 用 query param
- **選擇**：URL 格式 `/changes/<slug>?tab=<id>#<heading-slug>`。`ChangeDetail` 用 `useSearchParams()` 讀寫 `tab` param
- **替代方案**：用 path param（`/changes/<slug>/<tab>`）
- **理由**：path param 需要改 router 設定；query param 是單一頁面內的 view state，語意貼合，且不影響 back/forward 行為
- **Default**：若無 `tab` query param，預設 `proposal`（即 `tabs[0]`）

### Decision 3: Specs tab slug 前綴
- **選擇**：Specs tab 把多個 delta spec 合併渲染時，每份 spec 的 markdown 在進入 `MarkdownRenderer` 前先包一層 `<section data-spec-topic="<topic>">`，不直接字串拼接。TOC 端則獨立呼叫 `extractHeadings(spec.content)` 取得每 spec 的 heading，再以 `{topic}--{slug}` 格式組合出最終 slug
- **替代方案**：
  - (A) 直接字串拼接所有 spec 的 markdown：會讓 `rehypeSpekHeadingIds` 的 dedupe counter 跨 spec 共用，slug 雖不衝突但人類不可預期（例如兩 spec 各有 `### Requirement: Foo` 會變成 `requirement-foo` + `requirement-foo-2`）
  - (B) 改 `extractHeadings` 或 `rehypeSpekHeadingIds` 支援 prefix：污染 core API
- **理由**：slug 帶 spec topic 前綴語意最清楚；使用 DOM 分段 + 獨立解析，保持 core API 不變
- **實作細節**：
  - 新增 `SpecsTabContent` 元件包裝 Specs tab 多 spec 渲染邏輯
  - 元件內對每份 spec render 一個 `<section id="spec-<topic>">`，內部 `<MarkdownRenderer content={spec.content} idPrefix="<topic>--" />`
  - `MarkdownRenderer` 的 `rehypeSpekHeadingIds` 接受可選 `idPrefix`（預設空字串，不影響既有使用者）
  - TOC 項目顯示時保持 heading 原文（不帶 topic），視覺上靠 spec topic 的區塊 header 分組

### Decision 4: 每個 tab 獨立算 headings（useMemo by tab）
- **選擇**：`ChangeDetail` 對當前 tab 的內容用 `useMemo` 算一次 headings；切 tab 時重新計算
- **替代方案**：預先為所有 tab 都算一次 → 浪費且切 tab 時多餘 re-render
- **理由**：切 tab 頻率不高，計算成本低（regex + slugify），簡單

### Decision 5: 沿用既有 SpecToc，不改名
- **選擇**：`SpecToc` 名稱保留不改；它本來就是「markdown TOC」概念，與 spec 語意無綁定。ChangeDetail 直接 import 使用
- **替代方案**：rename 為 `TocSidebar` → 會牽動既有 SpecDetail 與 test，且無實質好處
- **理由**：降低 diff 範圍

### Decision 6: Scrollspy 元素邊界調整
- **選擇**：`useScrollspy` 既有邏輯沿用。因 ChangeDetail 有 sticky tab bar（top 14 = 56px）與主 header，header offset 維持 80px 與 SpecDetail 一致
- **驗證**：Tab bar sticky + TOC sticky 兩者在 ChangeDetail 下的疊加不重疊（tab bar 位於主內容區頂端，TOC 在右欄，垂直不衝突）

### Decision 7: Hash scroll 的 tab 協同
- **選擇**：若 URL 同時帶 tab + hash，`ChangeDetail` 先切到對應 tab（setActiveId），再在該 tab content mount + markdown render 完後觸發 hash scroll。沿用 SpecDetail 的 retry-with-rAF 模式（最多 ~300ms）
- **理由**：tab content 切換後 markdown 需重新 render 才會有新 heading 元素；retry 可自然等待

### Decision 8: Tab 切換時清除 hash
- **選擇**：使用者點擊 tab 切換時，清除 URL hash，只保留 `?tab=<new>`；避免殘留的 hash 指向舊 tab 不存在的 heading
- **理由**：hash 的語意是「當前 tab 的章節」；保留會造成視覺上 TOC 無對應項目、scrollspy 空轉

### Decision 9: Tasks tab 切換時不顯示 TOC 側欄
- **選擇**：`showToc = currentTab !== "tasks" && headings.length >= 3 && viewport >= xl`
- **理由**：Tasks tab 為結構化渲染，沒有 markdown heading；明確排除避免 TOC 顯示空內容

## Risks / Trade-offs

- **[Risk] TabView 受控模式改動可能破壞既有 SpecList / 其他使用 TabView 的頁面** → Mitigation：僅在 `activeId` + `onChange` 同時提供時切換為受控；其他使用者不傳這兩 props 即維持原行為；加 type-check 確保
- **[Risk] Specs tab `idPrefix` 若誤傳會使 SpecDetail 的 hash 錨點錯位** → Mitigation：`idPrefix` 預設空字串；SpecDetail 不傳即保持既有行為；新增 test 驗證 prefix 僅影響 ChangeDetail 路徑
- **[Risk] Tab + hash 同時 deep link 時，hash scroll 在 tab content 尚未 render 就觸發會失敗** → Mitigation：沿用 SpecDetail 的 rAF retry；useEffect 依賴 `currentTab` + content，tab 切換後會重跑
- **[Risk] Specs tab 有多份 spec 時 heading 數可能爆量，TOC 變得很長** → Mitigation：TOC 已有 `max-h-[calc(100vh-6rem)]` + overflow scroll；不額外處理
- **[Risk] 使用者用 browser back 回到前一個 tab，hash 若已被清空會失去位置** → Mitigation：接受此行為；一般預期 back 即是回到前一狀態，hash 清空屬正確語意
- **[Trade-off] URL 形狀 `?tab=design#slug` 在 VS Code webview 的 MemoryRouter 下同樣有效，但使用者無法真正「貼網址」分享 → webview 內部導覽仍可用**

## Open Questions

- Specs tab 的 TOC 項目要不要在視覺上用「spec topic 分組」顯示（例如 TOC 內加小 header 分段）？預設**先不做**，維持扁平列表；若實作時發現某些 change 的 delta spec 很多再補
- Tab 切換時的 scroll 行為：要不要在切 tab 時自動把主內容 scroll 回頂端？預設**是**（tab content 換新內容，停留在舊位置無意義）
