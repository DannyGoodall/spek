## ADDED Requirements

### Requirement: jj source, current-change, and conflict indicators

The change list SHALL visually distinguish changes sourced from a jj workspace (where `source.vcs === "jj"`) from git worktree sources, showing the jj workspace name. A change marked `isCurrent` (the change the jj working-copy commit `@` is currently editing) SHALL be visually highlighted as currently in progress. A change carrying `conflictsWith` (a jj workspace's version that diverges in content from the base copy of the same change) SHALL show a divergence indicator naming the source it conflicts with.

#### Scenario: jj-sourced change is labeled

- **WHEN** the change list renders an aggregated change whose `source.vcs === "jj"`
- **THEN** the card shows a jj workspace indicator with the workspace name

#### Scenario: Currently-edited change is highlighted

- **WHEN** an aggregated active change has `isCurrent: true`
- **THEN** the card shows a "currently editing" indicator

#### Scenario: Divergent change shows a conflict indicator

- **WHEN** an aggregated active change has `conflictsWith` set (e.g. `main`)
- **THEN** the card shows a "conflicts with &lt;source&gt;" indicator

### Requirement: jj aggregation toggle

The change list SHALL provide a control to include or exclude jj workspaces, independent of the git worktree aggregation toggle, shown when jj-sourced content is available. Its state SHALL persist (localStorage key `spek:aggregate-jj`) and default to enabled.

#### Scenario: Toggling jj inclusion

- **WHEN** the user clears the "Include jj workspaces" control
- **THEN** jj-sourced changes are removed from the list and the preference persists across reloads
