## MODIFIED Requirements

### Requirement: Aggregated graph data builder

The `@spek/core` package SHALL provide an async function `buildGraphDataAggregated(dir, options)` that returns graph data aggregated across all workspaces of the repository, enumerated via `listWorkspaces(dir, { includeJj })`. Change node ids SHALL be namespaced as `change:<workspaceKey>:<slug>` to prevent collisions between same-slug changes from different workspaces, covering both git worktrees and jj workspaces. Spec nodes SHALL be taken from the main workspace only. The existing synchronous `buildGraphData` function SHALL remain unchanged.

#### Scenario: Build aggregated graph data across git and jj

- **WHEN** `buildGraphDataAggregated("/path/to/repo")` is called and the repo has multiple workspaces (git and/or jj)
- **THEN** change nodes cover every workspace, with ids of the form `change:<workspaceKey>:<slug>`

#### Scenario: buildGraphData remains unchanged

- **WHEN** `buildGraphData("/path/to/repo")` is called
- **THEN** it returns single-directory graph data exactly as before this change

### Requirement: Worktree-aware adapter parameters

The `ApiAdapter` interface SHALL allow workspace aggregation to be controlled and resolved across all adapters. Change-list, overview, and graph fetches SHALL accept an `aggregate` flag and an `includeJj` flag, and single-change fetches SHALL accept an optional workspace `key`. `FetchAdapter` SHALL forward these as `aggregate` / `jj` / `wt` query parameters (omitting `jj` only when jj inclusion is enabled, sending `jj=false` to disable). `MessageAdapter` SHALL forward them in its `postMessage` payload to the VS Code extension host. `StaticAdapter` (Demo) SHALL ignore them and behave as a single non-aggregated source.

#### Scenario: FetchAdapter forwards jj toggle

- **WHEN** `FetchAdapter` requests the change list with jj inclusion disabled
- **THEN** it sends `GET /api/openspec/changes?dir=...&jj=false`

#### Scenario: FetchAdapter forwards worktree key

- **WHEN** `FetchAdapter.getChange(slug, wt)` is called with a workspace key
- **THEN** it sends `GET /api/openspec/changes/<slug>?dir=...&wt=<key>`

## ADDED Requirements

### Requirement: Workspace-aware API endpoints

The Web server openspec routes `/overview`, `/changes`, `/graph`, and `/watch` SHALL accept a `jj` query parameter (default enabled; `jj=false` disables jj inclusion) and thread it into core as `includeJj`. The `/watch` endpoint SHALL enumerate directories to watch via `listWorkspaces`, so that jj workspace `openspec/` directories are watched when jj inclusion is enabled.

#### Scenario: changes endpoint honors jj toggle

- **WHEN** `GET /api/openspec/changes?dir=...&jj=false` is requested
- **THEN** the response excludes jj-only workspace changes, matching git-worktree-only aggregation

#### Scenario: watch covers jj workspaces

- **WHEN** `/watch` runs with aggregation and jj inclusion enabled on a repo with a jj workspace
- **THEN** that workspace's `openspec/` directory is watched for changes
