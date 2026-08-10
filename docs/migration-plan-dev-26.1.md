# AnvilLib 迁移计划：`port/1.21.1` → `dev/26.1`（回迁执行方案）

> 文档信息
>
> - 生成日期：2026-08-10
> - 分析对象：`port/1.21.1` @ `fab551b`（Minecraft 1.21.1 / NeoForge 21.1.226）、`dev/26.1` @ `0b7e45f`（Minecraft 26.1.2 /
    NeoForge 26.1.2.76）
> - 共同祖先（merge-base）：`81c5a9c`
> - 数据来源：git 实测（`git show` / `git diff` / `git log` 只读命令，2026-08-10 本地仓库）+ NeoForge 26.1.2.76 sources jar
    实测 + 8 个并行子代理模块级调研
> - 关联文档：本文是 **迁移计划文档**，与 `docs/branch-comparison.md`（分支对比报告）配套；回迁项使用与对比报告一致的
    canonical 编号 #1–#8
> - 结论性质：除标注「待验证」处外，均基于实测提交、文件内容与 26.1.2.76 sources jar 得出
> - 执行约束：本计划只描述 **内容回迁**，不含 dev 侧既有演进的搬运；本文件仅为计划，执行时需在 dev/26.1 分支上以受控提交落地

---

## 1. 概述与范围

### 1.1 迁移背景

AnvilLib 的两个长期分支在 merge-base `81c5a9c` 之后完全分叉：`dev/26.1` 是演进主线（独有 170 提交），沿 1.21.2 → 1.21.11 →
26.1 的版本链升级，并完成了 `module.gradle` 聚合构建体系重构；`port/1.21.1` 是维护与特性分支（独有 46 提交），在冻结的 1.21.1
平台上落地了网络库、wheel、multiblock 动态系统、油库（yukkuri）模块并修复了一批 bug。

`docs/branch-comparison.md` 已对两侧关系做了完整分类：port 侧 46 个独有提交中，约 23 个与 dev 早期提交同主题镜像，真正需要回迁的独有内容收敛为
**8 项（canonical 编号 #1–#8）**。其中 `module.yukkuri` 整模块（#1）是唯一大项，其余为小型 bugfix、API 补充与发布配置。

本文档即这 8 项的执行计划： **每项给出目标、步骤（含具体 git 命令）、API 适配点与验收方式**，并汇总构建/CI
接入点、不做清单、建议执行顺序与总体验收标准。

### 1.2 范围界定

本计划 **包含**：

- 8 个回迁项（#1–#8）的完整执行方案与验收判据；
- 回迁涉及的构建 / CI / README 联动修改（仅 #1 与 #8 涉及）；
- 每个回迁项与 port 侧来源提交、dev/26.1 落点文件的一一对应关系。

本计划 **不包含**（即「不做清单」的完整理由见第 5 章）：

- dev 侧既有演进的任何反向搬运（动态多方块事件 `1e5bfb3` #69、未加载区块快照修复 `d888714` #73、`@NullMarked` 统一
  `3bf9b6c`/`f94126f`、`module.gradle` 构建体系 `ea4301f`、`@ApiStatus.Internal` `c72aa30` #93 等）；
- codec / network / config / integration / registrum / wheel / moveable-entity-block 七个共享模块的任何内容（dev
  侧为超集或同源重写，见第 5 章）；
- port 侧各模块独立构建脚本与 lang datagen 产物（构建体系已被 dev 侧聚合取代，lang 由 datagen 统一生成）。

**迁移方式**：两侧无共同提交，直接 merge 方向错误且冲突不可控，故采用 **受控回迁**——在 dev/26.1 分支上按 #1–#8
逐个以独立提交落地，每个提交只改本项内容，便于评审与回滚。

### 1.3 前置条件与约定

| 项       | 约定                                                                                                                            |
|----------|---------------------------------------------------------------------------------------------------------------------------------|
| 编号     | 回迁项一律使用 canonical 编号 **#1–#8**，与 `docs/branch-comparison.md` 第 5.2 节一致                                           |
| 基线     | dev/26.1 @ `0b7e45f`（文档描述的全部落点均以该提交为准；执行时如 HEAD 已前进，需先以 `git log` 复核落点文件未变）               |
| 提取方式 | 一律用 `git show port/1.21.1:<路径>` 提取源码，**不**进行分支切换、不产生 git 写入操作                                          |
| 工作区   | 回迁在 dev/26.1 分支工作树执行；只读对比可用 `git diff port/1.21.1 dev/26.1 -- <路径>` 或临时工作树（`git -C <临时目录> diff`） |
| 验证基线 | dev 侧构建命令：`gradlew :module.xxx:build`、`gradlew :module.xxx:compileJava`、`gradlew runData`（模块需要 datagen 时）        |
| 待验证   | 所有标注「待验证」的条目均已给出验证方法，执行时须完成验证后才能标记该项完成                                                    |
| API 事实 | NeoForge 26.1.2.76 的流体能力 API 事实见 3.1.4 小节（已实测），其余 26.1 API 差异以编译期核对为准                               |

### 1.4 回迁项一览（结论先行）

| #  | 内容                                                   | 难度  | 一句话结论                                                                                                 |
|----|--------------------------------------------------------|-------|------------------------------------------------------------------------------------------------------------|
| #1 | `module.yukkuri` 整模块                                | 高    | 唯一大项；14 个 Java 文件 + 构建/元数据/README，需按 dev 聚合结构落位并做 26.1 流体 API 适配（推荐选项 B） |
| #2 | `InWorldRecipe.matches()` 谓词栈回滚                   | 低    | 纯逻辑搬运，无 API 变更                                                                                    |
| #3 | `ItemResourceHandlerCacheElement.sync()` 短路/差量写入 | 中    | dev 侧已换 `ResourceHandler` 体系，需 Transaction 内 extract+insert 重写                                   |
| #4 | `ClientTickRecorder` 修复                              | 低    | 判空 + 退出归零两处小改，保留 dev 侧 modid 写法                                                            |
| #5 | `BlockStatePredicate.Builder.with(BlockState)`         | 低-中 | 改走 dev 侧泛型重载实现；与 #6 联动决策                                                                    |
| #6 | `MultiblockDefinition` 16 个便捷重载                   | 中    | 依赖 #5；若判定 dev 删除属有意精简则整体放弃                                                               |
| #7 | `module.test` multiblock 测试组                        | 中-高 | 按 dev 侧 module.test 新结构落位，适配 26.1 注册 API                                                       |
| #8 | `module.main` CurseForge 环境两行                      | 低    | `publishMods` 块加两行，无其他影响                                                                         |

---

## 2. 回迁项总表

### 2.1 总表（含前置依赖）

> 内容、来源提交、落点与难度与 `docs/branch-comparison.md` 第 5.2 节完全一致；「前置依赖」列为本文档新增，用于编排执行顺序。

| # | 内容                                                                                                                                           | 来源提交                                               | 落点（dev/26.1）                                                                                                      | 难度             | API 适配点                                                                                                       | 前置依赖                                                                                                  |
|---|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|------------------|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| 1 | module.yukkuri 整模块（14 Java 文件 + build + mods.toml + README）                                                                             | `569e01d` #88、`18b29ca` #89                           | 新模块 `module.yukkuri/`                                                                                              | 高（最大项）     | `ResourceLocation`→`Identifier`；流体 API（见 3.1.4）；`Level.getCapability` 签名核对                            | 无代码前置；需 `settings.gradle` / `module.main` 依赖 / CI（`modules.json`）/ README 四处联动（见 3.1.3） |
| 2 | module.recipe：`InWorldRecipe.matches()` 谓词栈回滚                                                                                            | `edd71ec` #83                                          | `InWorldRecipe.java`（同名同包）                                                                                      | 低（纯逻辑搬运） | 无                                                                                                               | 无                                                                                                        |
| 3 | module.recipe：`ItemResourceHandlerCacheElement.sync()` 短路/差量写入                                                                          | `e78d020` #84                                          | `cache/item/ItemResourceHandlerCacheElement.java`（port 侧原名 `ItemHandlerCacheElement`，dev 侧已随 `2033fd9` 改名） | 中               | dev 侧 `ResourceHandler` 无 `setResource`，需在 Transaction 内 extract+insert 实现原子替换                       | 无                                                                                                        |
| 4 | module.util：`ClientTickRecorder` 修复（主菜单计 tick / 退出归零）                                                                             | `9874db7`                                              | `util/ClientTickRecorder.java`                                                                                        | 低               | 无；保留 dev 侧 `@EventBusSubscriber(modid=AnvilLibUtil.MOD_ID,...)` 写法，勿搬 port 侧无 modid 写法             | 无                                                                                                        |
| 5 | module.util：`BlockStatePredicate.Builder.with(BlockState)`                                                                                    | `5fc6f65`                                              | `predicate/BlockStatePredicate.java`                                                                                  | 低-中            | port 实现用 `Property.getName(T)`（26.1 已移除）→ 改用 dev 侧泛型重载 `with(Property<T>, T)`；与 #6 联动决策     | 无                                                                                                        |
| 6 | module.multiblock：`MultiblockDefinition` 的 16 个 `BlockState` 便捷重载                                                                       | `5fc6f65`                                              | `dynamic/definition/MultiblockDefinition.java`                                                                        | 中               | 依赖 #5 恢复 `with(BlockState)`；若判定 dev 侧删除是有意 API 精简则放弃整项                                      | **#5**                                                                                                    |
| 7 | module.test：multiblock 动态系统测试组（TestControllerBlock + LibBlocks/LibItemGroups/LibMultiblockControllers/LibMultiblocks + datagen 注册） | port 特有（`5fc6f65` 动态系统配套，`ba395e7` datagen） | 按 dev 侧 module.test 新结构落位                                                                                      | 中-高            | `@Mod` 构造器去 ModContainer；`REGISTRUM.addDataGenerator` 替代 GatherDataEvent；`ResourceLocation`→`Identifier` | **#6**（测试组用到 `MultiblockDefinition.builder()` 便捷重载与 `BlockStatePredicate`）                    |
| 8 | module.main/build.gradle：curseforge `clientRequired=true`/`serverRequired=true`                                                               | `1311782` #90                                          | `module.main/build.gradle` publishMods 块                                                                             | 低（两行）       | 按 dev 侧结构落位；勿把 port 侧 `jarJar(api ...)` 写法搬回                                                       | 无                                                                                                        |

### 2.2 编号一致性说明

- 本表 #1–#8 与 `docs/branch-comparison.md` 5.2 节一一对应，本次回迁以本表为唯一执行依据。
- #5 与 #6 同源（均出自 port 提交 `5fc6f65`），#5 是 #6 的 **代码级前置**（`MultiblockDefinition` 的 `BlockState` 重载内部调用
  `BlockStatePredicate.builder().with(state)`）；二者共享同一个「放弃则一并放弃」的联动决策（见 3.6.3）。
- #1 无代码前置，但牵涉 4 处工程联动（`settings.gradle`、`module.main/build.gradle`、`.github/modules.json`
  、README），建议最先执行以尽早暴露 26.1 流体 API 适配风险。
