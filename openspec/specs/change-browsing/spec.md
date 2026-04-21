## ADDED Requirements

### Requirement: Change list with active/archived separation
The system SHALL display changes grouped into active and archived sections. Active changes SHALL be visually distinguished with a left accent color border (4px). Changes SHALL be sorted by git timestamp descending (most recent first), falling back to slug date when timestamp is unavailable. Each change SHALL display a relative time indicator (e.g., "3 hours ago", "2 days ago") when a git timestamp is available, with the full ISO timestamp shown as a tooltip on hover. When no git timestamp is available, the system SHALL fall back to displaying the slug date in YYYY-MM-DD format.

#### Scenario: Display active changes
- **WHEN** user navigates to the ChangeList page and there are active changes
- **THEN** active changes are listed in an "Active" section with a left accent color border, name, and task progress

#### Scenario: Display archived changes
- **WHEN** user navigates to the ChangeList page
- **THEN** archived changes are listed in an "Archived" section sorted by timestamp descending, without accent border

#### Scenario: Display relative time for changes with timestamp
- **WHEN** a change has a git timestamp
- **THEN** the change displays a relative time (e.g., "2 days ago") instead of YYYY-MM-DD
- **AND** hovering shows the full ISO timestamp as a tooltip

#### Scenario: Display date for changes without timestamp
- **WHEN** a change has no git timestamp
- **THEN** the change displays the slug date in YYYY-MM-DD format

#### Scenario: No changes
- **WHEN** there are no changes in the repo
- **THEN** system displays an empty state message

### Requirement: Change detail with tab navigation
The system SHALL display change details using a tabbed interface with tabs in OpenSpec workflow order: Proposal, Design, Specs (delta specs), and Tasks. Tab content SHALL transition with a fade-in animation when switching. The change title (including back navigation link) and tab navigation bar SHALL be sticky-positioned below the main header, remaining visible when the user scrolls through long content.

#### Scenario: View proposal tab
- **WHEN** user views a change and clicks the Proposal tab
- **THEN** the proposal.md content is displayed with a fade-in transition

#### Scenario: View design tab
- **WHEN** user clicks the Design tab
- **THEN** the design.md content is displayed with a fade-in transition

#### Scenario: View specs tab
- **WHEN** user clicks the Specs tab and the change has delta specs
- **THEN** the delta spec files are listed and their content displayed with a fade-in transition

#### Scenario: View tasks tab
- **WHEN** user clicks the Tasks tab
- **THEN** the tasks.md content is displayed with a TaskProgress bar showing completion statistics, with a fade-in transition

#### Scenario: Tab order
- **WHEN** the ChangeDetail page is rendered
- **THEN** the tabs are displayed in order: Proposal, Design, Specs, Tasks (matching the OpenSpec workflow sequence)

#### Scenario: Missing artifact
- **WHEN** a tab's corresponding artifact file does not exist
- **THEN** the tab shows a "No content" placeholder

#### Scenario: Sticky header on scroll
- **WHEN** user scrolls down through long change content
- **THEN** the change title (with back link) and tab navigation bar SHALL remain fixed below the main application header
- **AND** the sticky area SHALL have an opaque background that covers scrolling content beneath it

#### Scenario: Sticky does not overlap main header
- **WHEN** the sticky area is active
- **THEN** it SHALL be positioned directly below the main header (top offset equal to header height) with a z-index lower than the main header and sidebar

### Requirement: Task progress display in change detail
The Tasks tab SHALL display a progress bar and statistics (completed/total) derived from the change's task data.

#### Scenario: Show task progress
- **WHEN** viewing the Tasks tab of a change with tasks
- **THEN** a TaskProgress component shows a visual progress bar with "X / Y completed" text

### Requirement: Capability ID linking in proposal
The system SHALL render inline code elements in proposal markdown as navigable links when the code text matches an existing spec topic name.

#### Scenario: Capability ID matches existing spec
- **WHEN** a proposal markdown contains an inline code element (e.g., `` `responsive-layout` ``)
- **AND** a spec with the topic name `responsive-layout` exists
- **THEN** the inline code SHALL be rendered as a clickable link navigating to `/specs/responsive-layout`

