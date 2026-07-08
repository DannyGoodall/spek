# jj-workspace-aggregation Specification

## Purpose
TBD - created by archiving change aggregate-jj-workspaces. Update Purpose after archive.
## Requirements
### Requirement: Discover jj workspaces

The `@spek/core` package SHALL provide an async function `listJjWorkspaces(dir)` that returns the Jujutsu workspaces of the repository containing `dir`, by executing `jj workspace list` with a custom template, with `dir` as the working directory. Each returned entry SHALL be a `WorktreeInfo` with `vcs` set to `"jj"`, `path` (absolute workspace root), `branch` set to the workspace name, `head` set to the working-copy change id, `isMain` true for the `default` workspace, `isBare` false, and a path-derived `key`. The `default` workspace SHALL appear first regardless of jj's alphabetical output order. When `dir` is not inside a jj repository, when `jj` is not installed, or when the command fails, `listJjWorkspaces` SHALL return an empty array.

#### Scenario: Colocated repo with an added workspace

- **WHEN** `listJjWorkspaces(dir)` is called and the repo has a `default` workspace plus one added via `jj workspace add`
- **THEN** it returns two `WorktreeInfo` entries, each with `vcs: "jj"`
- **AND** the `default` entry has `isMain: true` and appears first

#### Scenario: Not a jj repository

- **WHEN** `listJjWorkspaces(dir)` is called and `dir` is not inside a jj repository, or `jj` is not installed
- **THEN** it returns an empty array

### Requirement: Unified workspace enumeration

The `@spek/core` package SHALL provide an async function `listWorkspaces(dir, options)` that merges git worktrees and jj workspaces of the repository into a single `WorktreeInfo[]`, deduplicated by `key` (the path hash) so that a colocated main directory — present in both `git worktree list` and `jj workspace list` at the same path — appears once, keeping the git entry to preserve its branch. The `isMain` entry SHALL be ordered first. Inclusion of jj workspaces SHALL be controlled by `options.includeJj`, defaulting to true; when false, only git worktrees are returned.

#### Scenario: Colocated main is not duplicated

- **WHEN** `listWorkspaces(dir)` is called on a colocated git+jj repo whose main directory is both the git main worktree and the jj `default` workspace
- **THEN** that directory appears exactly once with `vcs: "git"`, retaining the git branch

#### Scenario: jj-only workspace surfaced alongside git worktrees

- **WHEN** the repo has a git worktree and a separately added jj workspace
- **THEN** `listWorkspaces(dir)` includes the git worktree (`vcs: "git"`), the jj workspace (`vcs: "jj"`), and the deduplicated main, with the main first

#### Scenario: jj inclusion disabled

- **WHEN** `listWorkspaces(dir, { includeJj: false })` is called
- **THEN** the result contains only git worktrees, identical to `listWorktrees(dir)`

### Requirement: Current jj change highlight

The `@spek/core` package SHALL provide an async function `jjCurrentChangeSlugs(dir)` that returns the set of OpenSpec change slugs whose files are modified in the jj working-copy commit `@` of the workspace at `dir`, by inspecting `jj diff --ignore-working-copy --name-only -r @` and extracting `<slug>` from `openspec/changes/<slug>/…` paths (including the `archive/<slug>` form). It SHALL be side-effect-free with respect to the jj working copy. When `jj` is unavailable or the command fails, it SHALL return an empty set. Aggregated scanning SHALL set `isCurrent: true` on active changes whose slug is in the corresponding jj workspace's set.

#### Scenario: Working copy edits a change

- **WHEN** the jj working copy of a workspace has modifications under `openspec/changes/add-foo/`
- **THEN** `jjCurrentChangeSlugs(dir)` contains `add-foo`
- **AND** the aggregated active change `add-foo` from that workspace carries `isCurrent: true`

#### Scenario: jj unavailable

- **WHEN** `jjCurrentChangeSlugs(dir)` is called and `jj` is not installed or `dir` is not a jj repo
- **THEN** it returns an empty set and no change is marked current

### Requirement: Deduplicate shared jj changes by content

Because jj workspaces share one commit graph and each workspace materialises the entire trunk, a change present on the shared trunk appears in every workspace's `openspec/` on disk. Aggregation SHALL NOT emit such a change once per workspace. For jj workspaces, active changes SHALL be deduplicated against the base (the main/default workspace) and against each other by `slug` + a content fingerprint (a hash of all files in the change directory): a byte-identical copy of an already-seen change SHALL be dropped. A change whose `slug` matches an already-seen change but whose content differs SHALL be kept as a separate entry attributed to its workspace, and SHALL carry a `conflictsWith` label naming the source it diverges from (normally the main workspace). Git worktree aggregation SHALL be unaffected by this content-fingerprint mechanism: git worktrees are deduplicated by their own git-history divergence election (see the `worktree-aggregation` spec), which is a separate and independent code path. jj workspaces SHALL NOT be fed into that git-history election — they are invisible to `git worktree list` and their working-copy commit (`@`) is a jj change id, not a git ref, so the git divergence signal does not exist for them; the content-fingerprint path is their equivalent. The relationship graph SHALL apply the same content deduplication to jj change nodes.

#### Scenario: Shared change appears once across workspaces

- **WHEN** a repo has a trunk change `add-foo` and four jj workspaces, each materialising an identical copy
- **THEN** the aggregated active list contains exactly one `add-foo`

#### Scenario: Divergent jj copy is kept and flagged

- **WHEN** one jj workspace has modified its copy of `add-foo` so its content differs from the base
- **THEN** the aggregated active list contains the base `add-foo` plus the workspace's divergent `add-foo`
- **AND** the divergent entry carries `conflictsWith` naming the base (e.g. `main`)

#### Scenario: Graph does not duplicate shared jj changes

- **WHEN** the aggregated graph is built on a repo whose trunk change is materialised in multiple jj workspaces
- **THEN** that change appears as a single node and does not inflate the referenced spec's history count

### Requirement: Separate jj aggregation toggle

jj workspace inclusion SHALL be controllable independently of git worktree aggregation. Git worktree aggregation SHALL retain its existing auto-detect, default-on behavior unchanged. The jj toggle SHALL default to enabled. When jj inclusion is disabled, aggregated results SHALL be exactly those of git-worktree-only aggregation.

#### Scenario: jj toggle off leaves git aggregation intact

- **WHEN** aggregation runs with the jj toggle disabled on a colocated repo that also has git worktrees
- **THEN** the result is identical to git-worktree-only aggregation, with no jj-sourced changes

