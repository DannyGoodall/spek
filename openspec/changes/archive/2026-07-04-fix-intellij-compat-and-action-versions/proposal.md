## Why

兩個回報的維護性 issue，根因都在 repo 設定：

- **#4：IntelliJ plugin 裝不上 2026.1（build 261.x）。** `packages/intellij/gradle.properties` 把 `pluginUntilBuild` 寫死為 `253.*`，`patchPluginXml` 據此在 `plugin.xml` 注入 `until-build` 上限，導致 JetBrains Marketplace / IDE 直接以「requires IDE build 253.* or earlier」擋掉新版 IDE。spek 只用穩定 API（Tool Window、JCEF、Built-in Server），並無真正的相容性問題，純粹是版本上限沒跟上。
- **#7：對外發佈的 composite action 使用過時的 action 版本。** root `action.yml` 引用的 `actions/checkout@v4`、`actions/setup-node@v4`、`actions/cache@v4` 都跑在 GitHub 已標記過時的 Node 20 runtime，使用者引用 `kewang/spek@v1` 時會收到 deprecation 警告，且未來 GitHub 移除該 runtime 後會直接失效。

兩者互相獨立、皆為低風險設定調整，合併為一個 change 一次修掉。

## What Changes

- **移除 IntelliJ plugin 的 `until-build` 上限**：從 `gradle.properties` 拿掉 `pluginUntilBuild`，並在 `build.gradle.kts` 將 `ideaVersion.untilBuild` 設為無上限（`provider { null }`）。改用「不設上限」而非「bump 到 261.*」，避免每次 IDE 大版更新都要重發一版、同一問題反覆重演。
- **升級對外 composite action 的 action 版本**：`action.yml` 的 `checkout@v4→v7`、`setup-node@v4→v6`、`cache@v4→v6`。
- **順帶升級 repo 自身 CI workflow 的 action 版本**（同一 Node 20 過時問題）：`pages.yml`、`vscode-publish.yml`、`intellij-publish.yml` 內的 checkout / setup-node / setup-java / configure-pages / upload-pages-artifact / deploy-pages / setup-gradle 一併升到目前 GitHub 支援的主版本。

## Capabilities

### New Capabilities
<!-- 無新增 capability。 -->

### Modified Capabilities
- `intellij-marketplace-metadata`：新增「IDE 相容範圍」需求 — plugin 宣告 `since-build` 下限但不宣告 `until-build` 上限，確保能安裝於當前與未來的 IntelliJ Platform 版本。
- `github-action`：新增「維護中的 action runtime」需求 — 對外 composite action 的步驟只引用 GitHub 目前支援、跑在維護中 Node runtime 的 action 版本。

## Impact

- `packages/intellij/gradle.properties`：移除 `pluginUntilBuild`。
- `packages/intellij/build.gradle.kts`：`ideaVersion.untilBuild` 改為無上限。
- `action.yml`：升級 checkout / setup-node / cache。
- `.github/workflows/pages.yml`、`vscode-publish.yml`、`intellij-publish.yml`：升級所用 action 版本。
- 不變更任何 runtime 程式碼或前端行為。
- **交付需經一次 release**：#4 需重新發佈 IntelliJ plugin 到 JetBrains Marketplace（走 `intellij-publish.yml`）才對使用者生效；#7 需新的 release tag 讓 `kewang/spek@v1` 指向含新版 action 的 commit。版號 bump、CHANGELOG 三邊同步、change-notes 更新、tag 推送皆由 `/release` skill 於 archive 後處理，不列入本 change 的實作 tasks。
