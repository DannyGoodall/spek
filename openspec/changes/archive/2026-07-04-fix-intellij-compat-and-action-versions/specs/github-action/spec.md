## ADDED Requirements

### Requirement: Maintained action runtime versions
對外 composite action（`action.yml`）的每個步驟 SHALL 引用 GitHub 目前支援、跑在維護中 Node runtime 的 action 版本，SHALL NOT 依賴 GitHub 已標記過時的版本，以免使用者收到 deprecation 警告或在該 runtime 移除後失效。

#### Scenario: No deprecated action runtime
- **WHEN** 使用者的 workflow 透過 `kewang/spek@v1` 執行本 action
- **THEN** 各步驟所用的 action 版本不觸發 GitHub 對過時 Node runtime 的 deprecation 警告

#### Scenario: Action versions in action.yml
- **WHEN** 檢視 `action.yml`
- **THEN** `actions/checkout` 引用 `v7`（取代過時的 `v4`）
- **AND** `actions/setup-node` 引用 `v6`
- **AND** `actions/cache` 引用 `v6`