- #7 依赖 #6 的便捷重载（测试定义 `LibMultiblocks` 使用 `addController(...)` / `add(...)` /
  `seriaBuilder().mapController(...)` 等），故排在 #6 之后。
- #8 独立，可随时执行，安排在最后作为收尾。

---

## 3. 逐项详细计划

> 每节统一包含四部分： **目标**（一句话）、 **步骤**（具体到 git 命令与落点文件）、 **API 适配点**（旧 → 新）、 **验收方式**。步骤中的
> `git show port/1.21.1:<路径>` 均为只读操作。

### 3.1 迁移项 #1：`module.yukkuri` 整模块（高 · 最大项）

#### 3.1.1 目标

把 port 侧独有的油库/汽化运行时 `module.yukkuri`（14 个 Java 文件 + 构建文件 + mods.toml + README）整体迁移到 dev/26.1，按
dev 侧 `module.gradle` 聚合结构落位为独立模块 `module.yukkuri/`（坐标 `anvillib-yukkuri-neoforge-26.1`），完成 26.1 API
适配并接入构建、CI 发布顺序与 README。

#### 3.1.2 来源与现状（已核实）

| 项                       | 值                                                                                                                                                                                                                                                                                                                      |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 来源提交                 | `569e01d`（#88「添加油库（里）」，21 文件 / 739 行新增）、`18b29ca`（#89「ci: publish yukkuri before aggregate module」，CI 发布顺序）、`fab551b`（#91「修油库」，port 当前 HEAD，收尾修复）                                                                                                                            |
| port 侧文件清单          | 18 个：`module.yukkuri/README.md`、`build.gradle`（270 行独立脚本）、`gradle.properties`（3 属性）、`src/main/resources/META-INF/neoforge.mods.toml`（25 行），以及 14 个 Java 文件                                                                                                                                     |
| Java 文件（14）          | `Yukkuri.java`；`api/event/LargeCauldronProcessEvent.java`；`api/vapor/` 下 `IVaporConsumer` / `VaporAction` / `VaporStack` / `VaporizationCauldron` / `VaporizationContext` / `VaporizationManager` / `VaporizationOffer` / `VaporizationSource` / `VaporizationSources` / `YukkuriCapabilities` / `YukkuriVaporTypes` |
| 包名                     | `dev.anvilcraft.lib.v2.yukkuri`（与 dev 侧 `v2` 包体系一致，无需改包）                                                                                                                                                                                                                                                  |
| 模块间依赖               | **无**（已逐文件核对 import：仅引用自身包 + MC/NeoForge 类，不依赖 util/codec 等 anvillib 模块）→ CI 拓扑 `needs: []`，Level 0                                                                                                                                                                                          |
| port 侧独立构建脚本      | 不回迁（见第 5 章）；dev 侧由根 `build.gradle` 的 `subprojects` 块统一 `apply from: rootProject.file("module.gradle")` 接管                                                                                                                                                                                             |
| 行为契约                 | `VaporizationManager.tick(ServerLevel, VaporizationCauldron)` 是运行时入口，由外部大锅实现（AnvilCraft）在服务器 tick 调用；`YukkuriCapabilities.VAPOR_CONSUMER` 通过 `BlockCapability.createSided` 注册于大锅正上方方块，查询方向 `Direction.DOWN`                                                                     |
| port 收尾修复（fab551b） | `VaporizationManager.java`（+24/-2）、`VaporizationSource.java`（+6/-1）——迁移时直接以 `port/1.21.1` 侧文件为准，天然包含该修复                                                                                                                                                                                         |

#### 3.1.3 迁移步骤

**阶段 A：源码导出与落位**

```bash
# 1) 在 dev/26.1 分支工作树创建目录（执行时在 dev/26.1 分支，以下为只读提取）
mkdir -p module.yukkuri/src/main/java/dev/anvilcraft/lib/v2/yukkuri/api/{event,vapor}
mkdir -p module.yukkuri/src/main/resources/META-INF

# 2) 逐文件从 port 侧导出（示例；共 14 个 Java 文件，路径见 3.1.2 表）
git show port/1.21.1:module.yukkuri/src/main/java/dev/anvilcraft/lib/v2/yukkuri/Yukkuri.java \
  > module.yukkuri/src/main/java/dev/anvilcraft/lib/v2/yukkuri/Yukkuri.java
# 其余 13 个 Java 文件同理，逐一落位；可用循环批量提取：
for f in $(git ls-tree -r --name-only port/1.21.1 -- module.yukkuri | grep '\.java$'); do
  git show "port/1.21.1:$f" > "${f/port\/1.21.1\//}"  # 按 dev 工作树相对路径落位
done

# 3) 导出 README 与 mods.toml 作为内容底稿（build.gradle / gradle.properties 不回迁，见阶段 B）
git show port/1.21.1:module.yukkuri/README.md > module.yukkuri/README.md
git show port/1.21.1:module.yukkuri/src/main/resources/META-INF/neoforge.mods.toml \
  > module.yukkuri/src/main/resources/META-INF/neoforge.mods.toml
```

**阶段 B：构建文件按 dev 聚合结构重写（不搬 port 侧 270 行独立脚本）**

- `module.yukkuri/build.gradle`：dev 侧普通模块的 build.gradle 只保留 `dependencies {}` 块（如 `module.util/build.gradle`
  的形态），其余（版本、neoForge 配置、runs、processResources 替换、sourcesJar/javadocJar、publishing、jreleaser）全部由根级
  `module.gradle` 聚合提供：

```groovy
// module.yukkuri/build.gradle（按 dev 侧 module.util/build.gradle 精简）
dependencies {
    // yukkuri 无 anvillib 模块间依赖，此块可为空；未来引入依赖时按 NOT_DEV 双分支写法
}
```

- `module.yukkuri/gradle.properties`：dev 侧模块统一只含 3 个属性（参照 `module.util/gradle.properties`）：

```properties
## Mod Properties
mod_id=anvillib_yukkuri
mod_name=AnvilLib-Yukkuri
mod_description=Large-cauldron vaporization API and runtime for AnvilCraft addons
```

> 注：dev 侧 `module.gradle` 会读取 `anvillib.needRunConfig*` 系列属性（如 `module.explosion/gradle.properties` 设
> `anvillib.needRunConfig=true`、`anvillib.needRunConfig.data=true`），未设置的模块等价于不生成 run 配置，yukkuri 默认
> **不设置**（无 datagen / gametest 需求）；如迁移后需要本地冒烟运行，可临时加 `anvillib.needRunConfig=true`，验证完删除。

- `module.yukkuri/src/main/resources/META-INF/neoforge.mods.toml`：以 dev 侧模块模板为准（参照 `module.util` 的
  mods.toml），保留 `[[mods]]` 与 `[[dependencies]]`（neoforge / minecraft 两条）；port 侧版本无 mixin， **删除模板中的
  `[[mixins]]` 块**（模板默认含 `[[mixins]] config = "${mod_id}.mixins.json"`，yukkuri 没有 mixin 配置文件，保留会触发加载失败）；
  `displayURL`、`authors` 等占位符由 `processResources` 统一替换（`module.gradle` 已配置）。

**阶段 C：`settings.gradle` 注册（对照 `569e01d` 对 port 侧 settings.gradle 的改动，改到 dev 侧对应位置）**

```groovy
// settings.gradle：include 列表加一行（与 dev 侧模块并列）
include 'module.yukkuri'
// settings.gradle：project 改名表加一行（dev 侧坐标为 -neoforge-26.1 后缀）
project(':module.yukkuri').name = 'anvillib-yukkuri-neoforge-26.1'
```

**阶段 D：`module.main/build.gradle` 聚合依赖（使用 dev 侧既有写法，NOT_DEV 双分支各加一行）**

```groovy
// module.main/build.gradle：dependencies 块，NOT_DEV == 'true' 分支
jarJar(api("dev.anvilcraft.lib:anvillib-yukkuri-neoforge-26.1:latest.release"))
// module.main/build.gradle：dependencies 块，else（本地开发）分支
jarJar(implementation project(":anvillib-yukkuri-neoforge-26.1"))
```

> 严格沿用 dev 侧现有 19 个模块的两行写法（见 `module.main/build.gradle` 的 `dependencies` 块）； **不要**把 port 侧
> `569e01d` 中顺带做的 `jarJar(implementation ...)` → `jarJar(api ...)` 批量改写搬回（那是 port 侧的历史改动，dev 侧保持现状）。

**阶段 E：CI 发布顺序（对照 `18b29ca`，按 dev 侧机制落位）**

- port 侧 `18b29ca` 的做法：在 `ci.yml` 手工新增 `yukkuri` 与 `yukkuri-maven-central-deploy` 两个 job，并把 `yukkuri` 加入
  `main` job 的 `needs` 列表，确保 yukkuri 在聚合模块 `main` 之前构建并发布。
- dev 侧机制不同（`aa59e37` 重构 + `7f27908` 同步工作流）：`ci.yml` 的 `prepare` job 执行
  `.github/workflows/generate-matrix.js`，读取 `.github/modules.json` 做拓扑分层，自动生成 `build-l0/l1/l2` 与
  `deploy-l0/l1/l2` 矩阵；`main` job `needs: [build-l2]` 传递依赖所有层级。因此 **只需在 `.github/modules.json`增加一行**：

```json
{
  "module": "yukkuri",
  "needs": []
}
```

- `generate-matrix.js` 会从模块名自动推导 `module_id=anvillib-yukkuri`、`mod_id=anvillib_yukkuri`（与 port 侧手工声明一致）；
  `build_and_test.yml` 默认 `GRADLE_PROJECT=anvillib-yukkuri-neoforge-26.1`、`MODULE_DIR=module.yukkuri`，无需额外参数。
- 效果等价于 `18b29ca` 的意图：yukkuri 位于 Level 0（无依赖），进入 `build-l0` / `deploy-l0`，先于 `main`（`needs: [build-l2]`
  ）构建发布；`main` 侧无需改动 `needs`。
- 验收：本地 `node .github/workflows/generate-matrix.js` 逻辑核对（设置 `GITHUB_WORKSPACE` 后运行，确认 `level_0` 包含
  `yukkuri`）；或在 PR 分支观察 CI 的 `build-l0` 出现 yukkuri 任务。 **待验证**：`modules.json` 是否需要在 `needs`
  中补充（yukkuri 无模块间依赖，理论为 `[]`；如编译期发现引用了其他模块则补入对应 needs）。

**阶段 F：README 更新（对照 `569e01d` 对 README.md / README.en.md 的 4 处改动，改到 dev 侧对应位置）**

