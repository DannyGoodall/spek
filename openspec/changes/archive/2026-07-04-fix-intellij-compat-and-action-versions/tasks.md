## 1. IntelliJ plugin 相容範圍（issue #4）

- [x] 1.1 從 `packages/intellij/gradle.properties` 移除 `pluginUntilBuild = 253.*`
- [x] 1.2 在 `packages/intellij/build.gradle.kts` 將 `ideaVersion.untilBuild` 改為無上限（`untilBuild = provider { null }`），保留 `sinceBuild`

## 2. 對外 composite action 版本（issue #7）

- [x] 2.1 在 `action.yml` 升級 `actions/checkout@v4 → @v7`、`actions/setup-node@v4 → @v6`、`actions/cache@v4 → @v6`

## 3. Repo 自身 CI workflow 版本（同一 Node 20 過時問題）

- [x] 3.1 `.github/workflows/pages.yml`：`checkout@v4 → v7`、`configure-pages@v5 → v6`、`upload-pages-artifact@v3 → v5`、`deploy-pages@v4 → v5`
- [x] 3.2 `.github/workflows/vscode-publish.yml`：`checkout@v4 → v7`、`setup-node@v4 → v6`
- [x] 3.3 `.github/workflows/intellij-publish.yml`：`checkout@v4 → v7`、`setup-node@v4 → v6`、`setup-java@v4 → v5`、`gradle/actions/setup-gradle@v4 → v6`

## 4. 驗證

- [x] 4.1 跑 `./gradlew buildPlugin` 並確認產出的 `plugin.xml`（`build/tmp/patchPluginXml/plugin.xml`）含 `since-build`、不含 `until-build`（結果：`<idea-version since-build="233" />`，buildPlugin 完整成功）
- [x] 4.2 跑 `./gradlew verifyPluginProjectConfiguration`（通過，設定合法）；完整跨 IDE `verifyPlugin`（pluginVerifier）需額外配置 `pluginVerification.ides` 目標並下載目標 IDE，本 repo 未設定，屬 release 前的獨立檢查（見 design.md Decision 1 緩解措施），不在本 change 範圍內
- [x] 4.3 人工比對四個 workflow / action YAML（`grep` 確認無殘留 `@v4` / 過時版本引用，所有 `uses:` 已升至目標主版本；actionlint 未安裝故採人工檢查）
