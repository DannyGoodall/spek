## ADDED Requirements

### Requirement: Watch jj workspaces

When aggregation is enabled and jj inclusion is on, the live-reload file watcher SHALL watch the `openspec/` directory of each jj workspace in addition to git worktrees, so edits made in a jj workspace trigger a refresh. Directory enumeration for watching SHALL use `listWorkspaces`.

#### Scenario: Edit in a jj workspace triggers refresh

- **WHEN** the watcher is active over a repo with an added jj workspace and a file under that workspace's `openspec/` changes
- **THEN** a refresh is triggered