| 落点                       | 改动                                                                                                                                                                                                                                                                                              |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `README.md`                | 模块功能表加一行 `\| **Yukkuri** \| 大锅汽化 API 与运行时 \|`（对照 port 侧 README 的中文表样式）；聚合模块清单（`- yukkuri`）与依赖坐标示例（`implementation "dev.anvilcraft.lib:anvillib-yukkuri-neoforge-26.1:2.0.0"`）各加一处，坐标后缀用 **`-neoforge-26.1`**（dev 侧坐标，勿写 `-1.21.1`） |
| `README.en.md`             | 英文版同位置同步 4 处（模块表 / 模块清单 / 两处依赖坐标示例），坐标后缀 `-neoforge-26.1`                                                                                                                                                                                                          |
| `module.yukkuri/README.md` | 直接采用 port 侧版本；其中「Dependency」小节引用的聚合坐标 `anvillib-neoforge-1.21.1` 需改为 dev 侧 `anvillib-neoforge-26.1`（或按 dev README 的当前聚合命名核对后改写）                                                                                                                          |

**阶段 G：26.1 API 适配（核心工作，详见 3.1.4）**

- 全局替换 `ResourceLocation` → `Identifier`（涉及 5 个文件：`Yukkuri.java`、`VaporStack.java`、`VaporizationSource.java`、
  `VaporizationSources.java`、`YukkuriVaporTypes.java`）。
- 流体交互层二选一（选项 A / 选项 B，推荐 B），见 3.1.4。
- `Level.getCapability` 调用点核对（`VaporizationManager.java` 第 40 行附近），见 3.1.4。
- 全部文件过一遍 `gradlew :module.yukkuri:compileJava` 编译，逐个消灭报错。

#### 3.1.4 API 适配点

以下事实来自本地 NeoForge **26.1.2.76 sources jar** 实测（`net/neoforged/neoforge/...` 源码，2026-08-10 核对）：

**(a) `ResourceLocation` → `Identifier`（1.21.5+ 官方改名，必改）**

| 文件                       | port 侧用法                                                                                    | 26.1 写法                                                           |
|----------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| `Yukkuri.java`             | `ResourceLocation.fromNamespaceAndPath(...)`、`public static ResourceLocation of(String path)` | `Identifier.fromNamespaceAndPath(...)`；方法返回类型改 `Identifier` |
| `VaporStack.java`          | `record VaporStack(ResourceLocation type, int amount)`                                         | `record VaporStack(Identifier type, int amount)`                    |
| `VaporizationSource.java`  | `ResourceLocation id();`                                                                       | `Identifier id();`                                                  |
| `VaporizationSources.java` | `Map<ResourceLocation, VaporizationSource>`                                                    | `Map<Identifier, VaporizationSource>`                               |
| `YukkuriVaporTypes.java`   | `public static final ResourceLocation GASEOUS_OIL = Yukkuri.of("gaseous_oil")` 等              | `Identifier`；`isStandard(@Nullable ResourceLocation id)` 同步改    |
| `YukkuriCapabilities.java` | `BlockCapability.createSided(Yukkuri.of("vapor_consumer"), IVaporConsumer.class)`              | `createSided` 首参类型即 `Identifier`（见下），**签名不变**         |

**(b) 流体能力 API 二选一（已实测）**

旧 API 仍存在但已弃用：

- `net.neoforged.neoforge.fluids.capability.IFluidHandler` 标注 `@Deprecated(since = "1.21.9", forRemoval = true)`
  （sources jar 第 25 行），其各方法（fill/drain 等）同步弃用；
- `net.neoforged.neoforge.fluids.FluidStack` 仍可编译使用；
- 提供迁移包装器 `static IFluidHandler of(ResourceHandler<FluidResource> handler)`（第 35 行）。

新 API：

- `net.neoforged.neoforge.transfer.ResourceHandler<T extends Resource>`：方法为 `size()` / `getResource(int)` /
  `getCapacityAsInt(int, Resource)` / `isValid(int, Resource)` / `insert(...)` / `extract(...)`， **无 `setResource`**；
- `net.neoforged.neoforge.transfer.fluid.FluidResource`：不可变，实现 `DataComponentHolderResource<Fluid>`，静态字段
  `CODEC` / `OPTIONAL_CODEC` / `STREAM_CODEC` / `EMPTY`。

| 选项                     | 做法                                                                                                                                                                                                                                                                                                                                                                                                                                | 影响面                                                                                                                         | 建议                                                                    |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| **A 最小移植**           | 继续使用已弃用的 `IFluidHandler` / `FluidStack`（port 侧代码几乎不改）                                                                                                                                                                                                                                                                                                                                                              | 可编译但有弃用告警；`forRemoval` 后必须再迁一次；与 dev 侧物品能力已资源处理器化的现状（`2033fd9`）不一致                      | 不推荐，仅作兜底                                                        |
| **B 适配新 API（推荐）** | 用 `ResourceHandler<FluidResource>` 重写 yukkuri 的流体读写层：槽位为 `index`（对应 port 侧 `IFluidHandler` 的 tank 序号，注意 26.1 无 `getTanks()`，用 `size()`）；读写经 `getResource(index)` / `extract(index, resource, amount, transaction)` / `insert(index, resource, amount, transaction)`，`Transaction.openRoot()` + `commit()` 包裹；`FluidStack` 相关转换经 `FluidResource.toStack(...)` / `FluidResource.of(...)` 完成 | 一步到位，与 dev/26.1 物品能力迁移方向一致；`IVaporConsumer` 接口若暴露流体类型给外部（AnvilCraft 大锅实现），需同步约定新类型 | **推荐**（理由：与 dev 侧既有迁移方向一致、无弃用债、类型不可变更安全） |

> yukkuri 中实际触碰流体的点：`VaporizationManager` 的 `context.topFluid()`（`FluidStack`）与
> `VaporizationSource.createOffer(...)` 的 `FluidStack available` 出入参；选择 B 后这些签名改为 `FluidResource`
> （或保留内部转换、接口面最小化—— **待验证**：与 AnvilCraft 大锅实现的对接契约，需在 dev 侧仓库外确认调用方期望）。迁移完整性核对方法：14
> 个 Java 文件逐一 grep `FluidStack` / `IFluidHandler`，确认零残留后编译。

**(c) Capability 查询与注册**

- `BlockCapability.createSided(Identifier, Class<T>)` 在 26.1.2.76 **仍存在**（sources jar `BlockCapability.java` 第 119
  行：`public static <T> BlockCapability<T, @Nullable Direction> createSided(Identifier name, Class<T> typeClass)`
  ），仅标识符类型改为 `Identifier`——`YukkuriCapabilities.VAPOR_CONSUMER` 声明只需改 `ResourceLocation`→`Identifier`。
- `VaporizationManager.findConsumer(...)` 中的
  `context.level().getCapability(YukkuriCapabilities.VAPOR_CONSUMER, context.outletPos(), Direction.DOWN)`：
  `Level.getCapability` 在 26.1 的签名与上下文参数（BlockCapability / BlockPos / Direction） **待验证**——NeoForge sources
  jar 不含 MC patched 源码（`world/level/Level.java` 不在 jar 内），验证方法：① 迁移后直接编译，以编译错误定位签名；②
  `gradlew :module.yukkuri:compileJava` 通过后，在 `runData` / 运行环境用 IDE 反编译 `Level.getCapability` 确认语义（返回
  `@Nullable`、查询 side 传参顺序）。

#### 3.1.5 验收方式

1. **编译**：dev/26.1 分支执行 `gradlew :module.yukkuri:build` 通过（含 `compileJava`、`jar`、`sourcesJar`、`javadocJar`）；若选
   B，构建日志 **无弃用 API 告警**（`IFluidHandler` / `FluidStack` 相关 deprecation 警告为零）。
2. **产物**：`module.yukkuri/build/libs/` 生成 `anvillib-yukkuri-neoforge-26.1-<version>.jar`；`unzip -l` 核对包含 14
   个类文件、`META-INF/neoforge.mods.toml`（替换后变量正确）、README； **不含** mixin 配置引用。
3. **聚合**：`gradlew :module.main:build` 通过，主库 jar 的 `META-INF/jarjar/` 内含`anvillib-yukkuri-neoforge-26.1-*.jar`。
4. **运行时行为**（#1 特有验收）：在 dev/26.1 运行 `gradlew :module.main:runServer`（或 gameTest），构造大锅 + 上方消费者环境，验证：
    - `VaporizationManager.tick(ServerLevel, VaporizationCauldron)` 每服务器 tick 被调用且无异常；
    - 汽化流程：源（`VaporizationSources`）→ 流体抽取 → `IVaporConsumer.receiveVapor`（SIMULATE 不改变状态）→ 执行态差额入账；
    - 出口密封（`isOutletBlocked` / `sealsOutlet`）回压路径行为与 port 侧一致；
    - `YukkuriCapabilities.VAPOR_CONSUMER` 在 `Direction.DOWN` 查询正确返回消费者。
    - 若 module.test 迁入后（#7），可在 dev 侧为 yukkuri 补一条 gameTest 冒烟（ **可选，不在 #1 范围**）。
5. **CI**：PR 流水线 `build-l0` 矩阵出现 `yukkuri` 且通过；`deploy-l0` 发布
   `dev.anvilcraft.lib:anvillib-yukkuri-neoforge-26.1` 成功（若在发布分支触发）；`main` 构建不受影响。
6. **git 比对**：`git diff port/1.21.1 dev/26.1 -- module.yukkuri` 在迁移完成后应只剩「预期差异」（API
   适配、构建脚本形态、坐标后缀），无意外缺漏。

#### 3.1.6 文件级适配核对表（14 个 Java 文件逐一核对）

> 下表基于对 `port/1.21.1` 侧 14 个文件的逐文件 import / 用法扫描（2026-08-10）。执行时以此表为清单，按「已适配 /
> 无需适配」逐项打勾，勾满后进入编译验收。