#### Scenario: Inline code does not match any spec
- **WHEN** a proposal markdown contains an inline code element that does not match any existing spec topic
- **THEN** the inline code SHALL be rendered as a normal styled code element without a link

#### Scenario: MarkdownRenderer receives spec topics list
- **WHEN** MarkdownRenderer is used with a `specTopics` prop containing the list of available spec topic names
- **THEN** inline code matching any topic in the list SHALL be rendered as navigable links

### Requirement: Custom task checkbox styling
The system SHALL render task items in the Tasks tab using custom SVG icons instead of text-based `[x]`/`[ ]` markers. Completed tasks SHALL display a filled checkmark icon in green, and incomplete tasks SHALL display an empty circle icon. Completed task text SHALL have reduced opacity (0.6) in addition to the existing strikethrough styling.

#### Scenario: Incomplete task display
- **WHEN** a task item is not completed
- **THEN** the task displays an empty circle SVG icon followed by the task text at full opacity

#### Scenario: Completed task display
- **WHEN** a task item is completed
- **THEN** the task displays a green checkmark SVG icon followed by the task text with strikethrough and reduced opacity (0.6)

### Requirement: Change detail TOC sidebar
The change detail page (`/changes/:slug`) SHALL display a sticky table-of-contents (TOC) sidebar for the current markdown tab when the heading count is at least 3 and the viewport width is at least 1280px. TOC SHALL be shown only when the active tab is Proposal, Design, or Specs; the Tasks tab SHALL never show a TOC. Each TOC entry SHALL be a clickable link that smooth-scrolls the main content to the corresponding heading. `h3` entries SHALL be visually indented relative to `h2` entries.

#### Scenario: TOC visible on Proposal tab with long content
- **WHEN** user views a change whose Proposal content contains 3 or more `h2`/`h3` headings on a viewport at least 1280px wide
- **AND** the Proposal tab is active
- **THEN** a sticky TOC sidebar appears alongside the main content listing every `h2` and `h3` heading from the Proposal markdown in document order

#### Scenario: TOC visible on Design tab
- **WHEN** user switches to the Design tab and the Design content has 3 or more `h2`/`h3` headings
- **THEN** the TOC sidebar updates to list the Design tab's headings

#### Scenario: TOC visible on Specs tab
- **WHEN** user switches to the Specs tab and the combined headings across all delta specs total 3 or more
- **THEN** the TOC sidebar lists every heading from every delta spec in the order the specs are rendered

#### Scenario: TOC hidden on Tasks tab
- **WHEN** user switches to the Tasks tab
- **THEN** no TOC sidebar is rendered regardless of heading count, because Tasks content is structured (non-markdown)

#### Scenario: TOC hidden for short markdown tab
- **WHEN** the active tab's content contains fewer than 3 `h2`/`h3` headings
- **THEN** no TOC sidebar is rendered and the main content occupies the full available width

#### Scenario: TOC hidden on narrow viewport
- **WHEN** user views a change on a viewport narrower than 1280px
- **THEN** the TOC sidebar is not rendered regardless of the active tab

#### Scenario: Click TOC entry
- **WHEN** user clicks a TOC entry while on a markdown tab
- **THEN** the main content smooth-scrolls to the corresponding heading
- **AND** the URL hash updates to that heading's slug

#### Scenario: Indented h3 entries
- **WHEN** the TOC contains both `h2` and `h3` entries
- **THEN** each `h3` entry is visually indented relative to its preceding `h2` entry

### Requirement: Change detail TOC updates on tab switch
The TOC sidebar SHALL recompute its entries whenever the active tab changes, and SHALL apply scrollspy and hash-anchor behavior to the newly active tab's content.

#### Scenario: Tab switch recomputes headings
- **WHEN** user switches from Proposal to Design while a TOC is visible
- **THEN** the TOC entries update to reflect the Design tab's headings

#### Scenario: Tab switch clears previous hash
- **WHEN** user switches tabs by clicking the tab bar
- **THEN** the URL hash is cleared and only the new tab's query param remains

