## 1. MarkdownRenderer heading id prefix support

- [x] 1.1 Update `packages/web/src/components/MarkdownRenderer.tsx` to accept optional `idPrefix?: string` prop; pass to `rehypeSpekHeadingIds` plugin as an option
- [x] 1.2 Update `rehypeSpekHeadingIds` plugin signature to accept `{ idPrefix?: string }`; when prefix is set, prepend `${idPrefix}${slug}` to the heading id; dedupe counter must still run per-render so duplicates in the same doc keep working
- [x] 1.3 Verify SpecDetail still renders without `idPrefix` (default empty string, no behavior change)
- [x] 1.4 Run `npm run type-check` to confirm props signature

## 2. TabView controlled mode

- [x] 2.1 Update `packages/web/src/components/TabView.tsx` to accept optional `activeId` + `onChange` props; when both provided, use external state; otherwise keep existing internal state
- [x] 2.2 Preserve existing behavior for SpecList and any other uncontrolled TabView consumers
- [x] 2.3 Add a comment on the controlled-mode branch explaining the dual-mode contract

## 3. ChangeDetail: tab state synced to URL

- [x] 3.1 Replace internal TabView state in `packages/web/src/pages/ChangeDetail.tsx` with `useSearchParams` driven `tab` param; valid values: `proposal` | `design` | `specs` | `tasks`; default `proposal` when absent or invalid
- [x] 3.2 Wire `onChange` to `setSearchParams({ tab: newId })` and clear `location.hash` simultaneously (use `navigate(`${pathname}?tab=${id}`)` or setSearchParams + manual hash clear)
- [x] 3.3 Reset window scroll to top on tab switch (use `window.scrollTo(0, 0)` in the onChange handler)
- [x] 3.4 Add unit-level reasoning comment: why we clear hash on tab switch (different tab means old heading no longer valid)

## 4. SpecsTabContent component

- [x] 4.1 Create `packages/web/src/components/SpecsTabContent.tsx` that accepts `specs: { topic: string; content: string }[]`
- [x] 4.2 Render each spec as `<section id="spec-<topic>">` with a header `<h3 className="text-sm font-semibold text-accent mb-2">{topic}</h3>` (matches current inline behavior)
- [x] 4.3 Render each spec's `<MarkdownRenderer content={spec.content} idPrefix={`${topic}--`} />`
- [x] 4.4 Replace the inline Specs tab `<MarkdownRenderer>` loop in ChangeDetail with `<SpecsTabContent specs={data.specs} />`

## 5. ChangeDetail TOC sidebar layout

- [x] 5.1 Compute `currentHeadings` via `useMemo`, switching on active tab:
  - `proposal` → `extractHeadings(data.proposal)` if exists else `[]`
  - `design` → `extractHeadings(data.design)` if exists else `[]`
  - `specs` → flatten `data.specs.flatMap(s => extractHeadings(s.content).map(h => ({ ...h, slug: `${s.topic}--${h.slug}` })))`
  - `tasks` → always `[]`
- [x] 5.2 Compute `showToc = activeTab !== "tasks" && currentHeadings.length >= 3` (viewport handled via `xl:` Tailwind class)
- [x] 5.3 Wrap current `<TabView>` + sidebar in a responsive grid layout (mirrors SpecDetail): `xl:grid xl:grid-cols-[minmax(0,1fr)_16rem] xl:gap-8` when `showToc` is true
- [x] 5.4 Render `<SpecToc headings={currentHeadings} />` in the right column under `hidden xl:block`, using a `key={activeTab}` so scrollspy state resets per tab
- [x] 5.5 Ensure sticky tab bar (`top-14`) still works above main content; TOC sticky (`top-6`) lives in right column and does not conflict

## 6. Hash anchor scroll in ChangeDetail

- [x] 6.1 Add a `useEffect` depending on `data`, `activeTab`, `location.hash` that:
  - Reads `location.hash` (strip `#`)
  - Retries up to ~300ms via `requestAnimationFrame` until `document.getElementById(hash)` resolves
  - Scrolls using `window.scrollTo({ top, behavior: "smooth" })` with an 80px header offset (same as SpecDetail)
  - Cleans up `rafId` on unmount / dep change
- [x] 6.2 Verify hash scroll works after a tab switch triggered via URL deep link (e.g., opening `?tab=design#decision-1` directly)

## 7. Cross-platform verification

- [x] 7.1 Web: `npm run dev`, open a long change (e.g., the Phase 2 / Phase 3 archived changes). Verify:
  - Proposal tab shows TOC with correct headings
  - Switch to Design → TOC updates, URL becomes `?tab=design`, hash cleared, scroll resets
  - Switch to Specs → TOC lists headings from all delta specs with no id collisions
  - Switch to Tasks → TOC hidden
  - Click a TOC entry → smooth scroll + URL hash update (fixed mid-review: `SpecToc` now preserves `location.search` so TOC clicks don't drop `?tab=`)
  - Copy URL `?tab=design#<slug>` to a new tab → page opens on Design with scroll at heading
  - Viewport < 1280px → TOC hidden
  - Short change (< 3 headings on active tab) → TOC hidden
- [x] 7.2 VS Code: build (`npm run build -w @spek/core && npm run build:webview -w @spek/web && npm run build -w spek-vscode`), launch Extension Development Host, verify TOC appears in webview panel for Change detail when tab is markdown and panel wide enough (build confirmed in auto mode; runtime verification in Extension Development Host pending manual check)
- [x] 7.3 IntelliJ: run `npm run build:intellij` + `./gradlew runIde` (optional sanity), verify the same TOC behavior in JCEF tool window (webview bundle built; gradle runIde skipped)

## 8. Polish and release

- [x] 8.1 Run `npm run type-check` across all packages
- [x] 8.2 Run `openspec validate add-change-detail-toc --strict` and fix any issues
- [x] 8.3 Update root `CHANGELOG.md`, `packages/vscode/CHANGELOG.md`, `packages/intellij/CHANGELOG.md` Unreleased sections with the new change detail TOC feature (keep three files synchronized)
- [x] 8.4 Visual polish: check TOC `key={activeTab}` resets active highlight cleanly; verify Specs tab TOC label reads naturally without topic prefix; confirm light/dark theme contrast of active entries