| 文件                                       | API 触点（port 侧实测）                                                                                                                                        | 26.1 适配内容                                                                                                                                           | 选项 B 附加影响                                                                                                                      |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `Yukkuri.java`                             | `ResourceLocation.fromNamespaceAndPath`（`of(String)` 工厂，1 处）                                                                                             | `ResourceLocation`→`Identifier`，`of` 返回类型同步改                                                                                                    | 无                                                                                                                                   |
| `api/event/LargeCauldronProcessEvent.java` | 无（纯事件 record，携带 context 与阶段）                                                                                                                       | 无                                                                                                                                                      | 无                                                                                                                                   |
| `api/vapor/IVaporConsumer.java`            | 无（vapor 层面接口：`receiveVapor(VaporStack, VaporAction, VaporizationContext)`，不触碰流体类型）                                                             | 无                                                                                                                                                      | 无                                                                                                                                   |
| `api/vapor/VaporAction.java`               | 无（`EXECUTE` / `SIMULATE` 枚举）                                                                                                                              | 无；语义与 26.1 Transaction 的 execute/simulate 对应（见下）                                                                                            | 语义映射：EXECUTE→commit，SIMULATE→不 commit / 嵌套回滚                                                                              |
| `api/vapor/VaporStack.java`                | `record VaporStack(ResourceLocation type, int amount)`                                                                                                         | `ResourceLocation`→`Identifier`                                                                                                                         | 无                                                                                                                                   |
| `api/vapor/VaporizationCauldron.java`      | **大锅契约接口**：`FluidStack getTopVaporizationFluid()`、`FluidStack drainVaporizationFluid(FluidStack, IFluidHandler.FluidAction)`（import `IFluidHandler`） | 弃用 API 面最大的一处：接口暴露 `IFluidHandler.FluidAction`（随 `IFluidHandler` 弃用）                                                                  | 接口签名重设计：`FluidResource` + `boolean simulate`（或 Transaction 语义），**对外契约变化，需 AnvilCraft 大锅实现同步（见 V-Y3）** |
| `api/vapor/VaporizationContext.java`       | `FluidStack topFluid()`（第 25 行，封装大锅流体读取）                                                                                                          | 保持 `FluidStack`（选 A）或改 `FluidResource`（选 B）                                                                                                   | 签名改 `FluidResource`，调用方（Manager / Source）同步                                                                               |
| `api/vapor/VaporizationManager.java`       | `FluidStack`（`processFirstSource` 的 `available` 等）+ `level.getCapability(...)`（`findConsumer`，第 40 行）                                                 | `Level.getCapability` 签名核对（见 3.1.4-c，V-Y1）；流体类型随选项                                                                                      | 核心改造文件：`topFluid()` / `createOffer` 出入参全链改 `FluidResource`；`fab551b` 收尾修复已包含在 port 侧最终版中，勿丢            |
| `api/vapor/VaporizationOffer.java`         | `record VaporizationOffer(FluidStack input, VaporStack output)`                                                                                                | 随选项                                                                                                                                                  | 组件类型改 `FluidResource`                                                                                                           |
| `api/vapor/VaporizationSource.java`        | `ResourceLocation id()` + `createOffer(VaporizationContext, FluidStack available, int maxAmount)`                                                              | `ResourceLocation`→`Identifier`；流体随选项                                                                                                             | `createOffer` 入参改 `FluidResource`（源实现方 API 变化）                                                                            |
| `api/vapor/VaporizationSources.java`       | `Map<ResourceLocation, VaporizationSource> SOURCES`（注册表，`register` / `getSources`）                                                                       | `ResourceLocation`→`Identifier`                                                                                                                         | 无                                                                                                                                   |
| `api/vapor/YukkuriCapabilities.java`       | `BlockCapability.createSided(Yukkuri.of("vapor_consumer"), IVaporConsumer.class)`                                                                              | 仅 `Identifier` 类型变化，**签名不变**（26.1 实测 `createSided(Identifier, Class<T>)` 存在）                                                            | 无                                                                                                                                   |
| `api/vapor/YukkuriVaporTypes.java`         | `GASEOUS_OIL` / `GASEOUS_WATER` 常量 + `isStandard(ResourceLocation)`                                                                                          | `ResourceLocation`→`Identifier`；标准 ID 字符串 `yukkuri:gaseous_oil` / `yukkuri:gaseous_water` **保持不变**（历史存档与配方兼容，见 port README 约定） | 无                                                                                                                                   |

> 核对方法：迁移后执行 `grep -rn "ResourceLocation\|IFluidHandler\|FluidStack" module.yukkuri/src`——`ResourceLocation` 应为
> 0 匹配；`IFluidHandler` / `FluidStack` 选 B 时应为 0 匹配（选 A 时允许存在但必须逐处加 `@SuppressWarnings("deprecation")`
> 并登记到待验证 V-Y2）。

---

### 3.2 迁移项 #2：`module.recipe`：`InWorldRecipe.matches()` 谓词栈回滚（低）

#### 3.2.1 目标

把 `edd71ec`（#83）对 `InWorldRecipe.matches()` 的谓词栈回滚修复搬到 dev 侧同名同包文件，修复「匹配失败后未回滚谓词栈导致后续匹配串栈」的鱼缸加工
bug。

#### 3.2.2 步骤

```bash
# 1) 查看 port 侧修复的完整 diff（已核实：+17/-5，单文件）
git show edd71ec
# 2) 查看 dev 侧现状（已核实：仍是修复前的旧逻辑——失败路径 context.getStack().clear()，
#    成功路径 getStack().forEach(predicate -> predicate.clearStack(context))）
git show dev/26.1:module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java
# 3) 在 dev/26.1 工作树按 diff 语义改 module.recipe/.../InWorldRecipe.java 的 matches()：
#    - 进入 matches() 先记录 initialStackSize = context.getStack().size()；
#    - 两处失败路径改调新增私有方法 rollbackPredicates(context, initialStackSize)：
#      while (stack.size() > initialStackSize) context.pop(stack.getLast())；
#    - 成功路径改为仅清理由本次匹配新增的谓词：
#      for (int i = initialStackSize; i < stack.size(); i++) stack.get(i).clearStack(context)；
#    - 文件末尾补换行（port 侧修复顺带修掉了 "\\ No newline at end of file"）
```

**API 适配点**：无。两侧 `InWorldRecipe.java` 同名同包（`dev.anvilcraft.lib.v2.recipe`），`InWorldRecipeContext` /
`IRecipePredicate` / `ShapelessMatcher` 接口在 dev 侧均存在且语义一致；纯逻辑搬运，仅需确认 dev 侧 `context.pop(...)` 与
`getStack().getLast()` 可用（`List.getLast()` 为 JDK 21+，dev 侧 Java 25 工具链无问题；`pop` 方法是否存在 **待验证**
，验证方法：改完直接 `gradlew :module.recipe:compileJava`，如无 `pop` 则对照 dev 侧 `InWorldRecipeContext` 的栈操作 API
等价实现）。

#### 3.2.3 验收方式

1. `gradlew :module.recipe:compileJava` 通过。
2. 行为核对：对照 port 侧 `edd71ec` 的 diff 逐行比对 dev 侧改动一致（`git diff` 语义等价）。
3. 回归：dev 侧 `module.test` 补一条鱼缸加工场景回归用例（ **待验证**：dev 侧 module.test 是否已有 InWorldRecipe
   相关测试设施；若有则扩展，若无则按 #7 方式新建最小用例），验证：匹配失败后 `context.getStack()`
   尺寸回到初始值、后续配方可正常匹配；成功后仅新增谓词被清理。
4. `gradlew :module.recipe:test`（若有测试）通过。

---

### 3.3 迁移项 #3：`module.recipe`：`ItemResourceHandlerCacheElement.sync()` 短路/差量写入（中）

#### 3.3.1 目标

把 `e78d020`（#84）对缓存元素 `sync()` 的「无谓写入短路 + 差量写入」逻辑迁移到 dev 侧已资源处理器化的
`ItemResourceHandlerCacheElement.sync()`，避免每次同步都执行清空后重写的全量操作。

#### 3.3.2 步骤

```bash
# 1) 查看 port 侧修复的完整 diff（已核实：+20，单文件 ItemHandlerCacheElement.java）
git show e78d020
# 2) 查看 dev 侧现状（已核实：ItemResourceHandlerCacheElement.java，sync() 为无条件
#    extract-all + insert simulate，Transaction 包裹；无短路/差量）
git show dev/26.1:module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/item/ItemResourceHandlerCacheElement.java
# 3) 在 dev/26.1 工作树改写 sync()，语义对照 port 侧：
#    短路：getResource(slot) 与 simulate 转换后的 ItemStack 完全一致（数量 + 组件）→ 直接 return；
#    差量：isSameItemSameComponents 但数量不同 → 仅 extract/insert 差额；
#    全量替换：类型不同 → extract 全部后 insert simulate（即 dev 现有逻辑兜底）。
```

**API 适配点**（旧 → 新）：

| port 侧（1.21.1）                                                                  | dev 侧（26.1）                                                                                                                                    |
|------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `IItemHandler.getStackInSlot(slot)`                                                | `ResourceHandler<ItemResource>.getResource(slot)`（空时返回 `ItemResource.EMPTY`，需 `toStack()` 转 `ItemStack` 做比较）                          |
| `IItemHandlerModifiable.setStackInSlot(...)`（快捷整槽替换）                       | `ResourceHandler` **无 setResource**，此快捷路径不存在；统一走「extract 全量 + insert 目标」的 Transaction 原子替换（dev 现状即此模式）           |
| `IItemHandler.insertItem(slot, stack, false)` / `extractItem(slot, amount, false)` | `insert(slot, resource, amount, transaction)` / `extract(slot, resource, amount, transaction)`，`Transaction.openRoot()` + `transaction.commit()` |
| `ItemStack.isSameItemSameComponents(a, b)`                                         | 26.1 仍存在（`ItemStack` 静态方法，**待验证**：以编译为准；若移除则用 `ItemResource` 等价比较或 `ItemStack` 组件比对）                            |
| `stack.copyWithCount(n)`                                                           | 26.1 `ItemStack.copyWithCount` 仍存在（**待验证**，同上）                                                                                         |

> 注意 dev 侧类内已有私有辅助 `extract(ResourceHandler<ItemResource>, int)`（`ItemResource` → `ItemStack`
> 转换），差量实现可复用该模式；短路比较建议在 `Transaction` 开启 **之前**完成（只读 `getResource`，无需事务），确认需写入时再开事务。

#### 3.3.3 验收方式

1. `gradlew :module.recipe:compileJava` 通过。
2. 单元/集成测试（dev 侧 module.recipe 或 module.test）：构造 `ItemResourceHandlerCacheElement`，覆盖四类输入—— (a) 槽位与
   simulate 完全一致（期望：零写入调用）； (b) 同类型差量（期望：仅 extract/insert 差额，不触碰多余数量）； (c) 不同类型（期望：全量替换）；
   (d) 槽位空 / simulate 空（期望：与 port 侧行为一致）。
3. 写入次数可观测：在测试中包裹 `ResourceHandler` 实现并计数 `extract` / `insert` 调用，断言短路场景为 0 次、差量场景各 1
   次（避免空转写入路径回归）。
4. 行为回归：鱼缸加工（#83/#84 同源场景）在 dev 侧缓存流程中物品数量与组件与 simulate 一致。

---

### 3.4 迁移项 #4：`module.util`：`ClientTickRecorder` 修复（低）

#### 3.4.1 目标

把 `9874db7`（+9/-2）的两处修复搬到 dev 侧 `ClientTickRecorder`：主菜单（level 为 null）不累计 tick；退出世界时 tick 归零。

#### 3.4.2 步骤

```bash
# 1) 查看 port 侧修复 diff（已核实：onTick 加 level != null 判断 + 新增 LoggingOut 事件归零）
git show 9874db7
# 2) 查看 dev 侧现状（已核实：@EventBusSubscriber(modid = AnvilLibUtil.MOD_ID, value = Dist.CLIENT)，
#    onTick 仅 isPaused() 判断，无判空、无归零）
git show dev/26.1:module.util/src/main/java/dev/anvilcraft/lib/v2/util/ClientTickRecorder.java
# 3) 在 dev/26.1 工作树修改：
#    - onTick 判断改为：if (Minecraft.getInstance().level != null && !Minecraft.getInstance().isPaused())
#    - 新增 @SubscribeEvent onClientExit(ClientPlayerNetworkEvent.LoggingOut e) { ticks = 0; }
#    - 新增 import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
```

**API 适配点**：