#### Scenario: Tab switch scrolls content to top
- **WHEN** user switches to a different tab
- **THEN** the main content resets to the top of the new tab's content

### Requirement: Change detail tab state persisted in URL
The change detail page SHALL reflect the active tab in the URL query string (`?tab=<tab-id>`), and SHALL restore the correct tab when the page loads with a `tab` query parameter. If no `tab` parameter is present, the Proposal tab SHALL be active by default.

#### Scenario: Tab selection updates URL
- **WHEN** user clicks a tab
- **THEN** the URL query string updates to `?tab=<id>` matching the clicked tab

#### Scenario: Load with tab query param
- **WHEN** user opens a URL such as `/changes/<slug>?tab=design`
- **THEN** the Design tab is active when the page renders

#### Scenario: Load without tab query param
- **WHEN** user opens `/changes/<slug>` with no `tab` query parameter
- **THEN** the Proposal tab is active by default

#### Scenario: Invalid tab query param falls back to default
- **WHEN** user opens a URL with an unknown `tab` value (e.g., `?tab=bogus`)
- **THEN** the Proposal tab is active and no error is raised

### Requirement: Change detail scrollspy
The change detail page SHALL highlight the TOC entry corresponding to the heading currently closest to the top of the viewport while the user scrolls the active markdown tab (scrollspy behavior).

#### Scenario: Active entry on scroll
- **WHEN** user scrolls through a markdown tab's content while the TOC is visible
- **AND** a heading enters the top region of the viewport
- **THEN** the TOC entry matching that heading is visually highlighted as active

#### Scenario: Only one active entry
- **WHEN** multiple headings are simultaneously visible in the viewport
- **THEN** exactly one TOC entry is highlighted (the heading closest to the top)

### Requirement: Change detail hash anchor navigation
The change detail page SHALL scroll to the heading matching the URL hash after the page loads or after the hash changes, once the active tab's markdown content finishes rendering. When a URL contains both `tab` query param and a hash, the page SHALL first activate the specified tab, then scroll to the hash-matching heading within that tab.

#### Scenario: Direct link with tab and hash
- **WHEN** user opens a URL such as `/changes/<slug>?tab=design#decision-1`
- **THEN** the Design tab becomes active
- **AND** after the Design content renders, the page scrolls so the heading with slug `decision-1` is at the top of the visible area

#### Scenario: Direct link with hash but no tab param
- **WHEN** user opens `/changes/<slug>#some-heading` with no `tab` query param
- **THEN** the Proposal tab is active
- **AND** the page scrolls to the heading with slug `some-heading` in the Proposal content

#### Scenario: Hash change while on page
- **WHEN** user clicks a TOC entry while viewing a markdown tab
- **THEN** the URL hash updates and the page scrolls to the new target heading within the current tab

#### Scenario: Hash with no matching heading
- **WHEN** the URL hash does not match any heading slug on the current tab
- **THEN** no scrolling occurs and the page renders at its default scroll position

### Requirement: Specs tab heading slug prefix
When the Specs tab renders multiple delta specs in a single change, each spec's heading ids SHALL be prefixed with the spec topic using the format `<topic>--<slug>` to prevent id collisions across specs. The TOC entries SHALL display the original heading text unchanged (without the prefix), while the anchor links SHALL use the prefixed form.

#### Scenario: Distinct slugs across specs with duplicate heading
- **WHEN** a change's Specs tab contains two delta specs each with a `### Requirement: Foo`
- **THEN** the two resulting heading elements have distinct ids of the form `<topic-a>--requirement-foo` and `<topic-b>--requirement-foo`
- **AND** both appear in the TOC with label "Requirement: Foo"

#### Scenario: TOC anchor uses prefixed slug
- **WHEN** user clicks a TOC entry for a heading in the Specs tab
- **THEN** the URL hash is set to the prefixed form `<topic>--<slug>` and the matching element scrolls into view

#### Scenario: SpecDetail page slugs remain unprefixed
- **WHEN** user views a spec at `/specs/:topic`
- **THEN** the heading ids remain the original unprefixed slugs (behavior unchanged from the existing spec detail TOC)
