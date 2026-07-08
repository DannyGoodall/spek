## MODIFIED Requirements

### Requirement: Aggregated scan entry point

The `@spek/core` package SHALL provide an async function `scanOpenSpecAggregated(dir, options)` that returns a `ScanResult` aggregated across all workspaces of the repository containing `dir`. Workspaces SHALL be enumerated via `listWorkspaces(dir, { includeJj })`, which unions git worktrees and (when `options.includeJj !== false`) jj workspaces, deduplicated by path. Active changes from git worktrees SHALL be the source-tagged union of those worktrees (no content deduplication); active changes from jj workspaces SHALL be content-deduplicated against the base and each other per the jj-workspace-aggregation capability (identical copies dropped, divergent copies kept and flagged `conflictsWith`). Archived changes SHALL be the slug-deduplicated union; `specs` SHALL be taken only from the main workspace. Each aggregated `ChangeInfo` SHALL carry an optional `source` (`WorktreeSource`, including `vcs`). For each jj workspace, active changes whose slug is returned by `jjCurrentChangeSlugs(workspacePath)` SHALL be marked `isCurrent: true`. The existing `scanOpenSpec` function SHALL remain unchanged and continue to scan a single directory with no `source` attached. When aggregation is disabled, the repo has a single workspace, or it is not a version-controlled repository, the result SHALL equal `scanOpenSpec(dir)`.

#### Scenario: Aggregated scan over git and jj workspaces

- **WHEN** `scanOpenSpecAggregated(dir)` is called and the repo has git worktrees and/or jj workspaces with active changes
- **THEN** `activeChanges` contains every workspace's active changes, each with a `source` whose `vcs` identifies git or jj
- **AND** `archivedChanges` is the slug-deduplicated union across workspaces
- **AND** `specs` contains only the main workspace's spec topics

#### Scenario: jj-only workspace change is surfaced

- **WHEN** an active change exists only in an added jj workspace
- **THEN** it appears in `activeChanges` with `source.vcs === "jj"`

#### Scenario: Aggregated scan falls back for single workspace

- **WHEN** `scanOpenSpecAggregated(dir)` is called and the repo resolves to a single workspace or is not version-controlled
- **THEN** the result equals `scanOpenSpec(dir)` and no `source` is attached to changes

#### Scenario: scanOpenSpec remains unchanged

- **WHEN** `scanOpenSpec(dir)` is called
- **THEN** it scans only `dir` and its `ChangeInfo` entries carry no `source`, exactly as before this change