| 项     | port 侧（9874db7 后）                                  | dev 侧应保留                                                                                                                                                                                          |
|--------|--------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 类注解 | `@EventBusSubscriber(value = Dist.CLIENT)`（无 modid） | **保留 dev 现状** `@EventBusSubscriber(modid = AnvilLibUtil.MOD_ID, value = Dist.CLIENT)`——dev 侧 `AnvilLibUtil.MOD_ID` 存在，带 modid 更精确（只接收本 mod 事件总线），**不搬** port 侧无 modid 写法 |
| 事件类 | `ClientPlayerNetworkEvent.LoggingOut`                  | 26.1 同包同类仍存在（**待验证**：以编译为准；若改名则对照 dev 侧 `ClientPlayerNetworkEvent` 现状）                                                                                                    |

> 搬逻辑（判空 + 归零）而不搬注解写法；`ClientPlayerNetworkEvent.LoggingOut` 在 26.1 客户端事件体系的可用性以
> `gradlew :module.util:compileJava` 验证。

#### 3.4.3 验收方式

1. `gradlew :module.util:compileJava` 通过。
2. 运行验收（`gradlew :module.main:runClient`）：主菜单停留 N 秒 → `/time` 或调试输出确认 `ClientTickRecorder.ticks`
   不增长；进入世界 → 正常累计（每 24 小时取模重置不变）；退出世界回主菜单 → 归零。
3. 源码核对：`git diff` 语义与 `9874db7` 等价（除保留 dev 侧 modid 写法外无其他差异）。

---

### 3.5 迁移项 #5：`module.util`：`BlockStatePredicate.Builder.with(BlockState)`（低-中）

#### 3.5.1 目标

在 dev 侧 `BlockStatePredicate.Builder` 恢复 `with(BlockState)` 便捷重载（#6 的代码级前置），为动态多方块提供「直接以方块状态描述谓词」的入口。

#### 3.5.2 步骤

```bash
# 1) 查看 port 侧实现（已核实：port 侧 Builder 有 5 个 with 重载，含 with(BlockState)；
#    dev 侧有 4 个（String/int/boolean/泛型 T），缺 with(BlockState)）
git show port/1.21.1:module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/BlockStatePredicate.java
git show dev/26.1:module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/BlockStatePredicate.java
# 2) 在 dev/26.1 工作树 BlockStatePredicate.Builder 内、现有 with(Property<?>, String) 之前
#    （或其他合适位置）新增：
#    public Builder with(BlockState state) {
#        for (Property<? extends Comparable<?>> property : state.getProperties()) {
#            this.with(property, state.getValue(property));   // 走 dev 侧泛型重载，见适配点
#        }
#        return this;
#    }
```

**API 适配点**（旧 → 新）：

- port 侧实现为 `this.with(property, property.getName(Util.cast(state.getValue(property))))`——`Property.getName(T)` 在
  26.1 已移除，且依赖 `Util.cast` 辅助。
- dev 侧已有泛型重载 `public <T extends Comparable<T>> Builder with(Property<T> property, T value)`，其内部处理
  `StringRepresentable → getSerializedName()`、其余 `String.valueOf(value)`——与 1.21.1 的 `Property.getName` 行为
  **完全等价**（`getName` 的 1.21.1 实现即为
  `value instanceof StringRepresentable ? ((StringRepresentable) value).getSerializedName() : String.valueOf(value)`）。
- 因此新实现直接调用 `this.with(property, state.getValue(property))`（泛型推断 `T` 为属性值类型）， **不**使用
  `property.getName`，也不引入 `Util.cast`（dev 侧 `Util` 类是否保留该辅助 **待验证**，但新实现无需它）。
- 泛型细节：`state.getProperties()` 返回 `Collection<Property<? extends Comparable<?>>>`，`state.getValue(property)`可推断为对应
  `T`；若编译器因通配符无法推断，可照 dev 侧泛型重载签名做强转（`@SuppressWarnings` 或 `Util.cast`，以编译为准）。 **待验证**
  ：通配符泛型调用的编译通过性（方法签名 `with(Property<T>, T)` 的 `T` 需绑定到 `Comparable<?>` 的实际类型，Dev 侧如遇编译错误，参照
  port 侧同文件其余泛型写法的强转模式解决）。

#### 3.5.3 验收方式

1. `gradlew :module.util:compileJava` 通过。
2. 行为测试：`BlockStatePredicate.builder().with(Blocks.IRON_DOOR.defaultBlockState())`
   构造谓词，对同状态与改属性后的状态分别断言匹配/不匹配（属性级精确匹配）；含 `StringRepresentable` 属性（如
   `FurnaceBlock.FACING`）与枚举/整型属性各一例。
3. 与 #6 联动：若 #6 判定放弃（见 3.6.3），本项 **一并放弃**（回滚本项改动）；决策前本项保持 pending 状态。

---

### 3.6 迁移项 #6：`module.multiblock`：`MultiblockDefinition` 的 16 个 `BlockState` 便捷重载（中 · 可选）

#### 3.6.1 目标

在 dev 侧 `MultiblockDefinition` 恢复 port 侧 `5fc6f65` 引入的 16 个便捷重载（`add` / `addController` 各 8 个，其中 8 个直接以
`BlockState` 入参），使动态多方块定义可用「具体方块状态」而非仅「方块类型」描述； **可选**——若判定 dev 侧删除为有意 API
精简则整体放弃（见 3.6.3）。

#### 3.6.2 步骤

```bash
# 1) 对比两侧 Builder 方法集（已核实）：
#    port 侧：add ×8（predicate/block/BlockState/block+BlockState/tag/block+tag/BlockState+tag/block+BlockState+tag）
#             + addController ×8（同构）
#    dev  侧：add ×4（predicate/block/tag/block+tag）+ addController ×4（同构）——缺全部 BlockState 入参变体
git show port/1.21.1:module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/definition/MultiblockDefinition.java
git show dev/26.1:module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/definition/MultiblockDefinition.java
# 2) 在 dev/26.1 工作树 MultiblockDefinition.java 的 Builder 内补 8 个重载：
#    add(Vec3i, BlockState) / add(Vec3i, Block, BlockState) / add(Vec3i, BlockState, CompoundTag) /
#    add(Vec3i, Block, BlockState, CompoundTag) 与 addController 对应 4 个；
#    实现均为委托 BlockStatePredicate.builder().with(state)（依赖 #5），如：
#    public Builder add(Vec3i localPos, BlockState state) {
#        return this.add(localPos, BlockStatePredicate.builder().with(state));
#    }
# 3) 同步核对 dev 侧 SeriaBuilder（map/mapController）是否需要 BlockState 变体：
#    port 侧 SeriaBuilder 无 BlockState 变体（已核实 map ×4 / mapController ×4 均无），
#    故 dev 侧 SeriaBuilder 保持现状，不动。
```

**API 适配点**：

- 依赖 #5 的 `with(BlockState)`；若 #5 按新实现落地，本项重载编译即通。
- dev 侧 `MultiblockDefinition` 其余结构（record 定义、`toGlobal`、`isController`、SeriaBuilder）与 port 侧一致（已核实），无其他
  26.1 API 适配点。
- `add(Vec3i, Block, BlockState)` 语义与 port 一致：`BlockStatePredicate.builder().of(block).with(state)`（先限定方块再限定属性）。

#### 3.6.3 联动决策（执行前置）

| 判据                                                                                                                             | 决策                                                           |
|----------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| dev 侧删除 `BlockState` 重载是**无意的迁移遗漏**（`7019b38` 移植时改写掉了）                                                     | 迁：本项 + #5 一起落地，测试组（#7）覆盖该路径                 |
| dev 侧删除是**有意的 API 精简**（对齐 `@ApiStatus.Internal` 化趋势，见 `c72aa30` #93；或维护者认为只暴露 `block` 级 API 更干净） | **放弃整项**：保留 dev 现状，不引入 port 侧便捷层，#5 一并放弃 |

> 该决策需在迁移执行前与 dev 侧维护者确认（本计划不替维护者做决定）；确认前 #5/#6 保持 pending。

#### 3.6.4 验收方式（若迁）

1. `gradlew :module.multiblock:compileJava` 通过。
2. 行为测试：以 `Blocks.IRON_DOOR.defaultBlockState()` 等含属性状态构造
   `MultiblockDefinition.builder().addController(state)`，与 `BlockStatePredicate` 匹配断言一致（#7 测试组覆盖成形/解体路径）。
3. 无行为回归：dev 侧现有 `MultiblockDefinition` 用例（如有）不受影响。

---

### 3.7 迁移项 #7：`module.test`：multiblock 动态系统测试组（中-高）

#### 3.7.1 目标

把 port 侧独有的动态多方块测试组（`TestControllerBlock` + `LibBlocks` / `LibItemGroups` / `LibMultiblockControllers` /
`LibMultiblocks` + datagen 注册）迁入 dev 侧 `module.test`，按 dev 侧 26.1
新结构落位，为动态多方块系统提供游戏内可运行验证（成形/解体行为 + 定义数据生成）。

#### 3.7.2 步骤

```bash
# 1) 查看 port 侧测试组文件（已核实清单）：
#    module.test/src/main/java/dev/anvilcraft/lib/v2/test/multiblock/block/TestControllerBlock.java
#    module.test/src/main/java/dev/anvilcraft/lib/v2/test/multiblock/init/LibBlocks.java
#    module.test/src/main/java/dev/anvilcraft/lib/v2/test/multiblock/init/LibItemGroups.java
#    module.test/src/main/java/dev/anvilcraft/lib/v2/test/multiblock/init/LibMultiblockControllers.java
#    module.test/src/main/java/dev/anvilcraft/lib/v2/test/multiblock/init/LibMultiblocks.java
#    module.test/src/main/java/dev/anvilcraft/lib/v2/test/AnvilLibTestDatagen.java（datagen 注册入口）
#    datagen 产物：src/generated/resources/data/anvillib_test/anvillib/definitions/{simple,complicated,waht}.json
# 2) 查看 dev 侧 module.test 新结构（已核实）：all/{TestBlocks,TestItems,TestItemGroups,TestTiles}、
#    data/TestLangGenerator、AnvilLibTest（@Mod(AnvilLibTest.MOD_ID) 无 ModContainer 参数；
#    REGISTRUM.addDataGenerator(ProviderType.LANG, ...) 替代 GatherDataEvent）
git ls-tree -r --name-only dev/26.1 -- module.test
# 3) 落位决策：新建包 module.test/src/main/java/dev/anvilcraft/lib/v2/test/multiblock/（与 port 同构），
#    或并入 dev 侧 all 包（待维护者偏好）；以下按「新建 multiblock 包」给出，代码语义与 dev all 包一致
# 4) 适配并落位（各文件适配点见 3.7.3），在 AnvilLibTest 构造器中注册：
#    LibItemGroups.setupRegistration(); LibBlocks.setupRegistration(); LibMultiblockControllers.setupRegistration();
#    并在 setupDataGeneration() 中注册定义数据生成（见适配点 d）
# 5) datagen 产物：dev 侧产物由 runData 生成（module.test 的 gradle.properties 需开
#    anvillib.needRunConfig.data=true，参照 dev 侧已有模块），不手工提交 port 侧 JSON；
#    生成后核对 simple/complicated/waht 三个定义文件内容
```

