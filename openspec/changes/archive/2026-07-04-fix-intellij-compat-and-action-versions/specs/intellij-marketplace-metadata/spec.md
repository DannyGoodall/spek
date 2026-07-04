## ADDED Requirements

### Requirement: IDE compatibility range
Plugin SHALL 宣告 `since-build` 下限，但 SHALL NOT 宣告 `until-build` 上限，讓 plugin 能安裝於當前與未來的 IntelliJ Platform 版本，而非被人為的上限擋在舊版。

#### Scenario: No upper bound in build config
- **WHEN** 檢視 `packages/intellij/gradle.properties` 與 `packages/intellij/build.gradle.kts`
- **THEN** 定義了 `pluginSinceBuild`（`since-build` 下限）
- **AND** 未定義 `pluginUntilBuild`，且 `ideaVersion.untilBuild` 設為無上限

#### Scenario: Patched plugin.xml has no until-build
- **WHEN** 執行 `patchPluginXml` 產生最終 `plugin.xml`
- **THEN** `<idea-version>` 含 `since-build` 屬性
- **AND** `<idea-version>` 不含 `until-build` 屬性

#### Scenario: Installable on newer IDE builds
- **WHEN** 使用者在比開發平台更新的 IDE build（例如 2026.1 / build 261.x）安裝 plugin
- **THEN** IDE 不會以「requires IDE build … or earlier」阻擋安裝或啟用
