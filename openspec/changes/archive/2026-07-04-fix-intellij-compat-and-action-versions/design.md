## Context

兩個維護 issue 都是設定層級的調整，實作簡單，但各有一個「怎麼改才不會反覆重演」的取捨值得記錄。

## Decision 1：移除 `until-build` 上限，而非 bump 到 261.*

**選項 A（採用）**：拿掉上限。`gradle.properties` 移除 `pluginUntilBuild`，`build.gradle.kts` 設 `untilBuild = provider { null }`，`patchPluginXml` 產出的 `plugin.xml` 只有 `since-build`、無 `until-build`。

**選項 B（不採用）**：把 `pluginUntilBuild` bump 到 `261.*`。

取捨：spek 只用穩定的 IntelliJ Platform API（Tool Window、JCEF、Built-in Server），跨大版破壞的風險低。選項 B 每逢 IDE 大版更新（253→261→…）都要再發一版，issue #4 會週期性重演；選項 A 一次解決，之後只有真的遇到 API 破壞時才需重新設上限。`sinceBuild = 233` 下限維持不變，仍保有向下相容界線。

風險：若未來某個新平台版本真的破壞相容性，無上限的 plugin 可能在該版本安裝後行為異常。緩解：發版前跑 `pluginVerifier`（build.gradle.kts 已宣告該相依），若報 API 破壞再視情況重新加上限。

## Decision 2：action 版本升級範圍

issue #7 只點名對外 `action.yml`，但同一個 Node 20 過時問題也存在於 repo 自己的三個 workflow。一併升級以避免同類回報，且都是低風險的主版本跳升。目標版本取自各 action 目前的 latest release：

| Action | 目標主版本 |
|---|---|
| actions/checkout | v7 |
| actions/setup-node | v6 |
| actions/cache | v6 |
| actions/setup-java | v5 |
| actions/configure-pages | v6 |
| actions/upload-pages-artifact | v5 |
| actions/deploy-pages | v5 |
| gradle/actions/setup-gradle | v6 |

需求文字（spec）以「GitHub 目前支援、跑在維護中 Node runtime」描述以維持耐久性；具體版號僅作為本 change 的驗收 scenario，不寫死進 Requirement 敘述。