**API 适配点**（旧 → 新）：

| 点                                                          | port 侧                                                                                                                                                                                 | dev 侧（26.1）                                                                                                                                                                                                                                                                                                                                                             |
|-------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| (a) `@Mod` 构造器                                           | `AnvilLibTest(ModContainer container, IEventBus bus)`（port 侧现状）                                                                                                                    | `AnvilLibTest(IEventBus modBus)`——26.1 已去掉 ModContainer 参数（dev `AnvilLibTest` 现状即此签名）                                                                                                                                                                                                                                                                         |
| (b) datagen 注册                                            | `@EventBusSubscriber` + `GatherDataEvent`（`AnvilLibTestDatagen.gatherData` 空壳）+ `REGISTRUM.getDataGenInitializer().add(LibRegistries.DEFINITIONS_KEY, LibMultiblocks::bootstrap)`   | `REGISTRUM.addDataGenerator(...)` 体系（dev 现状）；`DataProviderInitializer` 在 dev 侧仍存在（`AbstractRegistrum.getDataGenInitializer()` 第 656 行）且 `RegistrumDatapackProvider` 支持 datapack registry（`getDatapackRegistryProviders()`）——`DEFINITIONS_KEY` 的 datapack 数据生成落位**待验证**（add 方法签名与 port 是否一致，以 dev 侧 registrum 源码 + 编译为准） |
| (c) `ResourceLocation`                                      | `TestControllerBlock.getDefinitionId()` 返回 `ResourceLocation`；`LibMultiblocks` 的 `ResourceKey` 构造                                                                                 | 全部 `Identifier`；`LibMultiblocks.key(...)` 的 `Identifier.fromNamespaceAndPath` 同步                                                                                                                                                                                                                                                                                     |
| (d) 注册链式 API                                            | `LibBlocks`：`REGISTRUM.block(...).blockstate((ctx, provider) -> {}).item().model((ctx, provider) -> {}).build().register()`；`LibItemGroups`：`DeferredRegister` + `REGISTRUM.addLang` | dev 侧 `TestBlocks` 用 `.properties(p -> ...).blockstate(() -> ...).simpleItem().register()`、`TestItemGroups` 用 `REGISTRUM.creativeTab("test_tab", () -> ...)`——**待验证**：dev 侧 `BlockEntry` builder 是否保留 `.blockstate(...)` / `.item()` / `.model()` / `.build()` 形态（对照 `module.registrum` 源码；若无则按 dev 现有链式 API 改写语义）                       |
| (e) `IController` / `ControllerRecord` / `SimpleController` | `onFormed` / `onUnformed` 回调                                                                                                                                                          | dev 侧 multiblock 已演进：`1e5bfb3`（#69）新增动态多方块事件、`22091d4` 状态管理改 `BlockPos`——`MultiblockState.getControllerPos()` 等接口**待验证**（以 dev 侧 `MultiblockState` / `IController` 现状为准微调测试回调体）                                                                                                                                                 |
| (f) 定义构造                                                | `MultiblockDefinition.builder().addController(Blocks.FURNACE).add(new Vec3i(0,1,0), Blocks.IRON_BLOCK)`                                                                                 | dev 侧 Builder 基础形态一致；若使用 `BlockState` 重载则依赖 #6                                                                                                                                                                                                                                                                                                             |

> 包内 `package-info.java`（`@NullMarked` 风格）按 dev 侧模块惯例补充（dev 侧 26.1 已统一 jspecify `@NullMarked`，见
> `3bf9b6c`/`f94126f` 演进）。

#### 3.7.3 验收方式

1. `gradlew :module.test:compileJava` 通过。
2. `gradlew :module.test:runData`（需开 `anvillib.needRunConfig.data=true`）生成产物，核对：
    - `data/anvillib_test/anvillib/definitions/simple.json`（熔炉控制器 + 铁块）、`complicated.json`（TestControllerBlock
      控制器 + 铁块）、`waht.json`（白床 + 分层 A/0）与 port 侧产物语义一致（序列化格式以 dev 侧 `DefinitionSerialization`
      为准）；
    - lang 产物（en_us / en_ud）含新增条目（若有）。
3. 运行验收（`gradlew :module.test:runServer` 或 gameTest）：进世界摆放 SIMPLE / COMPLICATED / WAHT 结构：
    - 成形：控制器方块被识别，`onFormed` 触发（如铁锭弹出 64 个）；
    - 解体：破坏结构，`onUnformed` 触发（铁栏杆弹出）；
    - 控制器方块（TestControllerBlock）本身可放置、可查询 `getDefinitionId`。
4. 覆盖联动：测试组运行路径覆盖 #2/#6 涉及的 `MultiblockDefinition` 便捷重载与谓词匹配逻辑。

---

### 3.8 迁移项 #8：`module.main`：CurseForge 环境两行（低）

#### 3.8.1 目标

在 dev 侧 `module.main/build.gradle` 的 `publishMods.curseforge` 块补充 `clientRequired = true` /`serverRequired = true`
两行环境声明（port 侧 `1311782` #90 已修复的发布元数据问题）。

#### 3.8.2 步骤

```bash
# 1) 查看 port 侧改动（已核实：publishMods.curseforge 块内 +2 行）
git show 1311782
# 2) 查看 dev 侧 publishMods 块（已核实：dev 侧结构与 port 同构——
#    file/changelog/type/modLoaders/additionalFiles/curseforge{accessToken,projectId,minecraftVersions}/modrinth）
git show dev/26.1:module.main/build.gradle
# 3) 在 dev/26.1 工作树 module.main/build.gradle 的 curseforge 块内
#    minecraftVersions.add(libs.versions.minecraft.get()) 之后追加两行：
#    clientRequired = true
#    serverRequired = true
```

**API 适配点**：

- 无 API 适配；`publishMods` 扩展属性（mod-publish-plugin 1.1.0，与 port 侧同版本，见 `gradle/libs.versions.toml`）在 dev
  侧可用。
- **不要**把 port 侧 `jarJar(api ...)` 批量写法或任何其他 port 侧 `module.main/build.gradle` 内容搬回（dev 侧保持现状）。
- **待验证**：mod-publish 的 `curseforge` 块中 `clientRequired` / `serverRequired` 属性在 1.1.0 版本的确切类型（boolean），
  `gradlew :module.main:compileGroovy` / 配置阶段即报错可发现；另注意 dev 侧是否已在其他发布块声明过环境（已核对 `0b7e45f`
  无）。

#### 3.8.3 验收方式

1. `gradlew :module.main:build` 配置阶段通过（publishMods 扩展解析无误）。
2. 发布 dry-run：`gradlew :module.main:publishMods`（或对应 dry-run 任务）在无 token 环境下不报扩展错误；CI
   发布分支实际发布后，CurseForge 元数据含「客户端必需 / 服务端必需」声明（ **待验证**：需一次真实发布核对页面元数据，或核对
   mod-publish 输出日志）。
3. `git diff` 与 `1311782` 语义一致（两行，无多余改动）。

---

## 4. 构建 / CI 接入点汇总表

> 仅 #1 与 #8 涉及工程接入；#2–#7 均为模块内源码改动，不触碰下列文件。所有改动均按 dev/26.1 现状结构落位，不回迁 port 侧对应文件。

| 文件                                                                    | 回迁项 | 改动内容                                                                                                                                                                                                                              | 对照来源                                                              |
|-------------------------------------------------------------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| `settings.gradle`                                                       | #1     | `include 'module.yukkuri'` + `project(':module.yukkuri').name = 'anvillib-yukkuri-neoforge-26.1'`（dev 侧坐标后缀）                                                                                                                   | port `569e01d`（坐标后缀改 `-neoforge-26.1`）                         |
| `module.yukkuri/build.gradle`（新建）                                   | #1     | 按 dev 侧精简形态：`dependencies {}` 块即可，其余由根级 `module.gradle` 聚合（`subprojects { apply from: rootProject.file("module.gradle") }`）提供                                                                                   | dev 侧 `module.util/build.gradle` 形态；**非** port 侧 270 行独立脚本 |
| `module.yukkuri/gradle.properties`（新建）                              | #1     | `mod_id=anvillib_yukkuri` / `mod_name=AnvilLib-Yukkuri` / `mod_description=...`；不设 `anvillib.needRunConfig*`（无 datagen/gametest 需求）                                                                                           | dev 侧 `module.util/gradle.properties`                                |
| `module.yukkuri/src/main/resources/META-INF/neoforge.mods.toml`（新建） | #1     | dev 模板落位；删 `[[mixins]]` 块（无 mixin）；`[[dependencies]]` 保留 neoforge / minecraft 两条                                                                                                                                       | dev 侧 `module.util` 模板；port `569e01d` 内容底稿                    |
| `module.main/build.gradle`                                              | #1     | `dependencies` 块 NOT_DEV 双分支各加一行：`jarJar(api("dev.anvilcraft.lib:anvillib-yukkuri-neoforge-26.1:latest.release"))` / `jarJar(implementation project(":anvillib-yukkuri-neoforge-26.1"))`                                     | dev 侧同块既有 19 模块写法；port `569e01d` 对应改动                   |
| `module.main/build.gradle`                                              | #8     | `publishMods.curseforge` 块 `minecraftVersions.add(...)` 后加 `clientRequired = true` / `serverRequired = true` 两行                                                                                                                  | port `1311782`                                                        |
| `.github/modules.json`                                                  | #1     | 增加 `{ "module": "yukkuri", "needs": [] }`（yukkuri 无模块间依赖 → Level 0）                                                                                                                                                         | port `18b29ca` 的 CI 意图（dev 侧机制为矩阵拓扑，等价实现）           |
| `.github/workflows/ci.yml`                                              | #1     | **无需改动**：`generate-matrix.js` 自动派生 `module_id=anvillib-yukkuri`、`mod_id=anvillib_yukkuri`、`GRADLE_PROJECT=anvillib-yukkuri-neoforge-26.1`、`MODULE_DIR=module.yukkuri`；`main` job 的 `needs: [build-l2]` 传递覆盖 Level 0 | 对比 port `18b29ca`（手工加 job + 改 main needs）                     |
| `.github/workflows/build_and_test.yml` / `publish_maven_central.yml`    | #1     | **无需改动**（参数默认值即匹配 yukkuri 命名）                                                                                                                                                                                         | —                                                                     |
| `README.md` / `README.en.md`                                            | #1     | 各 4 处：模块功能表 + 聚合模块清单 + 两处依赖坐标示例（坐标后缀 `-neoforge-26.1`）                                                                                                                                                    | port `569e01d`                                                        |
| `module.yukkuri/README.md`（新建）                                      | #1     | port 侧版本 + 聚合坐标改为 dev 侧 `-neoforge-26.1`                                                                                                                                                                                    | port `569e01d`                                                        |
| `module.test/gradle.properties`                                         | #7     | 若采用 runData 验证，加 `anvillib.needRunConfig.data=true`（参照 dev 侧 `module.explosion` 等）；验证后可保留（module.test 属开发模块）                                                                                               | dev 侧现有模块写法                                                    |

---

## 5. 不做清单与原因

> 以下内容 **明确不回迁**。理由均基于两侧 diff 分类结论（详见 `docs/branch-comparison.md` 第 5 章），执行时不得以「顺手」「保险」为由扩大范围。

| #   | 不回迁内容                                                                                                       | 原因（依据）                                                                                                                                                                                                                       |
|-----|------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| N1  | `module.codec` / `module.network` / `module.config` / `module.integration` / `module.registrum` / `module.wheel` | dev 侧为超集或已独立演进：port 侧相关提交（`0b56109`/`50aeac7`/`6a0e8a9`、网络库系列、wheel 系列）均有 dev 同主题对应（`3b5dfea`/`2e1b537`/`922d523`、`8d8ac8c` 系列、`0d73ab0`/`1ae3411`），逐文件 diff 无 port 独有内容          |
| N2  | `module.moveable-entity-block`（含 port `1acdcf8` PoseStack 修复）                                               | dev 侧以 26.1 渲染架构**同源重写**（`83477ab`/`b5b27aa`/`2d4cd84`）；port 侧修复的「PoseStack 提前 return 未清空」问题在新渲染架构下天然规避，搬运旧修复反而可能破坏新架构                                                         |
| N3  | port 侧各模块独立构建脚本与 `gradle.properties`（含 yukkuri 的 270 行 build.gradle）                             | 已被 dev 侧 `module.gradle` 聚合 + `gradle/libs.versions.toml` 版本目录取代（`ea4301f`/`16b7bb5`）；回迁即倒退，且两套构建体系并存会产生维护分歧                                                                                   |
| N4  | lang datagen 产物（`zh_cn.json` / `en_us.json` 等已生成 JSON）                                                   | dev 侧统一由 datagen 生成（`runData` 产出，块物品自行提供 lang 的工作流），直接提交产物与 dev 工作流冲突；需要语言内容时应以 datagen 代码回迁而非产物回迁（#7 即按此执行）                                                         |
| N5  | `module.multiblock/ASYNC_MULTIBLOCK_CHECK_PLAN.md`                                                               | 文档已标注「已完成实现」；dev 侧异步检查实现等价且更完善（含未加载区块快照修复 `d888714` #73），无迁移价值                                                                                                                         |
| N6  | `18b29ca` 的 CI 片段本身                                                                                         | CI 体系 dev 侧已整体重构（`aa59e37` 模块化矩阵 + `7f27908` 同步工作流），port 侧手工 job 片段不适用；#1 已按 dev 侧 `modules.json` 机制等价落位（见 3.1.3 阶段 E）                                                                 |
| N7  | dev 侧特有演进（不回迁方向）                                                                                     | 动态多方块事件 `1e5bfb3`（#69）、未加载区块快照修复 `d888714`（#73）、`@NullMarked` 注解统一 `3bf9b6c`/`f94126f`、`module.gradle` 构建体系 `ea4301f`、`@ApiStatus.Internal` `c72aa30`（#93）——均为 dev 独有演进，port 侧不反向吸收 |
| N8  | port 侧 `module.test` 的 mixin 与 wheel 测试（`ChestBlockMixin`、`wheel/` 包）                                   | dev 侧 module.test 已有 26.1 对应结构（`all/` 包、独立 wheel 相关演进），port 侧这些文件是旧平台形态；#7 只迁 multiblock 测试组                                                                                                    |
| N9  | `module.util` 其余 port 提交（`9d61997`/`c8ee900`/`0efcd3d`/`95bd5ca`）                                          | dev 侧已有等价或超集实现（分维度/玩家名工具、WeightedChanceBlockStates、VoxelShape 裁剪、UtilityClass 注解化等，`7019b38` 移植后继续演进）                                                                                         |
| N10 | port 侧 `module.recipe` 其余内容                                                                                 | dev 侧为演进主线（`2033fd9` 资源处理器缓存、`0d0649f` 配方代码优化等）；仅 #2/#3 两个 bugfix 为 port 独有                                                                                                                          |

> 执行纪律：出现「N 项之外是否遗漏」的疑问时，回到 `git log --oneline dev/26.1..port/1.21.1` 与
> `git diff port/1.21.1 dev/26.1 -- <模块>` 复核，确认有 port 独有内容再立项，不得凭印象扩大范围。

---

## 6. 建议执行顺序

> 总原则： **先大后小、先依赖后使用、独立项先验证**。每步完成即验收（第 3 章对应小节），全部通过再进入下一步；每步在 dev/26.1
> 分支以独立提交落地（提交信息建议按 dev 侧现有风格：`feat(yukkuri): ...` / `fix(recipe): ...` 等，具体按仓库约定）。

### 第 1 步：yukkuri（#1）——最大项与最大风险项

先做 #1 的原因：唯一整模块回迁，涉及流体 API 适配（26.1 最大不确定点）与四处工程联动；尽早暴露风险，后续步骤都在已稳定的聚合构建上叠加。

```bash
# 验证命令（在 dev/26.1 分支）
gradlew :module.yukkuri:build          # 模块编译 + 打包（验收见 3.1.5-1/2）
gradlew :module.main:build             # 聚合验证（验收见 3.1.5-3）
node .github/workflows/generate-matrix.js   # 本地核对 CI 矩阵含 yukkuri（需 GITHUB_WORKSPACE/GITHUB_OUTPUT 环境，见 3.1.3-E）
git diff port/1.21.1 dev/26.1 -- module.yukkuri   # 只读复核：只剩预期差异
```

### 第 2 步：recipe（#2 → #3）——纯模块内 bugfix

两个 bugfix 同模块同主题（鱼缸加工），一起做；#2 无 API 风险先落，再落 #3（涉及资源处理器 API）。

```bash
git diff port/1.21.1 dev/26.1 -- module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java   # #2 落位后核对
gradlew :module.recipe:compileJava     # #2 验收（3.2.3-1）
gradlew :module.recipe:compileJava     # #3 验收（3.3.3-1）；行为用例见 3.3.3-2/3
```

### 第 3 步：util（#4 → #5）——小修复 + API 补充

#4 独立小改先落；#5 与 #6 联动，本步只做代码与编译验证， **不**合并 #6 决策（见 3.6.3）。

```bash
gradlew :module.util:compileJava       # #4 / #5 验收（3.4.3-1、3.5.3-1）
gradlew :module.main:runClient         # #4 运行验收（3.4.3-2）
# #5 行为断言（3.5.3-2）可在 dev 侧 module.util 补一个测试方法或临时 main 验证后移除
```

### 第 4 步：multiblock（#6）——决策先行

先与 dev 侧维护者确认 3.6.3 的决策点：确认「迁」→ 落地 16 个重载（#5 生效）；确认「不迁」→ 回滚 #5 并跳过本步与 #7 的 BlockState
依赖部分。

```bash
gradlew :module.multiblock:compileJava   # #6 验收（3.6.4-1）
```

### 第 5 步：test（#7）——测试组落位

依赖 #6 的便捷重载（若 #6 放弃，本步定义改用 `block` 级重载构造，测试面收窄，需在验收中注明）。

```bash
gradlew :module.test:compileJava        # #7 验收（3.7.3-1）
gradlew :module.test:runData            # datagen 产物核对（3.7.3-2，需开 anvillib.needRunConfig.data=true）
gradlew :module.test:runServer          # 成形/解体行为验证（3.7.3-3）
```

### 第 6 步：main（#8）——收尾

独立两行，作为回迁批次最后一个提交。

```bash
gradlew :module.main:build              # 配置阶段验收（3.8.3-1）
```

### 顺序之外的两条纪律

1. 每步提交前先跑一遍该模块 `build`（含 `jar`），避免只验 `compileJava` 漏掉资源处理（mods.toml 替换、jarjar 内嵌）。
2. #1 与 #7 涉及 `runData` / 运行类验证，如本地环境不便跑客户端，可先以编译 + CI 为准，把运行验收登记为「待 CI/人工补验」并给出复现步骤，
   **不得**把未验证项标记为已完成（见第 7 章验收总表）。

---

## 7. 总体验收标准

### 7.1 构建验收（硬性门槛）

| #  | 验收项                        | 命令 / 判据                                                                                                                                                  | 覆盖回迁项 |
|----|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| B1 | dev/26.1 全量构建             | 在 dev/26.1 分支 `gradlew build`（根级，聚合全部模块含新 yukkuri）**全部成功**                                                                               | 全部       |
| B2 | 模块独立构建                  | 依次 `gradlew :module.yukkuri:build` `:module.recipe:build` `:module.util:build` `:module.multiblock:build` `:module.test:build` `:module.main:build` 均通过 | #1–#8      |
| B3 | 弃用告警检查（仅 #1 选 B 时） | `gradlew :module.yukkuri:compileJava` 日志中 `IFluidHandler` / `FluidStack` 相关 deprecation 警告为 0                                                        | #1         |
| B4 | 聚合内嵌                      | `module.main` 产物 jarjar 目录含 `anvillib-yukkuri-neoforge-26.1-*.jar`                                                                                      | #1         |
| B5 | CI 通过                       | 回迁 PR 的 GitHub Actions 全绿：`build-l0`（含 yukkuri）、`build-l1/l2`、`main`（含 gametest）                                                               | #1、全部   |

### 7.2 功能验收清单（行为核对）

| #  | 验收项                           | 判据                                                                                                                                                                                                                                                                                                                                                    | 执行方式                                       |
|----|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| F1 | 油库 API 行为（#1 特有验收）     | `VaporizationManager.tick(ServerLevel, VaporizationCauldron)` 在服务器 tick 正常驱动汽化流程：源发现（按 priority 降序）→ 顶层流体抽取 → `IVaporConsumer.receiveVapor`（SIMULATE 不改变状态；执行态按模拟接受量入账）→ 出口密封回压（`isOutletBlocked` / `sealsOutlet`）路径正确；`VAPOR_CONSUMER` 以 `Direction.DOWN` 在出口方块可查到；全程无异常日志 | dev/26.1 运行环境 + 模拟消费者；细节见 3.1.5-4 |
| F2 | 鱼缸加工（#2）                   | 失败匹配后谓词栈回滚至初始尺寸，后续配方可正常匹配；成功路径仅清理本次新增谓词；`context.getStack()` 无残留                                                                                                                                                                                                                                             | module.test 回归用例（3.2.3-3）                |
| F3 | 缓存同步（#3）                   | 完全一致场景零写入；同类型差量仅 extract/insert 差额；不同类型全量替换；空栈场景与 port 行为一致                                                                                                                                                                                                                                                        | 计数包装的 ResourceHandler 测试（3.3.3-2/3）   |
| F4 | ClientTickRecorder（#4）         | 主菜单 tick 不增长；进世界累计；退出世界归零                                                                                                                                                                                                                                                                                                            | runClient 手工验证（3.4.3-2）                  |
| F5 | BlockStatePredicate（#5）        | `with(BlockState)` 属性级精确匹配（含 StringRepresentable / 整型 / 布尔属性）                                                                                                                                                                                                                                                                           | 行为断言（3.5.3-2）                            |
| F6 | MultiblockDefinition（#6，若迁） | 16 个重载编译可用；`BlockState` 入参定义与 `BlockStatePredicate` 匹配一致                                                                                                                                                                                                                                                                               | 编译 + #7 测试覆盖（3.6.4）                    |
| F7 | 动态多方块测试组（#7）           | SIMPLE（熔炉）/ COMPLICATED（TestControllerBlock）/ WAHT（白床分层）三个定义 datagen 产物正确；进世界成形触发 `onFormed`、解体触发 `onUnformed`（铁锭/铁栏杆弹出）；定义 JSON 与 port 语义一致                                                                                                                                                          | runData + runServer（3.7.3-2/3）               |
| F8 | CurseForge 环境（#8）            | 发布后 CurseForge 元数据含客户端/服务端必需声明                                                                                                                                                                                                                                                                                                         | 发布 dry-run + 一次真实发布核对（3.8.3-2）     |

### 7.3 收尾核对

1. **提交完整性**：dev/26.1 分支上 #1–#8 各为独立提交（#5/#6 可合提交，取决于维护者偏好），提交信息含对应 issue
   号（#88/#89/#83/#84/#90 等）。
2. **范围守卫**：`git log --oneline dev/26.1..port/1.21.1` 中，除 N 表（第 5 章）与 #1–#8 外，不应有「实际回迁但未立项」的内容；如执行中发现新
   port 独有内容，先补充本计划再执行。
3. **文档同步**：`docs/branch-comparison.md` 的「待验证事项」（V1–V5）在回迁执行中逐项销账；本计划中的「待验证」标注同样逐一关闭，关闭记录可追加到附录
   B。
4. **遗留登记**：任何因环境限制未完成的运行类验收（F1/F4/F7 的运行部分）登记为遗留项，附复现步骤，不得默认通过。

---

## 附录

### A. 常用只读 git 命令（本计划全部事实均来自以下命令）

```bash
# 提取 port 侧文件内容
git show port/1.21.1:module.yukkuri/src/main/java/dev/anvilcraft/lib/v2/yukkuri/Yukkuri.java

# 查看来源提交的完整 diff
git show 569e01d              # #1（yukkuri 引入）
git show 18b29ca              # #1（CI 发布顺序，port 侧形态）
git show fab551b              # #1（yukkuri 收尾修复）
git show edd71ec              # #2
git show e78d020              # #3
git show 9874db7              # #4
git show 5fc6f65              # #5 / #6（动态多方块系统 + BlockStatePredicate 引入）
git show 1311782              # #8

# 两侧同路径对比
git diff port/1.21.1 dev/26.1 -- module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java

# 某模块 port 独有提交清单（回迁候选扫描）
git log --oneline dev/26.1..port/1.21.1 -- module.recipe

# dev 侧落点现状
git show dev/26.1:module.util/src/main/java/dev/anvilcraft/lib/v2/util/ClientTickRecorder.java

# 临时工作树对比（不切换当前分支）
#   git worktree add <临时目录> dev/26.1    # 注意：此为 git 写入操作，仅当执行者确认可创建 worktree 时使用；
#   git -C <临时目录> diff port/1.21.1 dev/26.1 -- <路径>
```

> 说明：`git worktree add` 属于工作区变更（非 commit/push），是否允许由执行者按仓库纪律判断；纯只读场景直接用 `git show` /
> `git diff` 两分支参数即可。

### B. 待验证事项汇总（执行时逐项销账）

| 编号 | 事项                                                                                                        | 涉及项 | 验证方法                                                                                               | 状态   |
|------|-------------------------------------------------------------------------------------------------------------|--------|--------------------------------------------------------------------------------------------------------|--------|
| V-Y1 | `Level.getCapability(BlockCapability, BlockPos, Direction)` 在 26.1 的签名与 side 传参顺序                  | #1     | `gradlew :module.yukkuri:compileJava` 编译定位 + IDE 反编译 `net.minecraft.world.level.Level` 确认语义 | 未验证 |
| V-Y2 | 选项 B 迁移完整性：14 个 Java 文件 `FluidStack` / `IFluidHandler` 零残留                                    | #1     | `grep -rn "FluidStack\|IFluidHandler" module.yukkuri/src` 无匹配 + 编译零告警                          | 未验证 |
| V-Y3 | yukkuri 与 AnvilCraft 大锅实现的流体对接契约（选 B 后 `FluidResource` 类型约定）                            | #1     | dev 侧仓库外与 AnvilCraft 维护者确认调用面                                                             | 未验证 |
| V-Y4 | `modules.json` 的 `needs` 是否需补充（当前结论 `[]`）                                                       | #1     | yukkuri 编译通过后按实际 import 复核                                                                   | 未验证 |
| V-Y5 | mods.toml 删 `[[mixins]]` 块后模组加载正常                                                                  | #1     | `gradlew :module.main:runServer` 启动日志无 mixin 报错                                                 | 未验证 |
| V-R1 | `InWorldRecipeContext.pop(...)` 在 dev 侧存在（或等价栈操作 API）                                           | #2     | dev 侧 `InWorldRecipeContext` 源码 + 编译                                                              | 未验证 |
| V-R2 | `ItemStack.isSameItemSameComponents` / `copyWithCount` 在 26.1 可用性                                       | #3     | `gradlew :module.recipe:compileJava`                                                                   | 未验证 |
| V-U1 | `ClientPlayerNetworkEvent.LoggingOut` 在 26.1 客户端事件体系可用                                            | #4     | `gradlew :module.util:compileJava` + runClient 验证                                                    | 未验证 |
| V-U2 | `with(Property<T>, T)` 泛型重载下 `with(BlockState)` 通配符调用的编译通过性                                 | #5     | `gradlew :module.util:compileJava`；遇错参照 port 侧泛型强转模式                                       | 未验证 |
| V-U3 | dev 侧 `Util` 是否保留 `cast`（新实现不依赖，仅记录）                                                       | #5     | `git grep "class Util" dev/26.1 -- module.util`                                                        | 未验证 |
| V-T1 | dev 侧 registrum `DataProviderInitializer` 对 datapack registry（`DEFINITIONS_KEY`）的 add 方法签名         | #7     | dev 侧 `AbstractRegistrum.getDataGenInitializer` / `RegistrumDatapackProvider` 源码 + 编译             | 未验证 |
| V-T2 | dev 侧 `BlockEntry` builder 链式 API 形态（`.blockstate()` / `.item()` / `.model()` / `.build()` 是否保留） | #7     | dev 侧 `module.registrum` `BlockEntry` 源码 + 编译                                                     | 未验证 |
| V-T3 | dev 侧 `MultiblockState` / `IController` 事件化后（`1e5bfb3`）回调体适配                                    | #7     | dev 侧 multiblock 源码 + `runServer` 行为验证                                                          | 未验证 |
| V-M1 | mod-publish 1.1.0 的 `curseforge.clientRequired/serverRequired` 属性可用性                                  | #8     | `gradlew :module.main:build` 配置阶段                                                                  | 未验证 |
| V-M2 | 真实发布后 CurseForge 元数据含环境声明                                                                      | #8     | 一次真实发布核对                                                                                       | 未验证 |

### C. 参考资料

| 资料                                                    | 位置                                                                                                                                                                                                                             |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 分支对比报告（编号体系、分类依据、待验证 V1–V5）        | `docs/branch-comparison.md`                                                                                                                                                                                                      |
| 8 个并行子代理模块级调研报告                            | `C:/Users/Administrator/.kimi-code/sessions/wd_anvillib_031cb785b7e3/session_6033f19b-f4ef-481a-b18c-d626cc22c314/agents/main/tool-results/AgentSwarm-call_00_nh47UIFAaqo9d96levr46332-594fb004-9f1f-4c9e-b230-43bc1440211d.txt` |
| NeoForge 26.1.2.76 sources jar（流体能力 API 核实依据） | `C:/Users/Administrator/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/1d1dabe31afb953e46fb278e1cd8f04ab8e0d9a0/neoforge-26.1.2.76-sources.jar`                                                             |
| dev 侧关键文件实测基准                                  | `settings.gradle`、`module.gradle`、根 `build.gradle`、`.github/modules.json`、`.github/workflows/{ci,build_and_test,generate-matrix}.js/yml`（均以 `0b7e45f` 为准）                                                             |
| port 侧来源提交基准                                     | `569e01d` / `18b29ca` / `fab551b` / `edd71ec` / `e78d020` / `9874db7` / `5fc6f65` / `1311782`                                                                                                                                    |

### D. 回迁提交清单（PR 提交模板）

> 每个回迁项在 dev/26.1 分支上对应一个独立提交（#5/#6 可合并为一个提交）。下表可直接作为 PR 描述模板，勾选「状态」列后随 PR
> 提交。

| 提交     | 回迁项                | 涉及文件（变更面）                                                                                                                     | 关键验证命令                                                                                                              | 状态 |
|----------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|------|
| commit 1 | #1 yukkuri            | `module.yukkuri/**`（18 文件）+ `settings.gradle` + `module.main/build.gradle` + `.github/modules.json` + `README.md` / `README.en.md` | `gradlew :module.yukkuri:build`；`gradlew :module.main:build`；`grep -rn "ResourceLocation" module.yukkuri/src`（0 匹配） | ☐   |
| commit 2 | #2 谓词栈回滚         | `module.recipe/.../InWorldRecipe.java`                                                                                                 | `gradlew :module.recipe:compileJava`                                                                                      | ☐   |
| commit 3 | #3 sync 短路/差量     | `module.recipe/.../cache/item/ItemResourceHandlerCacheElement.java`                                                                    | `gradlew :module.recipe:compileJava` + 写入计数测试                                                                       | ☐   |
| commit 4 | #4 ClientTickRecorder | `module.util/.../util/ClientTickRecorder.java`                                                                                         | `gradlew :module.util:compileJava` + runClient 手工验证                                                                   | ☐   |
| commit 5 | #5 with(BlockState)   | `module.util/.../predicate/BlockStatePredicate.java`                                                                                   | `gradlew :module.util:compileJava` + 行为断言                                                                             | ☐   |
| commit 6 | #6 16 重载（若迁）    | `module.multiblock/.../dynamic/definition/MultiblockDefinition.java`                                                                   | `gradlew :module.multiblock:compileJava`                                                                                  | ☐   |
| commit 7 | #7 测试组             | `module.test/.../test/multiblock/**`（6 文件）+ `AnvilLibTest.java` + `module.test/gradle.properties`                                  | `gradlew :module.test:compileJava`；`runData`；`runServer`                                                                | ☐   |
| commit 8 | #8 CF 环境两行        | `module.main/build.gradle`（publishMods 块）                                                                                           | `gradlew :module.main:build`                                                                                              | ☐   |
| 收尾     | 全量回归              | 全仓                                                                                                                                   | `gradlew build`（根级）全绿；CI 全绿；附录 B 待验证清单销账                                                               | ☐   |
