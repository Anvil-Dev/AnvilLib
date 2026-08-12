# AnvilLib 回移植计划：`dev/26.1` → `port/1.21.1`（功能回移植执行方案）

> ⚠️ 状态更新（2026-08-12）：#P7（`module.rendering`）与 #P8（`renderdoc-loader`）已从 `port/1.21.1` 分支整体移除；#P6（`module.font`）与 `module.main` 的渲染 jarJar 依赖、CI 工作流（ci / release / pull_request）中的渲染任务亦已同步清理。本文为历史计划文档，相关章节仅作记录保留。

> 文档信息
>
> - 生成日期：2026-08-10
> - 分析对象：`dev/26.1` @ `0b7e45f`（Minecraft 26.1.2 / NeoForge 26.1.2.76）、`port/1.21.1` @ `bd25229`（Minecraft 1.21.1 / NeoForge 21.1.226 / Java 21）
> - 共同祖先（merge-base）：`81c5a9c`
> - 数据来源：8 个并行子代理模块级调研报告（2026-08-10，1395 行，路径见附录 C）+ git 只读命令（`git show` / `git diff` / `git ls-tree`，2026-08-10 本地仓库核对）+ NeoForge 21.1.226 / fancymodloader loader 4.0.42/4.0.43 sources jar 解压核实 + 1.21.1 混淆 jar javap 实测
> - 关联文档与编号体系：本文与 `docs/migration-plan-dev-26.1.md`（port→dev 方向）**互为反向、互相补充**，两分支整体关系见 `docs/branch-comparison.md`；本文使用独立的 canonical 编号 **#P1–#P19**（「回移植项」），与 `docs/migration-plan-dev-26.1.md` 的 #1–#8 无任何对应关系，正文提及其它文档一律用文件名，避免编号混淆
> - 结论性质：除标注「待验证」处外，均基于调研报告与本地 git / sources jar / javap 实测得出；所有「待验证」条目均已给出验证方法，执行时必须完成验证后方可标记该项完成
> - 执行约束：本计划只描述 dev 侧「功能 / 修复 / 演进」的回移植，**不迁移**「26.1 平台适配」本身（`Identifier`、`ItemStackTemplate`、`RecipeMap`、`SavedDataType`、`ValueInput`/`ValueOutput`、`RenderPipeline`、`ResourceHandler`、`DataComponentPredicate`、`neoforgespi.transformation`、`@NullMarked` 等为 26.1 平台特性，port 侧已有 1.21.1 等价物，反向迁移无意义）；本文仅为计划，执行时需在 port/1.21.1 分支工作树上以受控提交落地，本文档本身不产生任何 git 写入操作

---

## 1. 概述与范围

### 1.1 回移植背景

AnvilLib 的两个长期分支在 merge-base `81c5a9c` 之后完全分叉：`dev/26.1` 是演进主线（沿 1.21.2 → 1.21.11 → 26.1 版本链升级，独有 170 提交），沿路新增了 rpc / sync / explosion / collision / space-select / font / rendering / renderdoc-loader 八个模块，并对既有共享模块（config / integration / network / util / registrum / multiblock / recipe / wheel）做了大量功能演进；`port/1.21.1` 是维护与特性分支（独有 46 提交），冻结在 Minecraft 1.21.1 / NeoForge 21.1.226 / Java 21 平台。

`docs/migration-plan-dev-26.1.md` 已把 port 侧独有的少量内容（yukkuri 模块、三个 bugfix、测试组、发布配置，共 #1–#8）回迁到 dev/26.1。与之相反，**dev 侧有大量「功能」是 port 侧完全没有的**——尤其是八个新模块，以及共享模块中的功能性演进（注册表 builder 扩展、配置分组、网络工具方法等）。这些功能大多建立在与平台无关的逻辑之上，或可经「26.1 → 1.21.1」反向 API 替换后落地。

本文档即这 19 项（canonical 编号 #P1–#P19）的执行计划：每项给出**目标、步骤（具体到文件与要点）、API 适配点（26.1 → 1.21.1 反向替换清单）、验收方式**，并汇总不可移植清单（第 5 章）、建议执行顺序（第 6 章）与总体验收标准（第 7 章）。

### 1.2 范围界定

本计划 **包含**：

- 19 个回移植项（#P1–#P19）的完整执行方案与验收判据；
- 每项与 dev 侧来源提交、port/1.21.1 落点文件的一一对应关系；
- 每项涉及的 26.1 → 1.21.1 反向 API 替换清单（全部以调研报告的 1.21.1 API 核实结论为准）。

本计划 **不包含**（即「不做清单」的完整理由见第 5 章）：

- 任何「26.1 平台适配」的反向搬运：`Identifier`、`ItemStackTemplate`、`RecipeMap`、`SavedDataType`、`ValueInput`/`ValueOutput`、`RenderPipeline`/`RenderState`/`SubmitNodeStorage`、`ResourceHandler`、`DataComponentPredicate`、`neoforgespi.transformation` SPI、jspecify `@NullMarked` 等，port 侧均有 1.21.1 等价物，反向迁移无意义；
- `module.moveable-entity-block` 的全部内容（dev 侧零功能增量，见 2.4 节）；
- dev 侧构建体系重构中的 26.1 特有内容（`clientRenderDoc`/`clientIrisWorkaround` run 配置、`createModule` 模板中的 `Identifier`、`java_version=25` 等，见 #P19）；
- module.test 中依赖 dev 新渲染模块的集成测试（Bloom / CachedBER / Compute / SDF 图层 / GUI 测试，见第 5 章）。

**迁移方式**：两侧无共同提交，直接 merge 方向错误且冲突不可控，故采用 **受控回移植**——在 port/1.21.1 分支工作树上按 #P1–#P19 逐个以独立提交落地，每个提交只改本项内容，便于评审与回滚。

### 1.3 前置条件与约定

| 项       | 约定                                                                                                                                                                                                                     |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 编号     | 回移植项一律使用 canonical 编号 **#P1–#P19**；引用其它文档时使用文件名（如 `docs/migration-plan-dev-26.1.md`），不得沿用其 #1–#8 编号                                                  |
| 基线     | port/1.21.1 @ `bd25229`（文档描述的全部落点均以该提交为准；执行时如 HEAD 已前进，需先以 `git log` 复核落点文件未变）                                                                       |
| 提取方式 | 一律用 `git show dev/26.1:<路径>` 提取源码，**不**进行分支切换、不产生 git 写入操作                                                                                                                                       |
| 工作区   | 回移植在 port/1.21.1 分支工作树执行；只读对比可用 `git diff port/1.21.1 dev/26.1 -- <路径>`                                                                                                                               |
| 验证基线 | port 侧构建命令：`gradlew :module.xxx:build`、`gradlew :module.xxx:compileJava`、`gradlew :module.xxx:runData`（模块需要 datagen 时）；port 侧工具链为 **Java 21**（`module.network/build.gradle:33` 确认），dev 侧代码中 Java 22+ 语法（如 lambda 参数 `_`、`field.accessFlags()`）必须改写 |
| API 事实 | 本计划全部 1.21.1 API 存在性结论来自调研报告（NeoForge 21.1.226 sources jar 解压、fancymodloader loader 4.0.42/4.0.43 sources、1.21.1 混淆 jar javap、port 分支现有代码引用），执行时以编译期核对为最终判据 |
| 待验证   | 所有标注「待验证」的条目均已给出验证方法（附录 B），执行时须完成验证后才能标记该项完成                                                                                                                                   |

### 1.4 回移植项一览（结论先行）

| #P   | 内容                                                                                          | 难度       | 一句话结论                                                                                      |
|------|-----------------------------------------------------------------------------------------------|------------|-------------------------------------------------------------------------------------------------|
| #P1  | `module.rpc` 整模块（注解式远程调用，25 文件）                                                | 低         | 整模块拷贝 + 3 处机械 API 反替换（Identifier / FMLLoader / accessFlags）                        |
| #P2  | `module.sync` + processor（同步框架，31 文件）                                                | 中-高      | 约 50/56 文件直接搬；processor 的 26.1 SPI 需 CoreMod（ICoreMod + ITransformer<ClassNode>）重写 |
| #P3  | `module.explosion` 整模块（10 文件）                                                          | 低         | 6 处 26.1 API 反替换，其余逐字可搬；依赖 port module.config                                     |
| #P4  | `module.collision` 整模块（纯数学，3 文件）                                                   | 极低       | 零 MC/NeoForge API 依赖，逐字搬移                                                               |
| #P5  | `module.space-select` 整模块（18 文件）                                                       | 中         | 唯一实现重写点是 DistrictRenderer 线框路径（ShapeRenderer → LevelRenderer.renderVoxelShape）    |
| #P6  | `module.font` 整模块（25 文件）                                                               | 中         | 约 60% 纯 CPU 逻辑直接搬；GPU 上传/绘制层与 GUI 按 1.21.1 改写；依赖 #P7                        |
| #P7  | `module.rendering`（113 文件，按功能块分 5 批）                                               | 高（分批） | 纯逻辑 19 项直接搬；后处理 / SDF GUI / CachedBER / Compute 四块执行层按 1.21.1 重写             |
| #P8  | renderdoc-loader（Java Agent，2 文件）                                                        | 低         | 源码逐字搬；构建脚本按 port 侧（Java 21、无 jreleaser 别名）改写                                |
| #P9  | `module.config`：group 分组 + TranslatableEnum + TOML key 点分隔                              | 低         | 三处纯逻辑/配置演进，1.21.1 加载链路原生支持子目录文件名                                        |
| #P10 | `module.integration`：数据加载客户端/服务端拆分 + meter 递增                                  | 低         | 纯反射逻辑，可移植；破坏性 API（applyData 改名）需提示下游                                      |
| #P11 | `module.network`：PacketData 泛型检查修复 + NetworkUtil Included 方法 + PacketProtocol public | 低         | 三处纯逻辑/修饰符改动，无平台依赖                                                               |
| #P12 | `module.util`：OutlineUtil + withCount + @EqualsAndHashCode                                   | 中         | 四处功能演进；                                                                                  |
| #P13 | `module.registrum`：14 种新注册表 builder/entry                                               | 低         | 绝大多数仅 Identifier 改名或个别签名微调；VillagerProfession 需按 1.21.1 6 元 record 适配       |
| #P14 | `module.registrum` datagen 增强（RecipeProvider public 化 + dataMap(provider)）               | 中         | 方法 public 化改「公开包装内调静态方法」；Runner / 模型生成器不迁                               |
| #P15 | `module.multiblock`：DynamicMultiblockEvent + 快照复用 + BlockPos 键化 + 懒解析               | 中         | 四项修复/演进均可移植；Config.group 依赖 #P9                                                    |
| #P16 | `module.recipe`：SpawnItem 零数量守卫 + SetBlock nbt 默认值                                   | 低         | 两处纯逻辑修复（含一个潜在 NPE），与 dev 侧其它改动无关                                         |
| #P17 | `module.wheel`：环形扇区选择效果（WheelSelectionEffect + fsh + 渲染适配）                     | 中-高      | 纯 UI 功能可移植；渲染必须落在 port 旧 Tesselator/ShaderInstance 体系，切断 dev 渲染依赖        |
| #P18 | `module.test`：可选集成测试（T2 / T8 / T9 / T10）                                             | 低-中      | 仅带回不依赖 dev 新渲染模块的测试；随 #P1/#P3/#P9/#P17 落地                                     |
| #P19 | 构建体系：roseauCheck API 兼容检查（+ module.gradle 集中化可选）                              | 中         | roseau-cli 无 MC 绑定直接搬；集中化需裁剪 26.1 特有内容                                         |

---

## 2. 判定框架与分类规则

### 2.1 三类分类定义

调研报告（附录 C）对 dev 侧每个功能点给出了三分类判定。本计划沿用该分类，规则如下：

**【可直接移植】**：功能逻辑完全不依赖 26.1 专有 API，或仅依赖两侧签名一致的稳定 API（如 `Mth`、JOML、`EventBus` 体系、`PacketDistributor`、codec/stream codec 基础件），可原样拷贝或仅做机械改名（`Identifier`→`ResourceLocation` 等）。判定标准：全部引用的类/方法在 1.21.1 sources / javap 中逐项核实存在，且无行为差异。

**【需反向适配】**：功能本质与平台无关（算法、数据结构、业务流程），但实现载体使用 26.1 平台 API；1.21.1 有直接等价物（`RegisterShadersEvent + ShaderInstance` 对应 `RegisterRenderPipelinesEvent + RenderPipeline`、`LevelRenderer.renderVoxelShape` 对应 `ShapeRenderer`、`GatherDataEvent` 对应 `GatherDataEvent.Client` 等）。移植 = 保留功能逻辑 + 按等价物重写执行层。判定标准：1.21.1 等价物存在且功能语义可完整承接；若等价物需整体重写（如 GPU 抽象层），难度上调。

**【不可移植】**（仅 26.1 平台）：功能本身就是 26.1 平台适配（`Identifier` 改名、`@NullMarked` 风格、`neoforgespi.transformation` SPI、`RenderPipeline` 注册、`ItemStackTemplate` 编解码分支、`SavedDataType` 持久化、`ValueInput`/`ValueOutput` 序列化等），或依赖的 26.1 API 在 1.21.1 无对应物（如 `GpuDevice`/`CommandEncoder` 抽象、`PictureInPictureRenderer` 框架）。**反向迁移无意义**：port 侧已有 1.21.1 等价实现，且迁移会破坏 port 侧现状。第 5 章按模块给出完整清单。

> 三类关系的简化表述：**直接移植**搬「功能 + 平台无关逻辑」；**反向适配**搬「功能」但重写「平台载体」；**不可移植**的「功能」本身就是平台，不搬。

### 2.2 判定时的参考判据（源自报告）

- **功能面判定**：一个 dev 侧差异是否构成「功能」，看其是否改变模块对外行为/能力（新增 API、修复 bug、新增注册表入口、性能演进）；纯风格、注解风格（`@NullMarked` vs `@MethodsReturnNonnullByDefault`）、反混淆名调整、构建重构均不构成功能。
- **平台面判定**：引用 26.1 专属类型/机制（`Identifier`、`ItemStackTemplate`、`RecipeMap`、`SavedDataType`、`ValueInput`/`ValueOutput`、`RenderPipeline`/`SubmitNodeStorage`/`GpuDevice`、`GuiGraphicsExtractor`、`neoforgespi.transformation.*`、`DataComponentPredicate`、`ResourceHandler`、jspecify `@NullMarked`、`GatherDataEvent.Client/Server`、`AddClientReloadListenersEvent`、`RegisterRenderPipelinesEvent` 等，完整清单见 1.2 与第 5 章）的任何代码，其「平台部分」一律不可迁移。
- **困难等级**：低 = 机械改名/拷贝，1 个提交可完成；中 = 涉及执行层重写或跨模块协调；高 = 需按 1.21.1 生态整体重写（后处理、CachedBER、Compute、SPI 适配层）。
- **已由 port 侧平行 PR 接收的内容不重复移植**：如 multiblock 双半方块修复、`AbstractCacheElement.grow()` 空堆修复、`ICacheInputOutputImpl.grow()` 只记录实际消耗、`ItemCache` 元素范围 `0.5→1.0`（port `6a0e8a9` #79 / `f2b60b6`，对应 dev `922d523` #78 / `1c523ee`），以及 wheel 与活塞模块的基础移植（`18834b5`/`53eaa11`/`9c437b1` 等）——均已核实存在，**不立项**。

### 2.3 为什么 moveable-entity-block 被整体排除

`module.moveable-entity-block` 在 dev 侧的差异经逐文件核实**全部为 26.1 平台适配，零功能增量**：

| dev 侧差异 | 性质 |
|---|---|
| `PistonMovingBlockEntityMixin`：`ValueInput`/`ValueOutput` 序列化重构（`b5b27aa`） | 26.1 NBT 序列化体系；port 侧 `loadAdditional(CompoundTag, HolderLookup)`/`saveAdditional` 等价 |
| `PistonHeadRendererMixin` + 新增 `PistonHeadRenderStateMixin` + `IPistonHeadRenderStateExtension`（`83477ab` 后续 26.1 适配） | 注入点改 `extractRenderState`/`submit`，依赖 `BlockEntityRenderState`/`SubmitNodeCollector`（26.1 渲染管线）；port 侧注入 `render(...MultiBufferSource)` 功能等价 |
| `PistonBaseBlockMixin` 局部变量名/反混淆名调整 | 26.1 反混淆名（`blockpos3→pos`、`blockstate→newState`），逻辑零变化 |
| 活塞 bug 修复（`922d523`） | port 侧 `6a0e8a9`（#79）已含同一修复，无需迁移 |
| `@ApiStatus.Internal` / `@NullMarked` 注解 | 纯注解风格，非功能 |

**结论**：该模块在 port/1.21.1 侧的现有实现（`PistonHeadRendererMixin` 注入 `render` + 原版 NBT 序列化）即 dev 侧功能的 1.21.1 等价物，反向迁移只会破坏 port 侧现状。整体排除，不立项。

### 2.4 平台适配「不迁移」的通用原则

除第 5 章清单外，以下 **机械性平台适配** 在任何一个回移植项中出现时均按「保留 port 侧写法」处理，不作为独立改动：

- `Identifier` → 保持 port 侧 `ResourceLocation`（`ResourceLocation.fromNamespaceAndPath` 与 26.1 `Identifier.fromNamespaceAndPath` 同名同参）；
- `FMLLoader.getCurrent().getLoadingModList()` / `getCurrent().getDist()` → 保持 port 侧 `LoadingModList.get()` / `FMLLoader.getDist()`（1.21.1 的 `FMLLoader` **无 `getCurrent()`**，loader 4.0.42/4.0.43 源码核实）；
- `field.accessFlags()` + `AccessFlag`（Java 22 API）→ `Modifier.isPublic/isStatic/isFinal`（port 工具链 Java 21）；
- `isClientSide()` 方法调用 → port 侧 `isClientSide` 字段；`ResourceKey::identifier` → port 侧 `ResourceKey::location`；
- `getValue/getKey`（26.1 注册表改名）→ port 侧 `get/getKey`；
- `GatherDataEvent.Client/Server` → port 侧单一 `GatherDataEvent`（21.1.226 核实无子类，构造器 `(ModContainer, DataGenerator, DataGeneratorConfig, ExistingFileHelper)`）；`AddClientReloadListenersEvent` → port 侧 `RegisterClientReloadListenersEvent`；
- `@NullMarked`（jspecify）→ 保持 port 侧 `@MethodsReturnNonnullByDefault` + `javax.annotation.ParametersAreNonnullByDefault`；
- lambda 参数 `_`（Java 22+ 语法）→ 改名（如 `ignored`）。

---

## 3. 回移植项总表

> 内容、来源提交、难度、前置依赖与 `docs/migration-plan-dev-26.1.md` 的方向相反（本文为 dev→port）；「1.21.1 落点」列为本文新增。来源提交为**代表提交**（同项可能含多个提交，正文给出完整清单）。

| #P  | 内容                                                                                                                                                     | 来源提交（代表）                                                              | 难度   | 前置依赖                     | 1.21.1 落点                                                                             |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|--------|------------------------------|-----------------------------------------------------------------------------------------|
| #P1 | module.rpc 整模块（注解式远程调用，25 文件）                                                                                                              | `d153e58` #60、`8d57cba` #63、`4910620` #87                                      | 低     | module.network/util 在 port 侧已就绪 | 新模块 `module.rpc/`（坐标 `anvillib-rpc-neoforge-1.21.1`）                            |
| #P2 | module.sync + processor（同步框架，31 文件；processor 需 CoreMod 重写）                                                                                  | `3ce2611`、`bf49789`、`8d57cba`、`1063ee9` #65                                   | 中-高  | 同 #P1                       | 新模块 `module.sync/` + `module.sync/processor/`（坐标 `anvillib-sync-neoforge-1.21.1`）|
| #P3 | module.explosion 整模块（10 文件）                                                                                                                       | `435e23f`、`fba1027`                                                             | 低     | module.config                | 新模块 `module.explosion/`（坐标 `anvillib-explosion-neoforge-1.21.1`）                 |
| #P4 | module.collision 整模块（纯数学，3 文件）                                                                                                                | `82b4f6a`                                                                         | 极低   | 无                           | 新模块 `module.collision/`（坐标 `anvillib-collision-neoforge-1.21.1`）                 |
| #P5 | module.space-select 整模块（18 文件）                                                                                                                    | `1335fc5`、`e75b10f`                                                             | 中     | module.network               | 新模块 `module.space-select/`（坐标 `anvillib-space-select-neoforge-1.21.1`）            |
| #P6 | module.font 整模块（25 文件）                                                                                                                            | `1d56a4e` #22、`946938f` #56                                                      | 中     | 依赖 module.rendering 的 1.21.1 版（#P7） | 新模块 `module.font/`（坐标 `anvillib-font-neoforge-1.21.1`）                |
| #P7 | module.rendering（113 文件，不可整模块；按功能块分批：#P7a 纯逻辑 19 项 / #P7b 后处理三件套 / #P7c SDF GUI / #P7d CachedBER / #P7e Compute）              | `7121bf6`、`494274e` #21、`7d07f20` #41、`64bcf21` #33                            | 高（分批） | 无                           | 新模块 `module.rendering/`（坐标 `anvillib-rendering-neoforge-1.21.1`）                 |
| #P8 | renderdoc-loader（Java Agent，2 文件）                                                                                                                   | `2751be4`、`1063ee9`                                                             | 低     | 无                           | 新模块 `renderdoc-loader/`（坐标 `renderdoc-loader-internal`）                           |
| #P9 | module.config：group 分组 + TranslatableEnum 枚举翻译 + TOML key 点分隔                                                                                 | `3d50d76`、`946938f` #56                                                          | 低     | 无                           | `module.config` 现有文件原地增强（`Config`/`ConfigManager`/`ConfigRecord`/`ConfigData`/`FormattingUtil`/`util/TranslatableEnum.java` 新增） |
| #P10 | module.integration：数据加载客户端/服务端拆分 + meter 递增                                                                                              | `dc4d583`、`e8efbd4`                                                             | 低     | 无（破坏性 API）             | `module.integration` 三个文件（`IntegrationInstance`/`IntegrationManager`/`IntegrationType`） |
| #P11 | module.network：PacketData 泛型检查修复 + NetworkUtil Included 方法 + PacketProtocol public                                                            | `20721b1` #52、`5733a9d`、`6220427`                                               | 低     | 无                           | `module.network` 三个文件（`PacketData`/`NetworkUtil`/`PacketProtocol`）                 |
| #P12 | module.util：OutlineUtil + predicate HolderGetter 重构 + withCount + @EqualsAndHashCode                                                                 | `e4c09fb` #75、`bb09805`、`d84912e`、`c480e9f` #49                                 | 中     | module.recipe 调用方连带     | `module.util`（`OutlineUtil`/`client/Line`/测试 + 三个 predicate 文件）+ `module.recipe` 四个调用方 |
| #P13 | module.registrum：14 种新注册表 builder/entry（GameEvent/MobEffect/Potion/VillagerType/PoiType/Self/三类 Modifier/Condition/CreativeTab/Attachment/DataComponent/SoundEvent/RecipeSerializer/RecipeType/VillagerProfession） | `9b2d0e3` #31、`9c89bfa` #27、`9627358` #30、`c307a4c` #28、`252922e` #47          | 低     | 无                           | `module.registrum` 的 `builders/**`、`util/entry/**` 新增文件 + `AbstractRegistrum` 入口方法 |
| #P14 | module.registrum datagen 增强（RecipeProvider 方法 public 化、dataMap(provider) 重载；GeneratorType 拆分可选）                                           | `58f01d4`、`b4989c0`                                                             | 中     | #P13（可选）                 | `module.registrum` 的 `providers/RegistrumRecipeProvider.java`、`providers/RegistrumDataMapProvider.java`、`builders/Builder.java` |
| #P15 | module.multiblock：DynamicMultiblockEvent + 未加载区块快照复用 + Long→BlockPos 键化 + 定义懒解析                                                          | `1e5bfb3` #69、`d888714` #73、`22091d4`、`1b0a652`                                 | 中     | #P9（Config.group，M7 项）   | `module.multiblock` 现有文件（`dynamic/event/` 新建 + `DynamicMultiblockManager`/`MultiblockState`/`MultiblockCheckSnapshot`/两个 packet） |
| #P16 | module.recipe：SpawnItem 零数量守卫 + SetBlock nbt 默认值                                                                                               | `40b8f84`、`91327c1`、`b8825bd`                                                   | 低     | 无                           | `module.recipe` 的 `outcome/SpawnItem.java`、`outcome/SetBlock.java`                     |
| #P17 | module.wheel：环形扇区选择效果（WheelSelectionEffect + fsh + 渲染适配）                                                                                 | `5d0b285`、`46e1b73`、`8a818f0`                                                   | 中-高  | 保留 port 旧渲染体系         | `module.wheel` 现有文件（`api/WheelSelectionEffect.java` 新建 + `WheelMenuBuilder`/`WheelMenuModel`/`WheelWidget`/`WheelScreen` + `annular_sector.fsh`） |
| #P18 | module.test：可选集成测试（T2 配置测试 / T8 RPC / T9 爆炸 / T10 wheel 按键）                                                                            | `a6a24f1`、`d153e58`、`435e23f`                                                   | 低-中  | 随 #P1/#P3/#P9/#P17          | `module.test` 新增（`AnvilLibTestConfig.java`、`rpc/**`、`command/TestCommand.java`、`wheel/` 按键扩展） |
| #P19 | 构建体系：roseauCheck API 兼容检查（+ module.gradle 集中化可选）                                                                                        | `ea4301f` #25、`a6a24f1`、`946938f`                                               | 中     | 无                           | 根 `roseau.yaml` + `gradle/scripts/roseau.gradle`（可选：根 `module.gradle` + `build.gradle` 调整） |

### 3.1 编号一致性说明

- 本表 #P1–#P19 为本文唯一执行依据，与 `docs/migration-plan-dev-26.1.md` 的 #1–#8 无任何对应关系。
- #P7 拆为 #P7a–#P7e 五个子项分别执行与验收（第 4 章对应小节），但作为单一模块落地。
- #P6 依赖 #P7 的 1.21.1 版（build.gradle jarJar 依赖 `anvillib-rendering-neoforge-1.21.1`），但 #P6 自身的 SDF 图集/布局/测量等纯逻辑部分可在 #P7a 完成后先行落地，无需等 #P7b–#P7e。
- #P15 的 Config.group 子项（M7）依赖 #P9（`@Config(group=…)` 离开 module.config 的 group 支持无法编译）；若 #P9 未先行，#P15 中两个模块保持 `@Config(name=…)` 亦可编译，但 group 语义缺失；#P18 各测试项分别依赖 #P1/#P3/#P9/#P17，可随对应项落地，不单独立项执行顺序；#P19 的 roseauCheck 独立于全部代码项，module.gradle 集中化为可选工程决策（port 侧现有独立构建脚本自洽，是否重构由维护者决定）。

---

## 4. 逐项详细计划

> 每节统一包含四部分：**目标**（一句话）、**步骤**（具体到文件与要点）、**API 适配点**（26.1 → 1.21.1 反向替换清单）、**验收方式**。步骤中的 `git show dev/26.1:<路径>` 均为只读操作。所有 1.21.1 API 存在性结论均来自调研报告（核实方式：NeoForge 21.1.226 sources jar 解压、fancymodloader loader 4.0.42/4.0.43 sources、1.21.1 混淆 jar javap、port 分支现有代码引用）。

### 4.1 回移植项 #P1：module.rpc 整模块（低）

#### 4.1.1 目标

把 dev 侧注解式远程方法调用框架 `module.rpc`（25 文件）整体回移植到 port/1.21.1，作为新模块 `anvillib-rpc-neoforge-1.21.1`，实现「`@RemoteCallable` 静态方法 + 方法引用解析 + 索引下发 + 双向 play 网络包 + 带返回值 invoke」的完整 RPC 能力。

#### 4.1.2 步骤

```bash
# 1) 提取 dev 侧模块源码（25 个 Java 文件，整体拷贝）
for f in $(git ls-tree -r --name-only dev/26.1 -- module.rpc | grep '\.java$'); do
  git show "dev/26.1:$f" > "$f"
done

# 2) 构建文件：build.gradle 依赖改 anvillib-network-neoforge-1.21.1 /
#    anvillib-util-neoforge-1.21.1（codec 仅为 jarJar 传递依赖，同步改坐标）；
#    jarJar 分支按 port 侧现有模块模式（NOT_DEV 双分支写法，参照 port module.wheel）
# 3) settings.gradle 注册：include 'module.rpc' + project(':module.rpc').name = 'anvillib-rpc-neoforge-1.21.1'
# 4) 模块元数据：neoforge.mods.toml 按 port 侧模板（mod_id=anvillib_rpc）；
#    anvillib_rpc.mixins.json 保留（dev 侧为空清单，port 侧同样空）；gradle.properties 按 port 模块惯例
# 5) 按 4.1.3 的三处 API 反替换修改源码
```

#### 4.1.3 API 适配点（已逐项核实）

| dev 侧（26.1） | port 侧（1.21.1） | 落点 |
|---|---|---|
| `net.minecraft.resources.Identifier` | `net.minecraft.resources.ResourceLocation`（`ResourceLocation.fromNamespaceAndPath` 同名）；`IPacket.type(...)` 随之适配（port 侧签名 `type(ResourceLocation)`） | `AnvilLibRpc.of/mod`、`RpcRegistry` 等全模块机械替换 |
| `FMLLoader.getCurrent().getLoadingModList()` | `LoadingModList.get()`（1.21.1 的 `FMLLoader` 无 `getCurrent()`，loader 4.0.43 源码核实；port 侧 NetworkRegistrar 即此写法） | `RpcRegistry.getKeys()` |
| `field.accessFlags()` + `AccessFlag`（Java 22 API） | `Modifier.isPublic/isStatic/isFinal`（port 工具链 Java 21，`module.network/build.gradle:33` 确认） | `RpcMethods.findDeclaredCodec` |

**已核实的 1.21.1 兼容面**（无需改动）：`RegisterConfigurationTasksEvent.register/getListener`、`ICustomConfigurationTask`、`IPayloadContext.reply/enqueueWork/flow/player/finishCurrentTask`、`ModFileScanData.AnnotationData`（1.21.1 与 26.1 的 record 签名完全一致，`clazz()` 均返回 asm `Type`）、`PacketDistributor.sendToPlayer`、`ClientPacketDistributor.sendToServer`、`RegistryFriendlyByteBuf(ByteBuf, RegistryAccess, ConnectionType)`、`ByteBufCodecs.*` 全家桶、`UUIDUtil.STREAM_CODEC`、`ServerTickEvent.Post`/`ServerStoppedEvent`/`ClientTickEvent.Post`/`ClientPlayerNetworkEvent.LoggingOut`。`LambdaResolver`/`RpcMethods` 反射为纯 Java 标准 API；无 mixin 注入（`anvillib_rpc.mixins.json` 为空）。

#### 4.1.4 验收方式

1. `gradlew :module.rpc:compileJava` 通过（3 处反替换后零编译错误）。
2. 行为验证（随 #P18 T8 或手工）：客户端注册 `@RemoteCallable` 方法并下发索引 → 服务端经 `invoke` 远程调用返回 `CompletableFuture` 结果；`@CallableParam` 自定义编解码器生效；tick 超时路径正确。
3. `git diff port/1.21.1 dev/26.1 -- module.rpc` 只读复核：只剩「预期差异」（`ResourceLocation`、`LoadingModList.get()`、`Modifier`、构建脚本形态、坐标后缀）。

---

### 4.2 回移植项 #P2：module.sync + processor（中-高）

#### 4.2.1 目标

把 dev 侧同步框架 `module.sync`（31 文件 = 主模块 25 + processor 子模块 6）整体回移植到 port/1.21.1；其中 processor 的字节码注入能力从 26.1 专属的 `neoforgespi.transformation` SPI 改写为 1.21.1 的 **CoreMod（`ICoreMod` + `ITransformer<ClassNode>`）** 方案，注入器逻辑复用。

#### 4.2.2 步骤（按功能点子项）

**子项 2.1：注解 + SyncProxy + SyncManager + SyncRegisterEntry + 自定义注册表（可直接移植，低）**

```bash
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/annotation/Sync.java  # 及 annotation/* 全部
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/management/SyncProxy.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/management/SyncManager.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/management/SyncRegisterEntry.java
# init/* 同名落位；AnvilLibSync.of() 的 Identifier → ResourceLocation
```

- 已核实 1.21.1 存在：`NewRegistryEvent`、`RegistryBuilder.maxId(int).create()`（返回 `Registry<T>`）、`DeferredRegister.create(ResourceKey, modid)`、`DeferredHolder`、`Registry.entrySet()`、`RegisterEvent.getRegistryKey()`、`FMLCommonSetupEvent`、`Entity::level`/`getUUID`、`BlockEntity::getBlockPos/getLevel`。
- `SyncProxy.defaultCodec` 的 codec 表（`ByteBufCodecs.TAG/COMPOUND_TAG/VECTOR3F/QUATERNIONF/GAME_PROFILE/GAME_PROFILE_PROPERTIES`、`BlockPos.STREAM_CODEC`、`ComponentSerialization.STREAM_CODEC`、`ItemStack.OPTIONAL_STREAM_CODEC`、`ByteBufCodecs.fromCodecWithRegistries`）全部为 1.20.5+/1.21.1 标准 API（`fromCodecWithRegistries` port 侧 `StreamCodecUtil` 已用）。
- **删除项**：`ItemStackTemplate.STREAM_CODEC` 分支——`ItemStackTemplate` 是 26.1 ItemStack 重构专有类型，1.21.1 无对应物（javap「找不到类」），1.21.1 的 `ItemStack` 已有 `OPTIONAL_STREAM_CODEC`，删除后功能无损（见第 5 章）。

**子项 2.2：SyncConfigManager（需反向适配，中）**

```bash
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/management/SyncConfigManager.java
```

- `FMLLoader.getCurrent().getLoadingModList()` → `LoadingModList.get()`（`AnnotationData` 用法两侧兼容，直接可用）。
- `modFile.getContents()`（`IModFile.getContents()` 返回 `net.neoforged.fml.jarcontents.JarContents`，其 `containsFile/openFile` 为 26.1 新增——loader 11.0.13 源码确认）→ **1.21.1 的 `IModFile` 无 `getContents()`**（loader 4.0.43 `IModFile.java` 方法列表确认）。等价实现：`fileInfo.getFile().getSecureJar().getPath(classPath)`（`cpw.mods.jarhandling.SecureJar.getPath(String, String...)` 返回 `Path`，securejarhandler 3.0.8 sources 确认）+ `Files.newInputStream`，或 `getSecureJar().findFile(classPath)` 返回 `Optional<URI>`；注意 1.21.1 的 `JarContents`（cpw）仅 `findFile/getPackages`，无 `openFile`。

**子项 2.3：LazySyncManager + LazySync 注解（可直接移植，低）**

```bash
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/management/LazySyncManager.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/annotation/LazySync.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/util/SyncDirection.java
```

纯 Java（`java.lang.reflect` + Guava `MapMaker` 弱引用 + netty）+ `SyncProxy.defaultCodec` + `SideUtil.send`，无 26.1 专有 API。

**子项 2.4：网络 payload 层（可直接移植，低）**

```bash
git ls-tree -r --name-only dev/26.1 -- module.sync/src/main/java | grep -E 'network/' | while read f; do git show "dev/26.1:$f" > "$f"; done
```

`IInsensitiveBiPacket` 在 port/1.21.1 与 dev/26.1 **逐字节一致**（默认方法 `bidirectionalHandler` 相同，port 侧仅 javadoc 缩进差异）；`IPacket.type(ResourceLocation)`、`IClientboundPacket`/`IServerboundPacket`、`ctx.reply/finishCurrentTask`、`ByteBufCodecs.map(...)`、`StreamCodec.unit`、`PacketFlow` 全部 1.21.1 标准。

**子项 2.5：SideUtil + 入口类（可直接移植，低）**

```bash
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/util/SideUtil.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/AnvilLibSync.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/client/AnvilLibSyncClient.java
```

已核实存在：`PacketDistributor.sendToAllPlayers/sendToPlayersTrackingChunk(ServerLevel, ChunkPos, Packet)/sendToPlayersTrackingEntity/sendToPlayersInDimension`、`ChunkPos.containing`、`ServerLifecycleHooks.getCurrentServer()`、`ClientPacketDistributor.sendToServer`、`Minecraft.level/getConnection`、`ClientPacketListener.registryAccess()`、`ServerLevel.getEntity(UUID)`、`RegisterConfigurationTasksEvent.getListener()` 返回 `ServerConfigurationPacketListener`（21.1.226 sources 确认）。仅 `Identifier`→`ResourceLocation` 机械改名。

**子项 2.6：processor 子模块——CoreMod 重写（需反向适配，高）**

dev 侧 `processor/src/main/java/.../transform/` 五个类（`SyncClassProcessor`/`SyncTargetIndex`/`SyncBytecodeInjector`/`LazySyncBytecodeInjector`/`LazySyncTargetIndex`）+ `META-INF/services/net.neoforged.neoforgespi.transformation.ClassProcessor`。`SyncClassProcessor` 实现的 `net.neoforged.neoforgespi.transformation.ClassProcessor`（`name()/handlesClass(SelectionContext)/processClass(TransformationContext)` + `ProcessorName`、`ComputeFlags`）是 **26.1 全新 SPI**：loader 4.0.42/4.0.43（1.21.1）jar 与 sources 中 `neoforgespi` 包**无 transformation 子包**（逐 jar 扫描确认），neoforge 21.1.226 sources 中无任何 transformation 文件，loader 11.0.13（26.1 系）含 40 个 transformation 类——该 SPI 属 26.1 平台特性，必须重写适配层。

**方案 A（推荐）：CoreMod 重写**

```bash
# 1) SyncClassProcessor 骨架改写为 ICoreMod 实现：
#    net.neoforged.neoforgespi.coremod.ICoreMod（getTransformers() 返回 Iterable<? extends ITransformer<?>>，
#    loader 4.0.43 源码确认存在）
# 2) services 文件改为注册 net.neoforged.neoforgespi.coremod.ICoreMod
# 3) 新增两个 ITransformer<ClassNode>（modlauncher 11.0.5 源码确认，即 1.21.1 所用版本）：
#    - targets() 取自 SyncTargetIndex / LazySyncTargetIndex 的静态索引；
#    - transform(ClassNode, ITransformerVotingContext) 内调用
#      SyncBytecodeInjector / LazySyncBytecodeInjector.inject(ClassNode)；
#    - ClassWriter.COMPUTE_FRAMES 对应原 COMPUTE_FRAMES 返回语义
```

**方案 B（备选）**：modlauncher `ITransformationService`（services 注册 `cpw.mods.modlauncher.api.ITransformationService`），同样复用注入器。

**可复用部分（约 90%）**：`SyncBytecodeInjector`/`LazySyncBytecodeInjector` 的 ASM `InsnList` 生成逻辑（操作 `ClassNode`，与 SPI 无关）、`SyncTargetIndex`/`LazySyncTargetIndex` 的注解扫描（`AnnotationData` 两侧兼容，仅 `FMLLoader.getCurrent()`→`LoadingModList.get()` 一处）。

**风险/验证点**：CoreMod transformer 在 game layer 类加载时执行，`AnvilLibSync.LAZY_SYNC_MANAGER` 的 GETSTATIC 引用要求 anvillib_sync 类可解析（V-10）；jar-in-jar（`jarJar(api(...))` 携带 processor）场景下 services 文件可见性需实测（V-9）。

#### 4.2.3 验收方式

1. `gradlew :module.sync:compileJava` 与 processor 编译通过（SPI 适配层零残留：`grep -rn "neoforgespi.transformation" module.sync` 无匹配）。
2. 行为验证（随 #P18 T8 或手工）：`@Sync` 注解字段在两端同步；LazySync 惰性差分同步生效；Configuration 阶段配置表下发正常。
3. 字节码注入验证：CoreMod 加载后目标类被注入（启动日志 + 反编译核对注入点）。

---

### 4.3 回移植项 #P3：module.explosion 整模块（低）

#### 4.3.1 目标

把 dev 侧分层球形爆炸执行器模块 `module.explosion`（10 文件）整体回移植到 port/1.21.1，作为新模块 `anvillib-explosion-neoforge-1.21.1`，依赖 port 侧 `anvillib-config-neoforge-1.21.1`。

#### 4.3.2 步骤

```bash
# 1) 提取 10 个文件（explosion/ 包 + data/ 包 + mixin + resources）
for f in $(git ls-tree -r --name-only dev/26.1 -- module.explosion | grep -E '\.(java|json|toml)$'); do
  git show "dev/26.1:$f" > "$f"
done
# 2) build.gradle：依赖 anvillib-config-neoforge-26.1 → anvillib-config-neoforge-1.21.1；
#    port 侧无 module.gradle 约定（anvillib.needRunConfig* 不生效），run 配置在模块 build.gradle 内自建，
#    参照 port module.config/build.gradle（含 data run 段）
# 3) settings.gradle 注册 module.explosion → anvillib-explosion-neoforge-1.21.1
# 4) mixins.json compatibilityLevel: JAVA_25 → JAVA_21（port 工具链 Java 21）
# 5) AnvilLibExplosionConfig 去掉 group = "anvillib"（port @Config 无 group 属性，见 4.3.3）；
#    zh_cn.json 语言键改写（section 名变化），en_us.json 由 runData 重生成
```

#### 4.3.3 API 适配点

| dev 侧（26.1） | port 侧（1.21.1） | 落点 |
|---|---|---|
| `RecipeManager.getRecipes()` 返回 `Collection<RecipeHolder<?>>` | 同名存在（javap） | `AnvilLibExplosion` 无需改 |
| `Ingredient.items()`（Stream\<Holder\<Item>>） | `Ingredient.getItems()` 返回 `ItemStack[]`（javap） | `AnvilLibExplosion` |
| `ItemStackTemplate`（`count()`/`item().value()`） | `ItemStack`（`getCount()`/`getItem()`） | `AnvilLibExplosion` |
| `BuiltInRegistries.BLOCK.get(TagKey)` | `Registry.getTag(TagKey)` 返回 `Optional<HolderSet.Named<Block>>`（javap） | `AnvilLibExplosion` |
| `Identifier` | `ResourceLocation.fromNamespaceAndPath` | `AnvilLibExplosion` |
| `recipe.input()` | 无此方法；改用 `recipe.getIngredients().get(0)`（`AbstractCookingRecipe` 恒为单元素） | `AnvilLibExplosion` |
| `SingleItemRecipe` 的 `result` 字段（dev 经 mixin 访问器） | 1.21.1 `SingleItemRecipe` 为抽象类，`protected final ItemStack result` 存在（javap）——`@Accessor ItemStack getResult()` 原结构可用，仅类型换 `ItemStack` | `mixin/SingleItemRecipeAccessor.java` |
| `entity.hurtServer(level, source, amount)`（26.1 专属） | `Entity.hurt(DamageSource, float)` 返回 boolean（javap） | `ExplosionExecutor` 的 entityProcessor 默认 lambda |
| `OnDatapackSyncEvent`（`getPlayerList().getServer()`）、`ServerStartedEvent` | 同名存在（NF sources） | `AnvilLibExplosion` 无需改 |
| `GatherDataEvent.Client` | 单一 `GatherDataEvent`（`getGenerator()`/`getPackOutput()`/`includeClient()` 存在）；`addProvider(event.includeClient(), ...)` 参照 port `module.multiblock` 的 `AnvilLibDatagen` | `data/AnvilLibDatagen.java`、`data/provider/ModLanguageProvider.java` |
| `@Config(group = "anvillib")` | port `@Config` 仅 `name()`/`type()`，**无 `group` 属性**（git show port 核实）→ 去掉 `group`；语言键 section 名变化（`section.anvillib.explosion.common.toml` → `section.anvillib_explosion.common.toml`） | `AnvilLibExplosionConfig.java` |

**逐字可搬（无需改动）**：`ExplosionSession.java`（`ServerTickEvent.Post`、`NeoForge.EVENT_BUS.register/unregister`、`ServerLevel.getEntities()` 公开返回 `LevelEntityGetter` 且含 `getAll()`、`GameEvent.Context.of(Entity, BlockState)`、`Block.UPDATE_CLIENTS`、`Block.dropResources` 6 参重载、`FluidState.createLegacyBlock()`、`Level.getHeight(Heightmap.Types, int, int)`、Guava `MultimapBuilder`、log4j `TriConsumer`——全部 javap 核实存在）。`ExplosionExecutor` 其余（`damageSources().explosion(Entity, Entity)`、`block.defaultDestroyTime()`、`block.builtInRegistryHolder()`、`Tags.Blocks.GLASS_BLOCKS/GLASS_PANES`、`BlockTags.LEAVES/REPLACEABLE`）均存在。

#### 4.3.4 验收方式

1. `gradlew :module.explosion:compileJava` 通过；`gradlew :module.explosion:runData` 生成 lang 产物并核对 section 键。
2. 行为验证（随 #P18 T9）：`ServerTickEvent.Post` 驱动的分层球壳破坏按概率半径执行，熔化替换（熔炼配方表）生效，实体处理器（`hurt`）与掉落/熔化路径正确。
3. `grep -rn "ItemStackTemplate\|hurtServer\|group" module.explosion/src` 复核零残留。

---

### 4.4 回移植项 #P4：module.collision 整模块（极低）

#### 4.4.1 目标

把 dev 侧纯数学 AABB×三角形 SAT 碰撞检测模块 `module.collision`（3 文件）整体回移植，作为新模块 `anvillib-collision-neoforge-1.21.1`。

#### 4.4.2 步骤

```bash
# 1) 提取 3 个文件（AnvilLibCollision.java、CollisionTest.java、package-info.java 如有）
for f in $(git ls-tree -r --name-only dev/26.1 -- module.collision | grep '\.java$'); do
  git show "dev/26.1:$f" > "$f"
done
# 2) build.gradle 为 dependencies {} 空块（无平台绑定）；mods.toml + 空 mixins.json 骨架
#    （模块为「无 @Mod 类的 mod jar」纯工具，port 侧同构）
# 3) settings.gradle 注册 module.collision → anvillib-collision-neoforge-1.21.1
```

#### 4.4.3 API 适配点

无。全文件仅引用 `org.joml.Vector3d/Vector3dc/Vector3fc`（joml 随 MC 1.21.1 提供，编译期在 neoForge 依赖里）；`CollisionTest.java` 为纯 Java（`System.out`/`System.nanoTime`/LCG 伪随机），自带断言与基准，为手动运行的自测入口（注意其 `static void main()` 无 `String[]` 参数，非标准 JVM 入口，IDE 手动调用）。

#### 4.4.4 验收方式

1. `gradlew :module.collision:compileJava` 通过。
2. 手动运行 `AnvilLibCollision` 测试入口（静态 SAT 13 轴 / 三轴独立扫掠 / 真扫掠碰撞三个入口 + `overlapOnAxis` 公开工具）断言全部通过、基准无异常。
3. `grep -rn "net.minecraft\|net.neoforged" module.collision/src` 确认零 MC/NF import。

---

### 4.5 回移植项 #P5：module.space-select 整模块（中）

#### 4.5.1 目标

把 dev 侧选区（区域框选/线框渲染/滚轮缩放）模块 `module.space-select`（18 文件）整体回移植，作为新模块 `anvillib-space-select-neoforge-1.21.1`，依赖 port 侧 `anvillib-network-neoforge-1.21.1`。

#### 4.5.2 步骤

```bash
# 1) 提取 18 个文件（space_select/ 包 + client/ + event/ + network/ + resources）
for f in $(git ls-tree -r --name-only dev/26.1 -- module.space-select | grep -E '\.(java|json|toml)$'); do
  git show "dev/26.1:$f" > "$f"
done
# 2) build.gradle：jarJar 依赖 anvillib-network-neoforge-26.1 → anvillib-network-neoforge-1.21.1；
#    port 无 module.gradle → 模块无 run 需求可不建 run 配置
# 3) settings.gradle 注册 module.space-select → anvillib-space-select-neoforge-1.21.1
# 4) 按 4.5.3 逐文件反替换；DistrictRenderer 渲染路径重写（本项唯一实现重写点）
```

#### 4.5.3 API 适配点

| dev 侧（26.1） | port 侧（1.21.1） | 落点 |
|---|---|---|
| `Identifier` | `ResourceLocation.fromNamespaceAndPath` | `AnvilLibSpaceSelect`、网络层 |
| `ARGB`（26.1 新类，javap「找不到类」） | 手写 `(a<<24)|(r<<16)|(g<<8)|b` int 打包 | `District.color()` |
| `player.getInventory().getSelectedSlot()`（javap：1.21.1 `Inventory` 无此方法） | 公开字段 `Inventory.selected` | `DistrictManager`、`SpaceSelectItem` |
| `ClientPacketDistributor.sendToServer(payload)`（26.1 新增；NF 21.1 sources 无 `client/network` 目录） | `net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload)` | `ClientDistrictManager` |
| `RenderLevelStageEvent.AfterTranslucentParticles` 子类 + `event.getLevelRenderState()` | 单一 `RenderLevelStageEvent`：`getStage()`（`Stage.AFTER_TRANSLUCENT_BLOCKS`/`AFTER_PARTICLES`）、`getPoseStack()`、`getCamera()`（NF sources） | `DistrictRenderer` |
| `BlockOutlineRenderState`、`LevelRenderState`、`cameraRenderState.pos`、`windowRenderState.appropriateLineWidth` | 无对应物（1.21.1 无 `client/renderer/state/level` 包）；相机位置取 `event.getCamera().getPosition()`，线宽用默认 | `DistrictRenderer` |
| `ShapeRenderer`（1.21.1 不存在，1.21.2 才引入） | 公开静态 `LevelRenderer.renderVoxelShape(PoseStack, VertexConsumer, VoxelShape, dx, dy, dz, r, g, b, a, boolean)` 与 `renderLineBox(PoseStack, VertexConsumer, AABB, r, g, b, a)`（javap） | `DistrictRenderer`（dev 的 `BlockOutlineRenderState` 分支渲染改写为直接 `renderVoxelShape(shape, 平移量, r,g,b,a)`；`DEBUG_SHAPES` 分支保留 `renderLineBox` 逐 shape 绘制） |
| `RenderTypes.lines()`（26.1 类） | `RenderType.lines()`（javap） | `DistrictRenderer` |
| `mc.hasControlDown()`/`mc.hasAltDown()`（javap：1.21.1 `Minecraft` 无此方法） | 静态 `Screen.hasControlDown()`/`Screen.hasAltDown()`（javap） | `SpaceSelectScrollHandler` |
| `Direction.getApproximateNearest(x,y,z)` | `Direction.getNearest(double,double,double)`（javap） | `SpaceSelectScrollHandler` |
| `InputEvent.MouseScrollingEvent` + `getScrollDeltaY()` + `setCanceled(true)` | 同名存在（NF sources）；`player.getViewVector(1.0F)` 存在 | `SpaceSelectScrollHandler` 无需改 |

**无需改动**：`AnvilLibSpaceSelectClient`（`@Mod(value=..., dist=Dist.CLIENT)` 为 21.1 标准）、`PlayerCreateDistrictEvent`+Handler（`PlayerEvent(Player)` 构造 + `getEntity()`、`@EventBusSubscriber(modid=...)` 属性名一致）、`BlockPos.MutableBlockPos`（公开无参构造）、`Direction.getStepX`、`Direction.Axis.choose`、`Vec3` 公开字段、`AABB`、`Shapes.create(double×6)`、`Minecraft.hitResult`、`CustomPacketPayload.Type` record（构造参数为 `ResourceLocation`）、`ByteBufCodecs.BOOL`、`BlockPos.STREAM_CODEC`、`RegisterPayloadHandlersEvent.registrar(String)`。

#### 4.5.4 验收方式

1. `gradlew :module.space-select:compileJava` 通过。
2. 行为验证：手持选区物品框选区域 → `DistrictManager` 创建区域；`DistrictRenderer` 在 `RenderLevelStageEvent` 各 stage 正确绘制线框（平移量/颜色语义目测确认，V-16）；滚轮缩放/移动（`Screen.hasControlDown` 组合键 + `Direction.getNearest`）生效；网络包（`SpaceSelectPayload` 经 port `NetworkRegistrar` 注册）双向收发正常。
3. `git diff port/1.21.1 dev/26.1 -- module.space-select` 只读复核只剩预期差异。

---

### 4.6 回移植项 #P6：module.font 整模块（中）

> ⚠️ 2026-08-12：module.font 已落地为独立模块，其与 module.rendering 的依赖已随渲染模块移除而删除（build.gradle 不再声明 anvillib-rendering-neoforge-1.21.1）。

#### 4.6.1 目标

把 dev 侧 SDF 字体模块 `module.font`（25 文件，AWT 字体 → CPU 生成 SDF 图集 → GPU 采样渲染）整体回移植，作为新模块 `anvillib-font-neoforge-1.21.1`；约 60% 纯 CPU 逻辑直接搬，GPU 上传/绘制层与 GUI 屏幕按 1.21.1 API 改写。**依赖 module.rendering 的 1.21.1 版（#P7）**（build.gradle jarJar 依赖 `anvillib-rendering-neoforge-1.21.1`；纯逻辑部分可在 #P7a 完成后先行落地）。

#### 4.6.2 步骤

```bash
# 1) 提取 25 个文件
for f in $(git ls-tree -r --name-only dev/26.1 -- module.font | grep -E '\.(java|json|toml|fsh|vsh|glsl)$'); do
  git show "dev/26.1:$f" > "$f"
done
# 2) build.gradle：依赖 anvillib-rendering-neoforge-26.1 → anvillib-rendering-neoforge-1.21.1；
#    gradle.properties interface_injection=true 保留（1.21.1 支持）
# 3) settings.gradle 注册 module.font → anvillib-font-neoforge-1.21.1
# 4) 按 4.6.3 分类处理：直接移植 9 项原样搬 + 反向适配 8 项改写
```

#### 4.6.3 分类与 API 适配点

**可直接移植（9 项）**：

| 文件 | 适配点 |
|---|---|
| `sdf/SdfGlyphAtlas.java`、`sdf/SdfGlyphPage.java` | 核心为 AWT（`Font`/`Graphics2D`/`BufferedImage`）+ 并发（虚拟线程、`CompletableFuture`）+ Dead Reckoning EDT 距离场算法，无 MC 渲染 API；`SdfGlyphPage.textureId` 的 `Identifier`→`ResourceLocation`；`Math.clamp`（Java 21 ✓）、`Thread.ofVirtual()`（Java 21 ✓，port `java_version=21`）；`GLYPH_EXECUTOR` 虚拟线程保留 |
| `sdf/SdfTextLayout.java`、`ALFont.java` | 纯 CPU（quad 生成 + LRU 缓存）；`Identifier` 改名；`Mth.ceil`、`FormattedCharSequence.accept`、`FormattedText.getString` 1.21.1 全有；含 `flatten`/`wrapLines` 工具原样搬 |
| `FontManager.java` | 纯 AWT/Swing（`GraphicsEnvironment`/`UIManager`） |
| `AnvilLibFontConfig.java` | 纯 Gson + `FMLLoader.getCurrent().getGameDir()`——报告子代理称 1.21.1 可用，但与其它子代理「1.21.1 的 `FMLLoader` 无 `getCurrent()`」（loader 4.0.42/4.0.43 源码）结论矛盾，**标为待验证（V-12）**：以编译为准，报错则改 `FMLPaths.GAMEDIR.get()` |
| `AnvilLibFont.java` | `IConfigScreenFactory`（21.1.226 sources 核实存在） |
| `data/AnvilLibFontData.java` | `GatherDataEvent.Client` ❌ → 单一 `GatherDataEvent` + `includeClient()` 判断（21.1.226 构造器 `(ModContainer, DataGenerator, DataGeneratorConfig, ExistingFileHelper)` 核实）；`generator.getPackOutput()`、`LanguageProvider`、`Util.makeDescriptionId` 存在 |
| shader 资源（`sdf_text.fsh/vsh`） | GLSL 原样搬，补 1.21.1 `.json` 声明 |

**需反向适配（8 项）**：

| 文件 | 适配点 |
|---|---|
| `sdf/SdfAtlasTexture.java` | 上传层：`GpuDevice.createTexture/createCommandEncoder().writeToTexture` + `RenderSystem.getSamplerCache()` → `DynamicTexture`（或 `NativeImage` 直传）+ `TextureManager.register(ResourceLocation, AbstractTexture)` + `RenderSystem.setShaderTexture`；`toNativeImage` 像素拷贝逻辑复用；`SdfTexture` 内部类改为基于 `DynamicTexture`；`PageEntry.version` 版本号缓存机制保留 |
| `ALFPipelines.java` | 26.1 `RenderPipeline` 注册 → 1.21.1 `RegisterShadersEvent` 注册 `ShaderInstance`（port 侧 wheel `LibShaders` 先例）；`SDF_TEXT_FORMAT` 用 1.21.1 `VertexFormat` 构造器重建（`new VertexFormat(ImmutableList.of(...), Mode.QUADS)`，1.21.1 无 builder） |
| `sdf/SdfTextRenderer.java` | 绘制提交走 `GuiGraphicsExtractor.submitGuiElementRenderState` → `RegisterShadersEvent` 注册 SDF shader + `GuiGraphics` 的 `BufferSource`/`RenderType` 直接画 quad；**分段样式逻辑**（颜色/粗斜体/下划线/删除线/混淆分段、`obfuscateCodepoint`、`drawDecorations`、居中/换行）纯逻辑原样复用；`drawAtlasPipeline` 改为 `guiGraphics.draw`（自定义 RenderType）或直接 `BufferBuilder` 提交 |
| `sdf/state/SdfTextRenderState.java` | `buildVertices` 的 UV/坐标计算复用；26.1 状态对象基类替换为 1.21.1 等价载体 |
| `extension/GuiGraphicsExtractorExtension.java` + `mixin/GuiGraphicsExtractorMixin.java` + `interface_injections.json` | 目标类 `GuiGraphicsExtractor` 1.21.1 不存在 → 注入 `net/minecraft/client/gui/GuiGraphics`（interface injection 机制 1.21.1 可用，port 侧已有先例）；`anvillib$text` 系列 11 个默认方法签名保留（`FormattedText`/`FormattedCharSequence`/`Component` 1.21.1 全有）；`ARGB`（26.1 新类）→ `FastColor.ARGB32` 或直接 int 运算（`textWithBackdrop` 中 `ARGB.multiply` 需手写） |
| `screen/FontConfigScreen.java`、`screen/FontTestScreen.java`、`screen/widget/Dropdown.java` | 26.1 `Screen.extractRenderState`/widget `extractWidgetRenderState`/`MouseButtonEvent` → 1.21.1 `Screen.render(GuiGraphics, int, int, float)`、`AbstractWidget.renderWidget(GuiGraphics)`、`mouseClicked/mouseReleased/mouseDragged/mouseScrolled`；布局/滚动/选项逻辑（`Dropdown` 的 expanded/scrollOffset/shielding 状态机）纯逻辑复用；`Button.builder().size().pos()`、`Component.translatable` 存在；绘制调用改 `guiGraphics.fill/drawString/drawCenteredString` |

#### 4.6.4 验收方式

1. `gradlew :module.font:compileJava` 通过（依赖 `:module.rendering` 编译产物或已发布的 `-1.21.1` 快照）。
2. `gradlew :module.font:runData` 生成 `en_us.json` 并核对。
3. 行为验证（运行 `runClient`）：`FontConfigScreen`/`FontTestScreen` 打开正常；SDF 文字渲染（含粗斜体/下划线/删除线/混淆分段）与居中/换行行为正确；`anvillib$text` 系列扩展在 `GuiGraphics` 上可用；字体配置（`FontManager` AWT 字体枚举）生效。

---

### 4.7 回移植项 #P7：module.rendering（高 · 按功能块分批）

> ⚠️ 2026-08-12：module.rendering 已从 port/1.21.1 整体移除，本节内容仅为历史计划记录。

#### 4.7.0 总体说明与拆批依据

`module.rendering`（约 113 文件）在 port/1.21.1 上完全不存在（`git diff --name-status` 全部为 `A`）。其功能层深度构建在 26.1 全新渲染架构之上（`GpuDevice/CommandEncoder/RenderPass/GpuBuffer`、`RenderPipeline/RenderState`、`SubmitNodeStorage/FeatureRenderDispatcher/GameRenderState`、`GuiGraphicsExtractor`），**不能整模块移植**；1.21.1 等价物为 `RegisterShadersEvent + ShaderInstance`、`RenderTarget`、`VertexBuffer`、`GuiGraphics`、`MultiBufferSource`。按功能块拆为 **#P7a–#P7e** 五批，每批独立步骤与验收，作为单一模块（`anvillib-rendering-neoforge-1.21.1`）落地。

**已核实的平台基线**：port 侧 `java_version=21`（`Math.clamp`、虚拟线程、`List.getFirst` 均可用）；port 侧已有 `RegisterShadersEvent + ShaderInstance` 用法（`module.wheel/.../init/LibShaders.java`）；`IConfigScreenFactory` ✓；`interface_injections.json` 机制 ✓（module.moveable-entity-block、module.recipe 已用）；NeoForge 21.1.226 有 `RenderFrameEvent`、`RenderLevelStageEvent`、`RegisterClientReloadListenersEvent`、`GatherDataEvent`（无 `Client` 子类），**无** `RegisterRenderPipelinesEvent`、`RegisterPictureInPictureRenderersEvent`、`AddClientReloadListenersEvent`、`ConfigureMainRenderTargetEvent`（后两者名称/形态已变化，均经 21.1.226 sources jar 核实）。

**模块落位**（#P7a–#P7e 共用）：`settings.gradle` 注册 `module.rendering` → `anvillib-rendering-neoforge-1.21.1`；mods.toml 按 port 侧模板对齐（`${loader_version_range}` 等模板变量由 port 构建脚本注入，V-7）；mixins.json 按 1.21.1 实际 mixin 清单重建；accesstransformer.cfg 整体重写（dev AT 目标 `GuiRenderer$Draw`、`GpuDevice`、`RenderType.state` 等绝大多数 1.21.1 不存在，仅 `BufferBuilder`（`vertices`/`format`/`mode`）与 `RenderType.name/outline` 等少数有意义）。

#### 4.7.1 子项 #P7a：纯逻辑与资源（19 项，可直接移植，低）

**目标**：把与平台无关的 19 项纯逻辑/资源先落地，形成模块骨架。

**步骤**：从 dev 侧提取以下文件原样落位（`foundation/buffers/layout/**` 11 文件 + `Std140LayoutRulesTest.java`、`sdf/Sdf2d.java`、`sdf/SdfParameters.java`、`sdf/SdfRenderType.java`、`sdf/SdfPassType.java`（剥离 `BufferObject` 的 `upload(CommandEncoder,...)` 依赖，见下）、`foundation/buffers/object/BufferObject.java`（反适配）、`foundation/compound/DirtyTracked.java`、`event/MainTargetResizeEvent.java` + `mixins/GameRendererMixin.java`、`event/RegisterComputePipelinesEvent.java`、`integration/mixins/ALRIntegrationCompatMixinPlugin.java`、`mixins/MinecraftMixin.java`、`mixins/RenderTypeMixin.java` + `extension/ALRRenderTypeExtension.java`、`ALRSharedMath.java`、`util/Timer.java`、`foundation/buffers/EmptyVertexConsumer.java`、`EmptyBufferSource.java`、`EmptyOutlineBufferSource.java`、`TransformingVertexConsumerWrapper.java`、`foundation/ALRMeshSorting.java`、`extension/blaze3d/MemoryBarrierFlag.java`、`extension/blaze3d/ALRComputeCapabilities.java`、`foundation/buffers/GpuBufferConstants.java`、shader 资源（7 个 shader + util.glsl））。

**API 适配点**：

| 项 | 适配点 |
|---|---|
| `Sdf2d` | 9 种 SDF 距离函数，仅用 `Mth.length/cos/sin/clamp/sign`（1.21.1 全有），原样搬 |
| `SdfParameters`/`SdfRenderType`/`SdfPassType` | 数据模型 + std140 布局定义，仅依赖 `Mth`/JOML/`BufferLayout`；剥离 `BufferObject` 基类中 `upload(CommandEncoder, GpuBufferSlice)` 与 `DynamicUniformStorage` 依赖 |
| `BufferObject` 基类 | `implements DynamicUniformStorage.DynamicUniform`（26.1 新类）→ 去掉接口与 `createDynamicStorage`；`upload(CommandEncoder, GpuBufferSlice)` → 改为「写入用户提供的 `ByteBuffer`」；各 UBO 子类（bloom/glitch/blur 参数类）改动极小 |
| `Timer` | `getDeltaTracker()`（26.1）→ `Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(paused)`，一行 |
| `EmptyVertexConsumer`/`TransformingVertexConsumerWrapper` | `VertexConsumer` 接口方法集（`addVertex/setColor/setUv/setUv1/setUv2/setNormal/setLineWidth`）两版本一致 |
| `EmptyBufferSource`/`EmptyOutlineBufferSource` | `MultiBufferSource.BufferSource` 1.21.1 存在；构造器参数 `(BufferAllocator, RenderType)` 需核对（dev 侧 `super(null, null)` 需小调，V-3） |
| `ALRMeshSorting` | `VertexSorting`/`CompactVectorArray`/`MeshData.sortQuads`/`SortState.buildSortedIndexBuffer` 1.21.1 均存在（1.20.2+ 稳定 API，LevelRenderer 使用；存在性判定见 V-1） |
| `MinecraftMixin` | `Minecraft.<init>(GameConfig)` 与 `updateLevelInEngines(ClientLevel, boolean)` 1.21.1 均存在（注入点签名一致） |
| `RenderTypeMixin` + `ALRRenderTypeExtension`（bloom 标记字段） | 1.21.1 `RenderType` 存在；interface injection 机制可用（port 侧已有 `interface_injections.json` 先例） |
| `MainTargetResizeEvent` + `GameRendererMixin` | `Event`/`IModBusEvent`/`ModLoader.postEvent` 1.21.1 存在；`GameRenderer.resize(int,int)` 存在（长期稳定方法） |
| `RegisterComputePipelinesEvent` | 纯 `Event`/`IModBusEvent`，1.21.1 存在；随 #P7e 的 compute 结构一起迁移 |
| `ALRIntegrationCompatMixinPlugin` | `IMixinConfigPlugin` 机制版本无关 |
| `MemoryBarrierFlag`/`ALRComputeCapabilities`/`GpuBufferConstants` | 纯 GL 常量，仅依赖 LWJGL |
| `Mth.roundToward` | 1.20.2+ 引入，1.21.1 `net.minecraft.util.Mth` 存在——**待验证（V-1）**：本机 gradle 缓存无 1.21.1 client jar 可反查，移植首个编译时以 1.21.1 client sources 复核 |

**验收**：`gradlew :module.rendering:compileJava` 通过；`Std140LayoutRulesTest` 作为 JVM 测试随 `gradlew :module.rendering:test` 运行通过（纯逻辑验证，符合仓库 TDD 规范）。

#### 4.7.2 子项 #P7b：后处理三件套（Bloom / GaussianBlur / Glitch，需反向适配，高）

**目标**：把「全屏后处理 + 高斯金字塔」三件套的算法与 GLSL 保留，执行层从 26.1 `GpuDevice/CommandEncoder/RenderPass` 体系重写到 1.21.1 `RenderTarget + ShaderInstance` 体系。

**来源提交**：`7121bf6`、`b5f5420`（#17/#20）、`6bb3269`（#38 resize 修复）、`0e8290d`（#92 稳定化）、`64bcf21`（#33 GaussianBlur）、`eccdc6d`（#43 Glitch）、`6b5a544`（#44）。

**步骤与适配点**：

```bash
# dev 侧文件（落点新包 dev.anvilcraft.lib.v2.rendering.bloom / blur / glitch）：
# bloom/BloomPostEffect.java、BloomParametersUbo.java、BloomPipelineParametersUbo.java、TransformsUbo.java、
# BloomRenderCallback.java、foundation/BloomSubmitNodeStorage.java、ALRPostEffects.java、
# blur/GaussianBlur.java、blur/BlurParametersUbo.java、glitch/GlitchPostEffect.java、glitch/GlitchParametersUbo.java、
# shaders（apply_bloom/down_sample/up_sample/blit 及 glitch/blur 相关）
```

- **保留**：5 级降采样/升采样流程、全部 GLSL、UBO 参数类的布局定义（`BlurParametersUbo`/`GlitchParametersUbo` 直接复用）。
- **重写**：
  - `RenderTarget`（`MainTarget`/`TextureTarget` 存在）+ 自定义 `ShaderInstance`（`RegisterShadersEvent` 注册，port 侧 wheel 已有先例）+ 全屏四边形（`BufferUploader`/`Tesselator`）逐 pass 绘制；UBO 上传改为 `ShaderInstance` uniform 或自管 GL 缓冲；
  - `RenderSystem.outputColorTextureOverride`（26.1 字段）改为临时切换 framebuffer 目标；bloom 重绘收集改为 `MultiBufferSource.BufferSource` 重画一遍（丢弃 `SubmitNodeStorage` 路径——`BloomSubmitNodeStorage`/`CompoundSubmitNodeStorage` 本身不可移植，见第 5 章）；
  - 尺寸变化监听用 `GameRendererMixin`（#P7a 已落位）；
  - `GlitchPostEffect` 的 `DeltaTracker` → `Minecraft.getTimer().getGameTimeDeltaPartialTick`；`process(GpuTextureView)` 改为「输入 RenderTarget 颜色纹理 → 输出 RenderTarget」。
- **挂接**：`RenderLevelStageEvent.AfterTranslucentParticles/AfterLevel`（均存在）。

**验收**：`gradlew :module.rendering:compileJava` 通过；运行验证 Bloom/Blur/Glitch 后处理效果正确（含窗口 resize 后尺寸跟随、帧稳定）；glitch 的时间驱动动画正常。

#### 4.7.3 子项 #P7c：SDF GUI 图形（需反向适配，中）

**目标**：把 `Sdf2d`（#P7a 已落位）之上的 `SdfGraphics` 绘制编排从 26.1 GUI 提交体系重写到 1.21.1 `GuiGraphics + BufferSource + 自定义 RenderType`。

**来源提交**：`494274e`（#21）、`27a8c79`、`05a1f6c`（#39）、`e8ee3f9`（#40）、`d7a8f5d`（#38 AMD 修复）、`6b5a544`（#44）、`6d5a97b`（#45）。

**步骤与适配点**：

- `SdfGraphics._draw` 改为「用 `GuiGraphics` 的 `BufferSource` + 自定义 `RenderType`（绑定 SDF shader）提交 4 顶点 quad，参数经 uniform 上传」；`graphics.pose()` 的 `Matrix3x2f` 换成 `PoseStack` 2D 变换；命中测试 `collide` 原样保留；旋转/居中数学全部复用。
- `MAX_SDFS=256` 的 UBO 数组在 1.21.1 若走 uniform 需改为按需绑定或保留自管 GL UBO（`GL46.glBindBufferRange` 可用，LWJGL 全量存在）。
- `mixins/GuiRendererMixin.java`（SDF 注入部分）**不迁移**：目标类 `GuiRenderer`/`GuiElementRenderState` 1.21.1 不存在（见第 5 章）。
- shader 资源（`sdf_graphics.fsh/vsh`）GLSL 复用 + 补 `.json` 声明。

**验收**：`gradlew :module.rendering:compileJava` 通过；运行验证 SDF 图形（圆/矩形/圆角等 9 种距离函数）绘制正确、命中测试准确。

#### 4.7.4 子项 #P7d：CachedBER（缓存型 BER 渲染管线，需反向适配，高——难度最高的一项）

**目标**：把「BER 渲染结果按 chunk 缓存为 GPU 顶点缓冲、视锥剔除、半透明排序、bloom 双画」的核心功能保留，执行层从 26.1 `GpuBuffer/RenderPass/SubmitNodeStorage` 重写到 1.21.1 `VertexBuffer + RenderSystem.draw + RenderType.shader()` 老路径。

**来源提交**：`2751be4`（#26）、`cff1802`（#29）、`d121047`（#68）、`48bfe55`、`fba1027`（#76）、`084c503`（#85）、`0e8290d`（#92）。

**步骤与适配点**：

- **保留**：`CachedBlockEntityRenderingPipeline` 的 chunk 表、`RebuildTask` 编译/上传队列、`blockRemoved/update/forcedUpdate` 失效逻辑；`ALRMeshSorting` 原样搬（#P7a 已落位）。
- **重写**：
  - `GpuBuffer` → `VertexBuffer`（`com.mojang.blaze3d.vertex.VertexBuffer`，`upload(MeshData)`/`bind()`）；
  - 渲染走 `RenderSystem.draw` + `RenderType.shader()` 老路径（port 侧 wheel 已证明 ShaderInstance 路径可用）；
  - `CachedBlockEntityRenderer` 接口改为单阶段 `render(...)`（1.21.1 的 BER 调度是 `BlockEntityRenderDispatcher.render(be, partialTick, poseStack, bufferSource, light, overlay, camera, frustum)` 单阶段，无 extract/submit 两阶段）；
  - `FullyBufferedBufferSource`/`VertexBufferHost` 随管线重写。
- **挂接**：`RenderLevelStageEvent.AfterOpaqueBlocks/AfterTranslucentFeatures` + `RenderFrameEvent.Pre`。
- **Iris 集成**（`integration/IrisSupport.java` + 两个集成 mixin）：`net.irisshaders.iris.api.v0.IrisApi`（`isShaderPackInUse`）在 1.21.1 的 Iris 1.7.x 中存在（长期 API）；`net.irisshaders.iris.vertices.ImmediateState` 与 `skipExtension`/`isRenderingLevel` 字段需按 1.21.1 Iris 版本核实（dev compileOnly 依赖为 Iris 1.10.9+26.1，1.21.1 对应 1.7.x，**待验证 V-2**）；`oculus` 分支 1.21.1 不存在，`ModList.get().isLoaded("oculus")` 判断可保留但恒假；build.gradle 中 Modrinth 依赖坐标同步替换（**待验证 V-6**：坐标需移植时查询）。

**验收**：`gradlew :module.rendering:compileJava` 通过；运行验证：缓存 BER 渲染正确（含视锥剔除与半透明排序）、方块移除/更新后失效重建正确、bloom 双画效果正常、Iris 开启时回退路径正常（无 Iris 时可跳过）。

#### 4.7.5 子项 #P7e：GPU 计算管线扩展（Compute，需反向适配，高）

**目标**：把 GL 4.3 compute 管线扩展保留全部 API 形状（`ALRComputePipeline.builder()` 等）与 binding 模型，后端从「26.1 `GpuDevice` mixin 扩展」重写为独立自管 GL 工具层。

**来源提交**：`7d07f20`（#41）、`0e8290d`（#92）。

**步骤与适配点**：

| 部分 | 适配点 |
|---|---|
| `extension/blaze3d/MemoryBarrierFlag.java`、`ALRComputeCapabilities.java`、`foundation/buffers/GpuBufferConstants.java` | 【直接搬】纯 GL 常量，仅依赖 LWJGL（#P7a 已落位） |
| `ALRComputePipeline`/`ALRComputePass`/bindings | 【直接搬】纯描述结构；`ShaderDefines` 是 26.1 类 → 改为简单 `Map<String,String>` defines（1.21.1 的 `ShaderInstance` 用 `Supplier<String>` 方式，无 ShaderDefines，**待验证 V-8**） |
| `ALRComputeShaderManager` | 【直接搬 + 小适配】`SimplePreparableReloadListener` 机制 1.21.1 存在，经 `RegisterClientReloadListenersEvent` 注册；`ShaderManager.createPreprocessor` 为 26.1 静态方法 → 1.21.1 的 `ShaderInstance` 内部有自己的 preprocessor 逻辑，改为自实现或简化 |
| `GlComputePassBackend`/`ALRComputePassBackend`/扩展接口与全部 6 个 blaze3d mixins（`GpuDevice/CommandEncoder/GlDevice/GlCommandEncoder/DirectStateAccess/GlDebugLabel`） | 【重写】mixin 目标 1.21.1 不存在；改为独立类持有 program/buffer 状态机；底层 GL 能力（`glDispatchCompute`、`glBindImageTexture`、`glBindBufferRange`(SSBO/atomic counter)、`glMemoryBarrier`、`glObjectLabel`）在 1.21.1 的 LWJGL 中完整可用；program 编译/链接逻辑（`GL46.glCreateShader(ARBComputeShader.GL_COMPUTE_SHADER)` 等）逐行复用 |
| `event/RegisterComputePipelinesEvent.java` | 机制保留（#P7a 已落位；纯 `Event`/`IModBusEvent`） |

**验收**：`gradlew :module.rendering:compileJava` 通过；运行验证：compute pipeline 注册、program 编译链接、dispatch 与内存屏障执行正确（可用 #P18 之外的临时测试或 module.test 现有用例核对，若随迁则登记）。

#### 4.7.6 #P7 收尾（资源与元数据）

- shader 文件原样搬 + 为每个管线补 1.21.1 的 `.json` 声明（1.21.1 的 `ShaderInstance` 加载约定 `shaders/core/xxx.json` 声明 uniform + `.vsh/.fsh`）；
- mixins.json 按 1.21.1 实际 mixin 清单重建；AT 文件重写（见 4.7.0）；mods.toml 按 port 模板；`AnvilLibRendering` 挂接点中 `AddClientReloadListenersEvent` → `RegisterClientReloadListenersEvent`。

**验收**：`gradlew :module.rendering:build`（含 jar/sourcesJar）通过；`gradlew :module.rendering:runData`（如有 datagen 需求）通过。

---

### 4.8 回移植项 #P8：renderdoc-loader（低）

> ⚠️ 2026-08-12：renderdoc-loader 已随 module.rendering 一并从 port/1.21.1 移除，本节内容仅为历史计划记录。

#### 4.8.1 目标

把 dev 侧 RenderDoc 调试用 Java Agent 模块 `renderdoc-loader`（2 文件）回移植，作为 `renderdoc-loader-internal`，实现 `premain`/`agentmain` 加载 RenderDoc DLL。

#### 4.8.2 步骤

```bash
# 1) 提取 2 个 Java 文件（RenderDocAgent.java、package-info.java），源码逐字搬移
git show dev/26.1:renderdoc-loader/src/main/java/dev/anvilcraft/lib/renderdoc/loader/RenderDocAgent.java
git show dev/26.1:renderdoc-loader/src/main/java/dev/anvilcraft/lib/renderdoc/loader/package-info.java
# 2) jar manifest 的 Premain-Class/Agent-Class 属性照抄
# 3) build.gradle 按 4.8.3 改写
```

#### 4.8.3 API 适配点

| dev 侧（26.1） | port 侧（1.21.1） |
|---|---|
| `JavaLanguageVersion.of(25)` | `JavaLanguageVersion.of(21)`（port 工具链） |
| `alias libs.plugins.jreleaser`（port `gradle/libs.versions.toml` 无 jreleaser 别名，仅 modDevGradle/lombok/machete） | 改为 `id 'org.jreleaser' version '1.23.0'`（或省略——本模块不发布中央仓库） |
| `alias libs.plugins.lombok` | port toml 有该别名，可直接用 |
| dev 侧 `module.gradle` 的 `clientRenderDoc`/`clientRenderDocIrisWorkaround` run 配置引用该 agent jar | port 无 module.gradle；如需 RenderDoc 调试需在目标模块 build.gradle 手工接线（`-javaagent:` + `-Dneoforge.rendernurse.renderdoc.library=...`） |

功能代码零依赖：仅 `java.lang.instrument` + jetbrains annotations compileOnly，`System.load` 读取 `renderdoc.library.path`（或 `neoforge.rendernurse.renderdoc.library`）系统属性。

#### 4.8.4 验收方式

1. `gradlew :renderdoc-loader:compileJava` 通过，jar 产物 manifest 含 `Premain-Class`/`Agent-Class`。
2. 冒烟：`java -javaagent:renderdoc-loader.jar -Drenderdoc.library.path=<dll> -version`（或任一 JVM 启动）确认 agent 加载不抛异常（无 RenderDoc DLL 时仅跳过加载）。

---

### 4.9 回移植项 #P9：module.config——group 分组 + TranslatableEnum + TOML key 点分隔（低）

#### 4.9.1 目标

把 dev 侧 module.config 的三项功能演进回移植：`@Config(group=…)` 配置分组（子目录文件）、`TranslatableEnum` 枚举翻译 key 生成、TOML 语言 key 点分隔化。

#### 4.9.2 步骤

```bash
# 1) group 分组（3d50d76）：Config.java（+group() 字段）、ConfigManager.java（group 传递）、
#    ConfigRecord.java（+group 记录组件，getFileName() 返回 group/modId-type.toml）
# 2) TranslatableEnum（946938f #56）：util/TranslatableEnum.java 新文件 + ConfigData.java
#    （+ENUM_STRING 常量、enumValueAdd()、枚举字段检测分支、@Slf4j）
# 3) TOML key 点分隔（946938f #56）：FormattingUtil.java（+toPointSplitName，并用 Arrays.stream 重构）、
#    ConfigData.java（TOML_STRING/TOML_TITLE_STRING 的 key 改用 FormattingUtil.toPointSplitName(name)）
# 4) （可选）ConfigManager 模组容器获取逻辑修复（c0a619b）：ModList.get().getModContainerById(modId)
#    → Optional.of(ModList.get()).flatMap(...)——1.21.1 中 ModList.get() 不返回 null，移植后行为等价无副作用，
#    该改动本身有争议（Optional.of(null) 仍抛 NPE），建议评估后决定是否保留
```

#### 4.9.3 API 适配点

| 项 | 核实结论 |
|---|---|
| 配置子目录文件名 | 1.21.1 的 `ConfigTracker.openConfig` 用 `basePath.resolve(config.getFileName())` 拼接路径（`ConfigTracker.java:138`），`setupConfigFile` 中 `Files.createDirectories(file.getParent())` 自动建目录（`ConfigTracker.java:250-251`）——**原生支持子目录**，无平台差异 |
| `TranslatableEnum` | 21.1.226 存在 `net.neoforged.neoforge.common.TranslatableEnum`（含 `getTranslatedName()` 默认方法）；AnvilLib 扩展接口（override `getTranslatedName` + 新增 `getTranslationKey`）可在 21.1 父接口上原样编译；`LanguageProvider`、`Component.translatableWithFallback` 均存在 |
| `enumValueAdd` | 用 `field.get(null)` 反射取枚举常量（`IllegalAccessException` 有日志，符合仓库规范）；`getTranslationKey()` 基于 `getCanonicalName()` 生成，与平台无关 |
| TOML 点分隔 | 纯 Java 字符串处理（`String.split` 正则 `[^A-Za-z0-9]` 切分过滤空串 + `Collectors.joining`），1.21.1 行为一致 |

**破坏性提示**：TOML key 点分隔化会改变语言文件 key（如 `section.<name>` 的 name 改为点分隔形式），使用方现有语言文件需同步——移植 commit 中必须说明；`section.anvillib.explosion.common.toml` 类键名变化会影响 #P3 的 lang 产物（若 #P3 先落地，可在 #P9 完成后补回 `group` 语义并重跑 datagen）。

#### 4.9.4 验收方式

1. `gradlew :module.config:compileJava` 通过。
2. `gradlew :module.config:runData`（或调用模块的 runData）生成 lang 产物，核对 `section.<点分隔名>.common.toml` 键名与枚举翻译 key（`enum.<canonical name>`）正确。
3. 运行验证：`@Config(group="xxx")` 的模块配置生成于 `<config-dir>/xxx/<modid>-common.toml` 子目录。

---

### 4.10 回移植项 #P10：module.integration——数据加载拆分 + meter 递增（低 · 破坏性 API）

#### 4.10.1 目标

把 dev 侧 module.integration 的两项功能演进回移植：数据加载按客户端/服务端拆分（`applyData` → `applyClientData`/`applyServerData`）、集成实例计数进度条递增。

#### 4.10.2 步骤

```bash
# 1) 数据加载拆分（dc4d583）：
#    IntegrationInstance.java：dataLoader 字段拆为 clientDataLoader + serverDataLoader，
#      findVirtual 查找方法名改为 applyClientData / applyServerData（各自 try/catch，失败置 null），
#      invokeData() 拆为 invokeClientData() / invokeServerData()，toString() 同步更新；
#    IntegrationManager.java：loadData 拆为 loadClientData / loadServerData 及对应 loadAll*，
#      load 中 containsType 判断逻辑不变；
#    IntegrationType.java：DATA → CLIENT_DATA + SERVER_DATA
# 2) meter 递增（e8efbd4）：IntegrationManager.compileContent() 循环内 meter.increment()（一行）
```

#### 4.10.3 API 适配点

- 纯 `java.lang.invoke.MethodHandle` 反射 + 枚举逻辑，无 26.1 API；`Integration` 注解、`IntegrationHook` 两侧一致，不受影响。
- 已 grep 确认仓库内无其他模块使用 `IntegrationType.DATA`/`applyData`，破坏面仅限模块自身——但 **API 破坏性**：旧 `applyData` 方法名不再被识别，下游使用方（如 AnvilCraft 主模组）需同步改名，移植 commit 必须说明。
- `StartupNotificationManager`/`ProgressMeter` 1.21.1 存在（port 侧原代码已用 `StartupNotificationManager.popBar(meter)`）。

#### 4.10.4 验收方式

1. `gradlew :module.integration:compileJava` 通过。
2. 行为验证：注册同时含 `applyClientData`/`applyServerData` 的集成实现，启动时两侧各自加载、进度条随实例递增；`@ApiStatus.Internal` 类（如附带迁移 `c72aa30` 时）不对外暴露。

---

### 4.11 回移植项 #P11：module.network——泛型检查修复 + Included 方法 + public（低）

#### 4.11.1 目标

把 dev 侧 module.network 的三处小演进回移植：`PacketData` 泛型类型参数精确匹配修复、`NetworkUtil` 条件筛选发送工具、`PacketProtocol` 枚举 public 化。

#### 4.11.2 步骤

```bash
# 1) PacketData 泛型检查修复（20721b1 #52）：
#    register/PacketData.java 新增 isMatchingTypeArgument(Type, int, Class)；
#    字段判断从 declaringClass.isAssignableFrom(Type.class) 反转为 Type.class.isAssignableFrom(fieldType)，
#    并追加类型实参校验：Type<X> 检查第 0 参、StreamCodec<B, T> 检查第 1 参
# 2) NetworkUtil Included 方法（5733a9d）：
#    util/NetworkUtil.java 新增 sendToAllPlayersIncluded(Predicate<ServerPlayer>, ...) 与
#    sendToAllPlayersInDimensionIncluded(ServerLevel, Predicate<ServerPlayer>, ...)；
#    included == null 时默认全通过；两方法与现有 sendToAllPlayersExcluded / sendToAllPlayersInDimension 对称；
#    顺带将 player.equals(excluded) 改为 Objects.equals 防 NPE
# 3) PacketProtocol public（6220427）：enum → public enum
```

#### 4.11.3 API 适配点

- 纯 `java.lang.reflect.ParameterizedType` 反射逻辑与纯 Java，无平台依赖。
- 已核实：21.1.226 存在 `PacketDistributor.sendToPlayer(ServerPlayer, CustomPacketPayload, CustomPacketPayload...)`（`PacketDistributor.java:50`）；`ServerLifecycleHooks.getCurrentServer()`、`ServerLevel.players()`、`ServerPlayerList.getPlayers()` 均为 port 侧已在用 API。

#### 4.11.4 验收方式

1. `gradlew :module.network:compileJava` 通过。
2. 行为验证：同一类中多个 `Type`/`StreamCodec` 静态字段时注册解析正确（旧逻辑无法区分，修复后可区分）；`sendToAllPlayersIncluded` 过滤发送正确（含 `included == null` 全通过路径）。

---

### 4.12 回移植项 #P12：module.util——OutlineUtil + HolderGetter 重构 + withCount + @EqualsAndHashCode（中）

#### 4.12.1 目标

把 dev 侧 module.util 的四项功能演进回移植：AABB 轮廓直接提取（OutlineUtil + Line + 测试）、predicate 系列 HolderGetter 参数化重构、`ItemIngredientPredicate.withCount`、`BlockStatePredicate` @EqualsAndHashCode；HolderGetter 重构连带 module.recipe 四个调用方。

#### 4.12.2 步骤

```bash
# 1) OutlineUtil（e4c09fb #75）：
#    util/OutlineUtil.java（434 行）整文件复制；util/client/Line.java（48 行）按 4.12.3 反适配；
#    src/test/.../ShapeUtilJoinTimingTest.java（232 行）复制；
#    build.gradle 集成：注册 runShapeUtilJoinTimingTest（JavaExec）+ check 依赖 +
#    test { failOnNoDiscoveredTests = false } + test sourceSets 配置（按 port 侧旧式 build.gradle 结构）
# 2) HolderGetter 重构（bb09805）：
#    predicate/BlockStatePredicate.java、predicate/ItemIngredientPredicate.java、predicate/ItemPredicate.java：
#    Builder.of(TagKey) → of(HolderGetter, TagKey)；
#    连带 module.recipe：recipe/builder/InWorldRecipeBuilder.java、recipe/predicate/block/HasBlock.java、
#    HasBlockIngredient.java、recipe/predicate/item/HasItem.java 四处调用方同步改
# 3) withCount（d84912e）：ItemIngredientPredicate 新增 withCount(int)
# 4) @EqualsAndHashCode（c480e9f #49）：BlockStatePredicate 加 lombok 注解
```

#### 4.12.3 API 适配点

| 项 | 适配点 |
|---|---|
| `OutlineUtil` | 仅依赖 `net.minecraft.world.phys.AABB/Vec3`（1.21.1 存在，mappings 确认）+ Java 标准库（TreeSet/BitSet/CompletableFuture），**原样复制** |
| `Line.java` | `vertex.setLineWidth(...)` 在 1.21.1 **不存在**（javap 1.21.1 `VertexConsumer`：仅 addVertex/setColor/setNormal 等，该 API 是 1.21.2 渲染管线改造后引入）→ 删除 `.setLineWidth(thickness)` 链式调用（thickness 字段可保留但无效，或改用 `RenderSystem.lineWidth()` 在渲染层设置——1.21.1 线宽是全局状态，无法逐顶点设置）；`PoseStack.Pose` 参数签名与 `pose.pose()`/`pose.last()` 兼容 |
| `ShapeUtilJoinTimingTest` | 测试依赖 port 侧**已有**的 `ShapeUtil.threadedJoin`（两分支 ShapeUtil 方法集一致）、`Shapes/BooleanOp/VoxelShape`（1.21.1 均有）；测试无渲染依赖（只构造 Line 不渲染） |
| HolderGetter 重构 | 1.21.1 `HolderGetter` 有 `getOrThrow(TagKey)`（default 方法）与 `get(TagKey)`（返回 Optional）——`BlockStatePredicate` 用 `blocks.getOrThrow(tag)`，另两个用 `items.get(tag).map(Function.identity())`；`Builder.of(HolderGetter, TagKey)` 与 `items.get(tag).map(...)` 均可行 |
| **不可照搬项** | dev 侧同时删除了 `Builder.with(BlockState)` 方法——**不能照搬**：port 侧 `module.multiblock/.../MultiblockDefinition.java` 大量使用 `.with(state)`（已 grep 确认 8 处以上），dev 侧是配合 26.1 multiblock 重构删的；port 侧保留 `with(BlockState)` |
| `ResourceKey::location` | dev 侧 `ResourceKey::identifier` 为 26.1 改名，反向迁移时保留 port 写法 |
| `withCount` | `public ItemIngredientPredicate withCount(int count) { return new ItemIngredientPredicate(this.items, count, this.components, this.subPredicates); }`（port 侧 4 参构造器） |
| `@EqualsAndHashCode` | lombok 注解，无平台依赖；注意 dev 侧**未**对 `statesCache` 做 `@EqualsAndHashCode.Exclude`，缓存字段参与 equals（dev 侧现状，照搬并留意该语义） |

#### 4.12.4 验收方式

1. `gradlew :module.util:compileJava` 与 `gradlew :module.recipe:compileJava` 通过（连带调用方编译验证）。
2. `gradlew :module.util:runShapeUtilJoinTimingTest` 运行测试通过。
3. 行为验证：`OutlineUtil` 提取 AABB 轮廓正确；`BlockStatePredicate.builder().of(holderLookup, tag)` 与 `.with(state)`（保留项）均正常；`withCount` 返回新实例不改原实例；equals/hashCode 包含属性与缓存字段（语义与 dev 一致）。

---

### 4.13 回移植项 #P13：module.registrum——14 种新注册表 builder/entry（低）

#### 4.13.1 目标

把 dev 侧 module.registrum 的「注册表类型扩展」回移植：14 种新 builder/entry 与 `AbstractRegistrum` 对应入口方法（17 个注册入口，其中 3 个 Modifier 同类），以及两个零碎修复（item 默认 lang、OneTimeEventReceiver 空值防御）。

#### 4.13.2 步骤

```bash
# 1) 提取 builders/ 与 util/entry/ 新增文件 + AbstractRegistrum 入口方法
for f in $(git ls-tree -r --name-only dev/26.1 -- module.registrum | grep -E '(builders|util/entry)/.*\.java$'); do
  git show "dev/26.1:$f" > "$f"
done
# 2) 按 4.13.3 分类处理：直接移植 10 项原样搬 + 反向适配 9 项微调
# 3) 零碎修复：BlockBuilder.item() 默认 lang（e3ba2cd，1 行：return item().defaultLang().build()）、
#    util/OneTimeEventReceiver 空值防御（23f5686：pairs == null return；unregister 判空 bus）
```

#### 4.13.3 分类与 API 适配点

**可直接移植（10 项）**——已核实 1.21.1 API 全部存在：

| 注册类型 | 核实结论 |
|---|---|
| GameEventBuilder/GameEventEntry + `gameEvent()` | 1.21.1 `GameEvent` 同为 record `GameEvent(int notificationRadius)`（mc-1.21.1 源码），`Registries.GAME_EVENT` 存在，零差异 |
| MobEffectBuilder/MobEffectEntry + `mobEffect()` | `Registries.MOB_EFFECT` 存在；builder 仅包 NonNullSupplier + DeferredHolder |
| PotionBuilder/PotionEntry + `potion()` | 1.21.1 存在 `Potion(@Nullable String name, MobEffectInstance... effects)` 构造；`Registries.POTION` 存在 |
| VillagerTypeBuilder + `villager()` / PoiTypeBuilder + `poi()` | `VillagerType::new`（String 构造）与 `Registries.VILLAGER_TYPE` 存在；`PoiType(Set<BlockState>, int, int)` 构造存在（内部经 `PoiStateSet` 包装），`Registries.POINT_OF_INTEREST_TYPE` 存在 |
| SelfBuilder/SelfEntry | 纯泛型包装（ResourceKey + NonNullSupplier + DeferredHolder），无平台 API |
| 三类 Modifier（Biome/GlobalLoot/Structure）+ entries | 21.1.226：`NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS/GLOBAL_LOOT_MODIFIER_SERIALIZERS/STRUCTURE_MODIFIER_SERIALIZERS` 均存在；`BiomeModifier`/`StructureModifier`/`IGlobalLootModifier` 均存在；builder 仅包 MapCodec |
| ConditionBuilder/ConditionEntry + `condition()` | 21.1.226：`NeoForgeRegistries.Keys.CONDITION_CODECS` 与 `ICondition` 存在 |

**需反向适配（9 项）**：

| 注册类型 | 适配点 |
|---|---|
| CreativeTabBuilder + `creativeTab()`（9c89bfa #27） | `CreativeModeTab.Builder` 具备 dev 用到的**全部**方法（title/icon/displayItems/alignedRight/hideTitle/noScrollBar/backgroundTexture/withSearchBar(int)/withScrollBarSpriteLocation/withTabsImage/withLabelColor/withTabFactory/withTabsBefore\|After）；`Item.getDefaultInstance()`、`Util.makeDescriptionId` 存在；仅 `Identifier`→`ResourceLocation`（4 处参数与 2 处 `getResourceKey().identifier()`） |
| AttachmentBuilder/AttachmentEntry + `attachment()`（9627358 #30） | 21.1.226 `AttachmentType.Builder` 有 `serialize(Codec)`/`serialize(Codec, Predicate)`/`copyOnDeath`/`copyHandler`/`sync(...)` 系列；**差异**：dev 的 `serialize(MapCodec<E>)` 在 21.1 是 `serialize(Codec<E>)`（26.1 才改 MapCodec）→ 参数类型改回 Codec |
| DataComponentBuilder/DataComponentEntry + `dataComponent()`（9b2d0e3） | 1.21.1 `DataComponentType.Builder` 有 `persistent(Codec)`/`networkSynchronized(StreamCodec)`/`cacheEncoding()`，**无 `ignoreSwapAnimation()`**（1.21.2+ 才加）→ 删除该方法（其余原样） |
| SoundEventBuilder/SoundEventEntry + `soundEvent()`（c307a4c #28） | `SoundEvent.createVariableRangeEvent(ResourceLocation)`/`createFixedRangeEvent(ResourceLocation, float)` 存在；`Registries.SOUND_EVENT` 存在；仅 `Identifier`→`ResourceLocation` |
| RecipeSerializerBuilder/RecipeTypeBuilder/RecipeEntry + `recipe()`（c307a4c、9627358） | `Registries.RECIPE_SERIALIZER` 与 `Registries.RECIPE_TYPE` 1.21.1 **均存在**；但 `RecipeSerializer` 在 1.21.1 是**接口**（含 `MapCodec<T> codec()` + `StreamCodec<RegistryFriendlyByteBuf, T> streamCodec()`），**不能 `new RecipeSerializer<>(...)`**（26.1 才改成类）→ `RecipeSerializerBuilder.createEntry()` 改为匿名类实现 `codec()`/`streamCodec()`；`RecipeTypeBuilder` 的匿名 `RecipeType<T>{toString}` 写法原样可用 |
| VillagerProfessionBuilder + `profession()`（9b2d0e3） | 1.21.1 `VillagerProfession` 是 **6 元 record**：`(String name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite, ImmutableSet<Item> requestedItems, ImmutableSet<Block> secondaryPoi, @Nullable SoundEvent workSound)`——**没有 tradeSetsByLevel 参数**（`TradeSet` 是 26.1 专有）；dev 的 `Component.translatable(...)` 名字参数 → 1.21.1 第一参数改传 `modid + ":" + name` 字符串；交易注册改走 1.21.1 机制（21.1.226 存在 `VillagerTradesEvent`，或引导用户用 `VillagerTrades.TRADES`） |
| （可选）`dataComponentPredicate()` 入口 | **不可移植**（见第 5 章）：1.21.1 无 `DataComponentPredicate`（1.21.5+ 引入），等价物为 `ItemSubPredicate` + `Registries.ITEM_SUB_PREDICATE_TYPE`；按框架判定不迁移，不提供该入口 |

#### 4.13.4 验收方式

1. `gradlew :module.registrum:compileJava` 通过（17 个入口全部编译可用）。
2. 行为验证（可在 module.test 或调用方验证）：GameEvent/MobEffect/Potion/VillagerType/PoiType 注册后可在 `BuiltInRegistries` 查到；`creativeTab()` 生成标签页；`recipe()` 注册的 RecipeSerializer 可被配方加载；`attachment()` 的附件类型可附加与同步。
3. 破坏性核对：`AttachmentBuilder.serialize` 的 Codec 参数与 `DataComponentBuilder` 无 `ignoreSwapAnimation` 的签名差异在 javadoc 中注明。

---

### 4.14 回移植项 #P14：module.registrum datagen 增强（中）

#### 4.14.1 目标

把 dev 侧 module.registrum 的 datagen 增强回移植（**不包含** 26.1 新模型生成器与 Runner 模式）：`RegistrumRecipeProvider` 的 vanilla 方法 public 化、`Builder.dataMap(..., HolderLookup.Provider)` 重载；`GeneratorType` 拆分列为可选单独提交。

#### 4.14.2 步骤

```bash
# 1) RecipeProvider 方法 public 化（58f01d4 及 ea4468a/d4191a0/e07aaa5/c1b10b8/b8825bd）：
#    cherry-pick dev 版 RegistrumRecipeProvider 的公开方法清单到 port 版文件（port 版已有
#    cooking/smelting/stonecutting/storage 等主体，实际增量是 vanilla 方法 public 化 +
#    shelf/colorItemWithDye 等）；port 版原地增强，不新建 Runner
# 2) dataMap(provider) 重载（58f01d4）：builders/Builder.java 新增带 HolderLookup.Provider 的 dataMap 重载
#    （port 已有 dataMap(type, val) 与 dataMap(type, factory)）；
#    providers/RegistrumDataMapProvider.java 按 4.14.3 捕获 provider
# 3) （可选）GeneratorType/ProviderType 拆分（58f01d4）：单独提交，见 4.14.3
```

#### 4.14.3 API 适配点

| 项 | 适配点 |
|---|---|
| RecipeProvider 方法 public 化 | 1.21.1 这些方法均为 **`protected static` 且首参为 `RecipeOutput`**（如 `oneToOneConversionRecipe(RecipeOutput, ...)`、`trimSmithing(RecipeOutput, Item, ResourceLocation)` 在 1.21.1 存在）——1.21.1 落法是「公开包装方法内调用 `RecipeProvider.xxx(this, ...)`」而非 override（26.1 才可 override）；**需剔除**：`shaped(RecipeCategory, ItemStackTemplate)`/`shapeless(RecipeCategory, ItemStackTemplate)`（ItemStackTemplate 26.1 专有，1.21.1 用 ItemLike 重载）；`generateForEnabledBlockFamilies(FeatureFlagSet)` 的 26.1 签名（1.21.1 为 `(RecipeOutput, FeatureFlagSet)`，port 已 override 旧签名）；`safeKey(...)` + `save(this, ResourceKey)` 需改为 `ResourceLocation`（已核实 1.21.1 `RecipeBuilder.save(RecipeOutput, ResourceLocation)` 存在、**无 ResourceKey 重载**） |
| `dataMap(provider)` | dev 侧靠 26.1 `DataMapProvider.gather(HolderLookup.Provider)` 重载捕获 provider；1.21.1 的 `gather()` 无参数（port 已用 `@SuppressWarnings("removal")` 标记）→ 等价方式：在 `RegistrumDataMapProvider` 里 override `run(CachedOutput, HolderLookup.Provider)` 缓存 provider 再调 super（datagen 线程内安全），或直接使用构造器传入的 lookup future；`DataMapType` 在 21.1.226 存在（数据映射 1.21.1 引入） |
| GeneratorType 拆分（可选） | `GeneratorType` 接口本身无平台依赖，但拆分动机是支撑 26.1 新模型生成器（MODEL/BLOCKSTATE/ITEM_MODEL generator 装配）与 `RegistrumRecipeRunner`，**这两个消费方在 1.21.1 均不存在**；若同步迁移需连带改 `AbstractRegistrum.genData/setDataGenerator/addDataGenerator`、`RegistrumDataProvider`（新增 `subGenerators` 与 `putSubProvider`）、`Builder.setData/addMiscData` 签名（ProviderType→GeneratorType），并同步放弃「sides 过滤」（dev 注释掉 `includeServer/includeClient` 过滤是行为变更，1.21.1 需自行决策是否保留）——**建议低优先级、单列一次提交** |
| RegistrumRecipeRunner / 模型生成器五件套 | **不迁移**：`RecipeProvider.Runner` 1.21.1 不存在（26.1 才引入 runner 模式）；模型生成器依赖 26.1 新模型 datagen（`net.minecraft.client.data.models.*`、`net.minecraft.client.renderer.block.dispatch.*`、NeoForge 26.1 `template.*`，且 `RegistrumBlockModelGenerator` 用 `ObfuscationReflectionHelper` 反射改写 26.1 字段）——port 保留旧体系 `RegistrumBlockstateProvider`/`RegistrumItemModelProvider`（功能等价） |

#### 4.14.4 验收方式

1. `gradlew :module.registrum:compileJava` 通过；`gradlew :module.registrum:runData` 生成产物核对（配方/数据映射/provider 正常输出）。
2. 行为验证：公开包装方法可从外部模块调用生成配方（如 `shelf`/`colorItemWithDye`）；`dataMap(type, holderLookup, factory)` 在 datagen 时正确捕获 `HolderLookup.Provider`。
3. 若做 GeneratorType 拆分：`gradlew :module.registrum:compileJava` + `runData` 回归核对（sides 过滤决策记录在 commit 说明中）。

---

### 4.15 回移植项 #P15：module.multiblock——事件化 + 快照复用 + BlockPos 键化 + 懒解析（中）

#### 4.15.1 目标

把 dev 侧 module.multiblock 的四项功能演进/修复回移植：`DynamicMultiblockEvent` 成型/解散可取消事件、未加载区块快照复用、控制器位置 Long→BlockPos 键化、定义懒解析（ResourceKey + registryAccess）。**注意**：dev 侧大部分 multiblock 代码源自 1.21.1 移植（`7019b38`），真正增量即上述四项（M1–M4，M5/M6 可选注解项）。

#### 4.15.2 步骤

```bash
# M1 DynamicMultiblockEvent（1e5bfb3 #69）：
#   dynamic/event/DynamicMultiblockEvent.java 新建（原样复制）；
#   DynamicMultiblockManager.updateFormed、MultiblockFormPacket、MultiblockUnformPacket 各加事件触发点：
#   服务端 updateFormed 中取消 → 回滚 setFormed；客户端两个 packet handler 中取消 → 同步回滚
# M2 未加载区块快照复用（d888714 #73）：
#   DynamicMultiblockManager.buildSnapshot 加 if (!level.isLoaded(pos)) 分支：复用 old.entries()
#   或填 (null, null, predicate)；MultiblockCheckSnapshot.Entry.blockState 可空 + Entry.test() 空态返回 true；
#   MultiblockState 新增 snapshot 字段（构造时初始化 new MultiblockCheckSnapshot(this.controllerPos, Map.of())）；
#   checkMultiblockFormed 中 mstate.setSnapshot(snapshot) 先存快照
# M3 Long→BlockPos 键化（22091d4）：
#   DynamicMultiblockManager：Map<Long,…>→Map<BlockPos,…>、pendingChecks 同理（约 20 处 asLong()/immutable() 调整）；
#   MultiblockCheckSnapshot record 字段 controllerPosLong → BlockPos controllerPos；
#   注意 getAt/add/removeAt/containsAt 与异步回调处的 pos.immutable() 防哈希突变（BlockPos 可变）
# M4 定义懒解析（1b0a652）：
#   MultiblockState 构造器改收 ResourceKey，持 @Nullable Holder.Reference，
#   getDefinition(HolderLookup.Provider) 懒解析；DEFINITION_KEY_CODEC / DEFINITION_KEY_STREAM_CODEC 外提；
#   MultiblockFormPacket / MultiblockUnformPacket / DynamicMultiblockManager.add/onPlace（dev 用 holder.key()）同步；
#   STREAM_CODEC：ByteBufCodecs.holder(...) → ResourceKey.streamCodec(...)（两者 1.21.1 都有）
# M5（可选）ApiStatus.Internal（c72aa30 #93）：multiblock 6 个类逐行加注
# M6（可选）package-info @NullMarked（3bf9b6c）：multiblock 10 个包（jspecify 在 1.21.1 构建链可用，
#   port module.test 已用 org.jspecify.annotations.NullMarked，且 gradle 缓存存在 org.jspecify/jspecify）
```

#### 4.15.3 API 适配点

| 项 | 适配点 |
|---|---|
| 事件体系 | `net.neoforged.bus.api.Event`、`ICancellableEvent`、`NeoForge.EVENT_BUS.post()` 全套 21.1.226 自带（EventBus 体系 1.20.5 起未变） |
| M1 平台写法 | dev `this.state.getDefinitionKey().identifier()` → port `location()`；dev `level.isClientSide()` → port `isClientSide` 字段 |
| M2 | `Level.isLoaded(BlockPos)` 是 1.21.1 标准 vanilla API；`Map.of()`/`LinkedHashMap` 纯 JDK |
| M4 | `ResourceKey.streamCodec(ResourceKey)`（port `module.codec/StreamCodecUtil.java:264` 已在用）、`HolderLookup.Provider.lookup(...).getOrThrow(...)`、`Level.registryAccess()`（port `SetBlock.java:81` 已用）、`Holder.Reference`——全部 1.21.1 存在 |
| Java 21 陷阱 | dev 使用了 `computeIfAbsent(level, _ -> ...)`——`_` 作为 lambda 参数名需 Java 22+，port 工具链是 Java 21，**必须改名**（如 `ignored`） |
| M7 Config.group（跨模块） | `@Config(group=…)` 离开 module.config 的 group 支持无法编译——**依赖 #P9**；#P9 落地后两个模块各改 1 行注解（`AnvilLibMultiblockConfig`/`AnvilLibRecipeConfig`），config 文件路径变化（`group/xxx-common.toml`）；若 #P9 未先行，两个模块维持 `@Config(name=…)` 即可编译 |

#### 4.15.4 验收方式

1. `gradlew :module.multiblock:compileJava` 与 `gradlew :module.recipe:compileJava` 通过（连带验证）。
2. 行为验证（运行 `runServer` 或测试环境）：成形/解散事件可取消（取消后回滚）；未加载区块处快照复用不触发强加载；`Map<BlockPos,…>` 键化后 getAt/add/removeAt/containsAt 正确（异步回调防哈希突变）；定义经 `HolderLookup.Provider` 懒解析正确。
3. `grep -rn "asLong()\|controllerPosLong" module.multiblock/src` 复核零残留（预期差异除外）。

---

### 4.16 回移植项 #P16：module.recipe——SpawnItem 守卫 + SetBlock nbt 默认值（低）

#### 4.16.1 目标

把 dev 侧 module.recipe 的两处纯逻辑修复回移植：`SpawnItem.accept()` 零数量守卫、`SetBlock` nbt 编解码默认值 null→空 `CompoundTag`（修复潜在 NPE）。

#### 4.16.2 步骤

```bash
# R1 SpawnItem 零数量守卫（40b8f84）：
#   outcome/SpawnItem.java 的 accept()：int count = context.getInt(...); if (count == 0) return;
#   （port 当前 SpawnItem.java:107 直接 copyWithCount(...) 无守卫，随机 count 掷 0 时仍会生成空堆物品实体）
# R2（可选）SpawnItem Builder 必填校验（91327c1）：Builder.item 字段 ItemStack.EMPTY → @Nullable，
#   build() 里 Objects.requireNonNull（1.21.1 用 @Nullable ItemStack 等价实现即可，不必引入 ItemStackTemplate）
# R3 SetBlock nbt 默认值（b8825bd）：
#   outcome/SetBlock.java 的 optionalFieldOf("nbt", null) → optionalFieldOf("nbt", new CompoundTag())
#   （port SetBlock.java:100 仍为 null，而 accept() 直接 entity.loadWithComponents(this.nbt, ...)——
#   JSON 缺 nbt 字段时 1.21.1 的 loadWithComponents(null, ...) 会 NPE；改默认值即可，无需动 loadWithComponents 调用）
# R4（可选）InWorldRecipe.matches() 谓词栈清理简化（0d0649f）：行为等价重构，风险收益比低，可缓做
```

#### 4.16.3 API 适配点

- 纯逻辑改动，无 API 适配；port 用 `this.item.copyWithCount(count)`，与 `ItemStackTemplate` 无关。
- **已由 port 平行 PR 接收、无需重复移植**：双半方块修复、`AbstractCacheElement.grow()` 空堆修复、`ICacheInputOutputImpl.grow()` 只记录实际消耗、`ItemCache` 方块实体元素范围 `0.5→1.0`（port `6a0e8a9` #79 / `f2b60b6`，对应 dev `922d523` #78 / `1c523ee`）。

#### 4.16.4 验收方式

1. `gradlew :module.recipe:compileJava` 通过。
2. 行为验证：随机 count 掷 0 时 `SpawnItem` 不生成物品实体；`SetBlock` 在 JSON 缺 `nbt` 字段时正常放置（无 NPE）。

---

### 4.17 回移植项 #P17：module.wheel——环形扇区选择效果（中-高）

#### 4.17.1 目标

把 dev 侧 module.wheel 唯一的真功能——环形扇区选择效果（`WheelSelectionEffect`，DOT/ANNULAR_SECTOR 两种形态 + 可配置颜色 + 删除固定 `selectionEffectRadius` 参数）回移植；**渲染必须落在 port 侧既有 Tesselator / `ShaderInstance` / `LibShaders` 体系上，不带入任何 RenderState/UBO 管线**。

#### 4.17.2 步骤

```bash
# W1 WheelSelectionEffect（5d0b285、46e1b73、8a818f0 累计）：
#   api/WheelSelectionEffect.java 新建（纯逻辑部分原样搬）；
#   api/WheelMenuBuilder.java（链式 selectionEffect()/selectionEffectColor()）、
#   api/WheelMenuModel.java（of(...) 新重载）、
#   gui/component/WheelWidget.java（renderSelectionEffect）、
#   gui/screen/WheelScreen.java 按 4.17.3 适配
# W2 annular_sector.fsh（5d0b285）：assets/anvillib/shaders/core/annular_sector.fsh 落位，
#   片段逻辑（径向+角度 AA、atan 扇区裁剪）GLSL 150 通用；
#   26.1 的 layout(std140) uniform AnnularSectorUniform（UBO）改为仿 port 现存 ring.json/selection.json 注册方式
#   + safeGetUniform("Center") 逐个 set（port 侧 WheelWidget.renderRing 即此模式）
# W3 环/选择点渲染（46e1b73、494274e 上游）：
#   WheelWidget.renderRing/renderSelectionEffect/renderProgressAnimation 功能（环、选中点随进度缩放、
#   getRendererSize 动态尺寸）移植；port 侧保留 renderRingFallback/renderDisc 式多边形回退与旧 shader 双路径
# W7（可选）@ApiStatus.Internal（c72aa30）：AnvilLibWheel、WheelWidget 等
```

#### 4.17.3 API 适配点

| 项 | 适配点 |
|---|---|
| 纯逻辑（可直接搬） | `normalizePositiveAngle()` 角度归一化、扇区范围 `(angleEnd-angleStart)/2`、`WheelMenuModel.of(...)` 新重载、builder 链式 `selectionEffect()`/`selectionEffectColor()`——纯 Java |
| 渲染实现（重写） | dev 依赖 `SdfGraphics`（module.rendering）、`GuiGraphicsExtractor`、`Matrix3x2fStack`、`AnnularSectorRenderState`（`RenderPipeline`+`GpuBufferSlice`）、`LibDynamicUniforms`（`DynamicUniformStorage`/`Std140Builder`）——全部 26.1 → 1.21.1 落点为 port 侧 `WheelWidget` 原 Tesselator / `ShaderInstance`+`LibShaders` 体系（现仍存在），按 port 旧式重写扇区绘制 |
| 输入/渲染签名 | dev `WheelScreen`（`KeyEvent`/`MouseButtonEvent`/`extractRenderState`）、`WheelEntryRenderer`（`GuiGraphicsExtractor`+`Matrix3x2fStack`）→ 1.21.1 对应 `keyPressed(int,int,int)`/`mouseClicked(double,double,int)`/`GuiGraphics`/`PoseStack`——port 侧现存代码即等价物，不搬 dev 写法 |
| 模块依赖 | dev 侧 module.wheel 的 build.gradle 已 `jarJar` 依赖 `anvillib-rendering-26.1`（dev 侧 wheel 的新增模块依赖）——**回移植时应切断**，不引入 rendering 依赖 |

**不迁移（仅 26.1 平台）**：`gui/render/state/{AnnularSector,Ring,Selection}RenderState.java`、`client/init/LibRenders.java`、`client/init/LibDynamicUniforms.java`（三者直接实现 `dev.anvilcraft.lib.v2.rendering.state.LibQuadGuiElementRenderState`、引用 `RenderPipeline`/`GpuBufferSlice`/`DynamicUniformStorage`；port 无 module.rendering 目录），dev 侧删除的 `LibShaders.java` port 保留。

#### 4.17.4 验收方式

1. `gradlew :module.wheel:compileJava` 通过。
2. 行为验证（运行 `runClient`）：环形菜单打开后显示扇区选择效果（DOT 点状 / ANNULAR_SECTOR 环形扇区两种形态）；选中项随进度缩放动画正确；颜色配置（`selectionEffectColor`）生效；滚轮/按键交互无回归（port 旧渲染路径仍正常）。

---

### 4.18 回移植项 #P18：module.test——可选集成测试（低-中）

#### 4.18.1 目标

把 dev 侧 module.test 中**不依赖 dev 新渲染模块**的四组集成测试回移植：T2 配置类测试、T8 RPC 测试、T9 爆炸测试、T10 wheel 按键扩展。其余测试（Bloom/CachedBER/Compute/SDF 图层/GUI 测试）依赖 module.rendering 与 26.1 渲染管线，不迁移（见第 5 章）。

#### 4.18.2 步骤

```bash
# T2 配置类测试（a6a24f1）：AnvilLibTestConfig.java + ConfigManager.register
#   ——port module.config 已有 ConfigManager/Config/Comment/CollapsibleObject/ConfigData；
#   缺 TranslatableEnum → 随 #P9 移植后保留 TestEnum 翻译，否则去掉 TestEnum 翻译部分
# T8 RPC 测试（d153e58、2754d18）：rpc/{RpcClientTest,RpcIntegrationTest,TestRpcMethods,RpcTestCommands,RpcTestScheduler}
#   ——依赖 module.rpc（#P1）；@EventBusSubscriber(Dist.CLIENT) 无 modid 为 26.1 语法，1.21.1 需补 modid
# T9 爆炸测试（435e23f）：command/TestCommand.java ——依赖 module.explosion（#P3），命令骨架为 1.21.1 通用 API
# T10 wheel 测试扩展（随 W1）：wheel/{WheelTestClientHandler,WheelTestKeys,WheelDemoMenus}
#   ——KeyMapping.Category + event.registerCategory() 为 26.1 新 API；port 1.21.1 为
#   String category 构造 + RegisterKeyMappingsEvent；KeyMapping.matches(KeyEvent) 换 matches(key, scanCode)
# T13（可选）构建形式：module.test 的 runtimeOnly dev.dubhe:anvilcraft-neoforge-26.1.2
#   换 1.21.1 版坐标或删除（dev 平台调试依赖）
```

#### 4.18.3 API 适配点

- T8 测试代码所用 MC/NF API 1.21.1 全部存在（`ClientPlayerNetworkEvent.LoggingIn`、`RegisterCommandsEvent`、`ServerTickEvent`、`PlayerEvent.PlayerLoggedInEvent`、`StreamCodec`/`ByteBufCodecs`）；依赖 `RPC`/`RpcTarget`/`RemoteCallable`/`CallableParam`/`IRemoteCallableValidator`（#P1）。
- T9 爆炸功能逻辑（分 tick 破坏、概率半径）与平台无关；`ExplosionExecutor` 属 #P3。
- T10 按键 API 反向替换见上表。

#### 4.18.4 验收方式

1. `gradlew :module.test:compileJava` 通过。
2. 运行验证（`runClient`/`runServer` 或 gameTest）：T2 配置加载与枚举翻译正确；T8 RPC 远程调用往返成功；T9 爆炸命令执行分 tick 破坏正确；T10 wheel 环形按键切换与 demo 菜单正常。

---

### 4.19 回移植项 #P19：构建体系——roseauCheck（中）

#### 4.19.1 目标

把 dev 侧构建体系中最具迁移价值的两项回移植：**roseauCheck API 兼容检查**（核心项，可直接搬）；**module.gradle 集中化**（可选工程决策，需裁剪 26.1 特有内容）。

#### 4.19.2 步骤

```bash
# 1) roseauCheck（a6a24f1、946938f）：根 roseau.yaml + gradle/scripts/roseau.gradle 拷贝，
#    根 build.gradle 接入 apply from + 任务注册
# 2)（可选）module.gradle 集中化（ea4301f #25）：根 module.gradle + build.gradle 的 createModule 脚手架 +
#    各模块 build.gradle 精简为 dependencies 块 + gradle.properties 的 anvillib.needRunConfig* 属性——
#    port 侧现有独立构建脚本自洽，是否重构由维护者决定；若做，按 4.19.3 裁剪
```

#### 4.19.3 API 适配点（若做集中化，必须裁剪的 26.1 特有内容）

| dev 侧内容 | port 处理 |
|---|---|
| `clientIrisWorkaround`/`clientRenderDoc` run 配置 | 剔除（port 无 module.gradle 的 RenderDoc 接线，见 #P8） |
| `evaluationDependsOn(':renderdoc-loader-internal')` | 按需保留/剔除（若 #P8 落地为独立模块可保留） |
| `createModule` 模板生成 `Identifier` 代码 | port 应生成 `ResourceLocation` |
| `java_version=25` | port 为 21 |
| parchment 配置已移除 | port **需要**（保留 port 现有 parchment 配置） |
| roseau 配置 | 通用（`io.github.alien-tools:roseau-cli:0.6.0` 无 MC 绑定，直接搬） |

**不做**：`changeModsToml` 依赖注入任务（port 无 `.github/modules.json`，需补建该清单或裁剪任务）；版本目录插件化与 `jarJar api→implementation`（低价值工程改动，由维护者决定）。

#### 4.19.4 验收方式

1. `gradlew roseauCheck`（或对应任务）在 port/1.21.1 基线上运行通过，输出 API 差分报告（可对 1.21.1 基线跑差分）。
2. 若做集中化：全量 `gradlew build` 通过（全部模块聚合构建无回归）。

---

## 5. 不可移植清单（明确不迁移的平台适配项）

> 以下内容**明确不迁移**。判定依据见第 2 章分类规则：这些「功能」本身就是 26.1 平台适配，port 侧已有 1.21.1 等价物，反向迁移无意义且会破坏 port 侧现状。执行时不得以「顺手」「保险」为由扩大范围。按模块分节，每项一句话理由。

### 5.1 module.rendering（26.1 渲染架构本体）

| 不迁移内容 | 理由（一句话） |
|---|---|
| `ALRPipelines.java`（`RenderPipeline.builder()`、`UniformType.UNIFORM_BUFFER`）与 `RegisterRenderPipelinesEvent` 注册 | `RenderPipeline` 体系与 `RegisterRenderPipelinesEvent` 均为 26.1 专属（21.1.226 事件列表核实无此事件）；1.21.1 等价物是 `RegisterShadersEvent`（port 侧已用），其 vertex format 声明以 1.21.1 `VertexFormat` 构造器在 #P7 中重建 |
| `mixins/GuiRendererMixin.java`（SDF 注入部分） | 目标类 `GuiRenderer`/`GuiElementRenderState` 1.21.1 不存在（26.1 GUI 渲染状态体系）；SDF 绘制经 #P7c 改走 `GuiGraphics` |
| `extension/GuiGraphicsExtractorExtension.java` + `GuiGraphicsExtractorMixin` + `ItemStackRenderStateMixin` + `ItemStackRenderStateInternals` | 目标类 `GuiGraphicsExtractor`/`ItemStackRenderState` 为 26.1 对象；其功能（半透明物品/方块/结构预览）并入 #P7c 的 GUI 增强，按 1.21.1 `GuiGraphics + MultiBufferSource` 重写 |
| `foundation/compound/CompoundSubmitNodeStorage`、`CompoundSubmitNodeCollection`、`foundation/BloomSubmitNodeStorage` | 父类 `SubmitNodeStorage`/`SubmitNodeCollection` 与 `SubmitNodeCollector`/`FeatureRenderDispatcher` 体系为 26.1 渲染架构核心，1.21.1 不存在（port 分支全库无 `SubmitNode` 引用）；bloom 双画在 #P7b 的 BufferSource 重画方案中直接实现 |
| `extension/blaze3d/compute/**` 的 6 个 blaze3d mixin（`GpuDevice`/`CommandEncoder`/`GlDevice`/`GlCommandEncoder`/`DirectStateAccess`/`GlDebugLabel`） | mixin 目标类 1.21.1 不存在（无 GPU 抽象层）；compute 能力经 #P7e 以自管 GL 工具层重建 |
| `state/LibGuiElementRenderState`、`LibQuadGuiElementRenderState` 及 `gui/state/` 系列状态类 | 26.1 GUI 渲染状态体系（`GuiElementRenderState`/`PictureInPictureRenderState` 接口），1.21.1 无对应物 |
| `foundation/fakeworld` 中的 `cardinalLighting()` 相关实现 | `CardinalLighting` 是 26.1 新接口方法，1.21.1 的 `BlockAndTintGetter` 无此方法（1.21.1 用 `getShade(Direction, boolean)` 体系）；`FakeDisplayLevel` 抽象方法集按 1.21.1 补齐（#P7 相关但不单独立项，随 fakeworld 反向适配处理） |

### 5.2 module.font

| 不迁移内容 | 理由 |
|---|---|
| 无独立不可移植项 | font 模块「需反向适配」项（GPU 上传/绘制层、GUI 屏幕）均在 #P6 中给出 1.21.1 改写方案，无仅 26.1 平台的内容 |

### 5.3 module.rpc / module.sync

| 不迁移内容 | 理由 |
|---|---|
| `ItemStackTemplate.STREAM_CODEC` 编解码分支（SyncProxy.defaultCodec） | `ItemStackTemplate` 是 26.1 ItemStack 重构专有类型，1.21.1 无对应物（javap「找不到类」）；1.21.1 的 `ItemStack` 已有 `OPTIONAL_STREAM_CODEC`，删除后功能无损 |
| `neoforgespi.transformation.ClassProcessor` SPI（processor 子模块的注册面） | 该 SPI 属 26.1 平台特性（loader 4.0.42/4.0.43 无 transformation 子包，loader 11.0.13 才有 40 个类）；processor 功能经 #P2 子项 2.6 以 CoreMod（`ICoreMod` + `ITransformer<ClassNode>`）重写 |

### 5.4 module.explosion / module.collision / module.space-select / renderdoc-loader

| 不迁移内容 | 理由 |
|---|---|
| 无独立不可移植项 | 四模块功能依赖的 26.1 API 面很窄（`Identifier`、`ItemStackTemplate`、`hurtServer`、`Ingredient.items()`、`Registry.get(TagKey)`、`Inventory.getSelectedSlot()`、`ClientPacketDistributor`、`Minecraft.hasControlDown`、`Direction.getApproximateNearest`、`ARGB`、`GatherDataEvent.Client`、渲染状态体系），每一项在 1.21.1 都有直接等价物（javap 逐一核实），已并入 #P3/#P5/#P8 的适配点 |

### 5.5 module.config / module.integration / module.network

| 不迁移内容 | 理由 |
|---|---|
| package-info 全面切换 `@NullMarked`（jspecify） | jspecify 是 26.1 MC 的新依赖（21.1.226 sources jar 中无 `org/jspecify`）；1.21.1 标准写法是 `@MethodsReturnNonnullByDefault` + `javax.annotation.ParametersAreNonnullByDefault`（port 现状），纯风格迁移无功能价值 |
| `IPacket.type(ResourceLocation→Identifier)` 改名 | `Identifier` 是 26.1 MC 的类名变更；1.21.1 为 `net.minecraft.resources.ResourceLocation`（port 现状） |
| `FMLLoader.getCurrent()` 系用法 | 1.21.1 的 `FMLLoader` 无 `getCurrent()`（loader 4.0.42 sources 核实），port 侧 `FMLLoader.getDist()`/`LoadingModList.get()` 写法正确，不随 dev 改动 |
| BIDIRECTIONAL 包注册的 4 参数 handler | 21.1.226 `PayloadRegistrar` 三个 bidirectional 方法均为 3 参数（26.1 才引入双 handler 签名）；port 侧 3 参调用保持 |

### 5.6 module.util

| 不迁移内容 | 理由 |
|---|---|
| `ClientTickRecorder` 修复（`6b4d6bd` #61，删除 level 判空与 LoggingOut 归零） | dev 的修复针对 26.1 事件触发时机变化（主菜单 tick 行为）；1.21.1 的 `ClientTickEvent.Pre` 行为与 port 现状一致，照搬会破坏计时器正确性（反向移植引入 bug） |
| `ChanceItemStack`/`ChanceBlockState`/`WeightedChanceBlockStates`/`NbtPredicate` 的 26.1 版 | `ItemStackTemplate` 化、`ContextMap/ContextKeySet` LootParams 构造、`TagValueOutput`+`ProblemReporter`——均为 26.1 专属；port 侧 ItemStack 版/LootParams 旧构造器实现完整可用 |
| package-info `@NullMarked` 迁移 | 同 5.5，注解风格迁移无功能价值 |

### 5.7 module.registrum

| 不迁移内容 | 理由 |
|---|---|
| `DataComponentPredicateBuilder`/`DataComponentPredicateEntry` + `dataComponentPredicate()` 入口（`252922e` #47） | `DataComponentPredicate` 与 `Registries.DATA_COMPONENT_PREDICATE_TYPE` 1.21.1 不存在（1.21.5+ 才引入）；1.21.1 等价物是 `ItemSubPredicate` + `Registries.ITEM_SUB_PREDICATE_TYPE`（`d6fb290` 即 dev 侧从 ItemSubPredicate 换成 DataComponentPredicate 的迁移记录）——按框架判定不迁移 |
| generators/ 模型生成器五件套（`RegistrumModelProvider`/`RegistrumBlockModelGenerator`/`RegistrumItemModelGenerator`/`RegistrumLegacyBlockModelBuilder`/`PropertyDispatchWrap`） | 全部依赖 26.1 新模型 datagen（`net.minecraft.client.data.models.*`、`net.minecraft.client.renderer.block.dispatch.*`、NeoForge 26.1 `template.*`），且 `RegistrumBlockModelGenerator` 用 `ObfuscationReflectionHelper` 反射改写 26.1 字段；1.21.1 仍是旧体系 `net.neoforged.neoforge.client.model.generators.*`，port 现有 `RegistrumBlockstateProvider`/`RegistrumItemModelProvider` 功能等价 |
| `RegistrumRecipeRunner` 与 `generators/RegistrumRecipeProvider` 的 Runner 形态 | `RecipeProvider.Runner` 1.21.1 不存在（26.1 才引入 runner 模式）；port 沿用现有「extends RecipeProvider implements RecipeOutput」模式（#P14 只做方法增强） |
| 平台适配 diff（Identifier/jspecify/`FMLLoader.getCurrent()`/`EntitySpawnReason`/`BlockTintSource`/渲染器双泛型/loot 基类更换/TagsProvider 去 ExistingFileHelper/`GatherDataEvent.Client`/`Registration` static 化等） | 均为 26.1 平台适配（见报告 §4 清单），port 侧现状即 1.21.1 正确写法 |
| `RegistrumDataProvider` 「忽略 sides 过滤，全部生成」行为变更 | 与 26.1 `GatherDataEvent.Client` 拆分相关，1.21.1 仍是一个 `GatherDataEvent`；建议保留 port 现有过滤（#P14 中不迁移此行为变更） |

### 5.8 module.multiblock / module.recipe

| 不迁移内容 | 理由 |
|---|---|
| 持久化重写：`SavedData.load/save(NBT)` → `SavedDataType` + `CODEC` | `SavedDataType` 是 26.1 新 API；1.21.1 用 `SavedData.Factory`（port 现有实现保留）；dev 新 CODEC 只持久化 `controller_pos`+`definition`（丢弃 `formed`）属平台重构副作用，不回移植 |
| `BreakBlockEvent` | 26.1 把 `BlockEvent.BreakEvent` 拆为 `net.neoforged.neoforge.event.level.block.BreakBlockEvent`；1.21.1 用原类（port 现状正确） |
| `MultiblockDefinition` Builder 12 个重载删除 | 因 dev 的 `module.util/BlockStatePredicate` 移除了 `with(BlockState)` 与 `of(TagKey)`（26.1 标签/方块状态 Holder 化）；port 的 `BlockStatePredicate` 两者俱在，重载保留（见 #P12「不可照搬项」） |
| `RecipeMapMixin` + `IRecipeMapExtension` | `RecipeMap` 是 1.21.2+ 新类；1.21.1 `RecipeManager` 仍是 `byName`/`byType` 字段，port `RecipeManagerMixin` 已含 dev 拆分前的完整实现 |
| `ItemResourceHandlerCache(Element)` + `VanillaContainerWrapper` | `net.neoforged.neoforge.transfer.ResourceHandler/ItemResource` 是 26.1 transfer API；1.21.1 等价物是 `IItemHandler`（21.1.226 自带）+ `InvWrapper`，port 的 `IItemHandlerCache`/`ItemHandlerCacheElement` 保留 |
| `LibDataComponentPredicates` → `LibItemSubPredicates` | `DataComponentPredicate.Type` 注册表是 26.1 特性；1.21.1 用 `ItemSubPredicate.Type`（`BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE`），And/Or/Not 组合谓词两侧等价，port 保留 |
| `ValueInput`/`ValueOutput`/`ProblemReporter`/`TagValueInput`（BlockCache/SetBlock 的 NBT 加载错误上报） | 26.1 专属；1.21.1 用 `loadWithComponents(CompoundTag, Provider)` |
| Recipe 接口新方法（`placementInfo()`/`recipeBookCategory()`/`showNotification()`/`group()`、Serializer 脱接口化）、`serverLevel.recipeAccess()`、`RecipeUnlockedTrigger` 包迁移、`ContextMap/ContextKeySet` LootParams、`ItemStackTemplate`（icon/SpawnItem/Builder/SaveComponentToTag） | 26.1 Recipe/注册表体系变化，1.21.1 用 `canCraftInDimensions/getResultItem/RecipeSerializer` 等旧接口，port 现状即正确 |
| `ResourceEventListener` 注释掉 `RecipesUpdatedEvent` 客户端处理器 | 26.1 客户端配方管理重构；port 保留该处理器是正确行为 |
| `IRecipeManagerExtension.anvillib$getRegistries` @Deprecated | 26.1 适配 |
| `ASYNC_MULTIBLOCK_CHECK_PLAN.md` 删除 | 文档，不回移植（port 现状可用） |
| 语言文件管理差异（dev 删除 generated en_us.json 跟踪、zh_cn.json） | 资源管理差异，port 现状可用，不回移植 |

### 5.9 module.moveable-entity-block（整体排除）

| 不迁移内容 | 理由 |
|---|---|
| 模块全部（F1–F5） | dev 侧零功能增量，全部为 26.1 平台适配（ValueIO 序列化重构、RenderState 渲染管线重构、反混淆名调整）；活塞 bug 修复已在 port（`6a0e8a9` #79）；注解为纯风格——详见 2.3 节 |

### 5.10 module.wheel

| 不迁移内容 | 理由 |
|---|---|
| `gui/render/state/{AnnularSector,Ring,Selection}RenderState.java`、`client/init/LibRenders.java`、`client/init/LibDynamicUniforms.java` | 三者直接实现 `dev.anvilcraft.lib.v2.rendering.state.LibQuadGuiElementRenderState`（module.rendering）、引用 `RenderPipeline`/`GpuBufferSlice`/`DynamicUniformStorage`；port 无 module.rendering 目录，1.21.1 无对应物（见 #P17） |
| 输入/渲染签名 26.1 化（`WheelScreen` 的 `KeyEvent`/`MouseButtonEvent`/`extractRenderState`、`WheelEntryRenderer` 的 `GuiGraphicsExtractor`+`Matrix3x2fStack`） | 1.21.1 对应 `keyPressed(int,int,int)`/`mouseClicked(double,double,int)`/`GuiGraphics`/`PoseStack`，port 侧现存代码即等价物 |
| dev 侧删除的 `LibShaders.java` | port 保留（环形扇区渲染落点依赖它） |

### 5.11 module.test

| 不迁移内容 | 理由 |
|---|---|
| T1 测试组织重构（`all/` 包 + `TestLangGenerator`） | 依赖 dev 侧 registrum 演进 API（entry 包 + `PropertyDispatchWrap`），datagen 输出含 26.1 MC 类；port 保留现有结构 |
| T3 Bloom 测试（`TestBloomBlock/Tile/TESR`） | 依赖 `ALRPostEffects`/`BloomPostEffect`/`CompoundSubmitNodeStorage`（module.rendering）+ 26.1 渲染管线 + `SyncProxy`；等于重写 |
| T4 缓存型 BER 测试（`TestCachedRendering*`） | 依赖 `CachedBlockEntityRenderingPipeline` 等 + `BlockModelRenderState`/`BlockDisplayContext`（26.1 渲染） |
| T5 Compute 测试（`ComputeSupport`/`TestPipelines` + 4 个 .csh） | 依赖 `GpuDevice`/`CommandEncoder`/`GpuFence`/`GpuSampler`/`GpuTexture`/`TextureFormat`（26.1 GPU 抽象，1.21.1 无） |
| T6 SDF 图层测试（`SdfGraphicsLayer`） | `SdfGraphics`（26.1 管线）+ `implements GuiLayer` 为 26.1 NeoForge 接口（21.1.226 的 `RegisterGuiLayersEvent` 接受 `LayeredDraw.Layer`，非 `GuiLayer`） |
| T7 GUI 渲染测试（`GuiTestScreen`） | `FakeDisplayLevel`/`GuiRenderExtras`（module.rendering）+ `ALRCommandEncoderExtension`/`GpuSampler`（26.1 GPU） |
| T11 `MinecraftMixin` | 注入 `Minecraft.<init>` 初始化 `ALRComputeCapabilities`，依赖 module.rendering 26.1 |
| T12 生成资源（bloom/cber 的模型、loot、原版模型覆盖） | datagen 产物，随 T3/T4 无意义；lang 中 `anvillib.configuration.*` 段随 T2/#P9 可再生成 |

### 5.12 构建体系（module.gradle 中的 26.1 特有内容）

| 不迁移内容 | 理由 |
|---|---|
| `clientIrisWorkaround`/`clientRenderDoc` run 配置、`evaluationDependsOn(':renderdoc-loader-internal')` | 26.1 模块化构建的 RenderDoc/Iris 调试接线；port 无 module.gradle 约定，RenderDoc 调试需手工接线（见 #P8） |
| `createModule` 模板中的 `Identifier` 生成、`java_version=25`、parchment 移除 | 26.1 平台内容；port 应生成 `ResourceLocation`、Java 21、保留 parchment |
| `changeModsToml` 依赖注入任务 | port 无 `.github/modules.json`（git ls-tree 核实），需补建清单或裁剪任务 |
| `gradle.properties` 的 `anvillib.needRunConfig*` 属性体系 | 26.1 聚合构建约定；port 各模块自建 run 配置（见 #P3） |

---

## 6. 执行顺序与依赖关系

> 总原则：**先依赖后使用、纯逻辑先行、独立项先验证、高风险项尽早暴露**。每步完成即验收（第 4 章对应小节），全部通过再进入下一步；每步在 port/1.21.1 分支工作树以独立提交落地（提交信息建议按仓库现有风格：`feat(rpc): ...` / `fix(recipe): ...` 等）。

### 执行顺序总览

```
#P1/#P2 ──→（#P7a）──→ #P6 ──→ #P3/#P4/#P5/#P8 ──→ #P9 ──→ #P11/#P12/#P13/#P16 ──→ #P15 ──→ #P17 ──→ #P14 ──→ #P18 ──→ #P19
```

### 第 1 步：#P1 → #P2（新模块骨架与最大 SPI 风险）

- #P1（RPC）先行：依赖面最窄（port 侧 network/util 已就绪）、3 处机械反替换，作为「新模块落地模板」验证 `settings.gradle` 注册、jarJar 坐标 `-1.21.1`、mods.toml 模板在 port 侧的整套流程；#P2（sync + processor）紧随其后，processor 的 CoreMod 重写（子项 2.6）是**最大的 SPI 不确定点**（services 文件可见性、GETSTATIC 类解析），尽早暴露。

### 第 2 步：#P7a（rendering 纯逻辑批）→ #P6（font）

- #P7a 先落地 rendering 模块骨架与 19 项纯逻辑（Buffer 布局、Sdf2d、事件、辅助类、shader 资源），难度低、无渲染平台风险，同时为 #P6 提供 jarJar 依赖坐标；#P6 在 #P7a 完成后即可开工（SDF 图集/布局/测量等纯逻辑不依赖 #P7b–#P7e），其 GPU 上传/绘制层可先以「DynamicTexture + GuiGraphics」独立路径落地，不受后处理进度阻塞。

### 第 3 步：#P3/#P4/#P5/#P8（其余新模块，可并行）

- #P4（collision）极低难度独立模块，可随时插入；#P3（explosion）依赖 port module.config（已就绪），与 #P5（space-select，依赖 port module.network）互不阻塞；#P8（renderdoc-loader）独立。四者均可并行执行，每个独立提交。

### 第 4 步：#P9（config 三项演进）——必须早于 #P15

- #P9 是 #P15 的 M7 子项（Config.group）的代码级前置：`@Config(group=…)` 离开 module.config 的 group 支持无法编译；与 #P3 的交互——#P3 若先落地按「去掉 group」处理（port @Config 无 group 属性），#P9 完成后可选补回 `group = "anvillib"` 并重跑 datagen（lang section 键名以 #P9 的点分隔规则为准）。

### 第 5 步：#P11/#P12/#P13/#P16（共享模块小演进，可并行）

- 四项均低-中难度、模块内改动：#P11（network）、#P12（util + recipe 调用方连带）、#P13（registrum builder 全家桶）、#P16（recipe 两处修复）。#P12 的 HolderGetter 重构与 #P16 互不干扰；#P12 注意保留 port 的 `Builder.with(BlockState)`（不可照搬 dev 删除）。

### 第 6 步：#P15（multiblock 四项）——依赖 #P9 已落地

- M1–M4 建议一并做（M3 与 M4 构造路径耦合、M2 与 M3 快照 record 字段耦合）；M7 的模块注解改动依赖 #P9。

### 第 7 步：#P17（wheel 环形扇区效果）

- 依赖 port 旧渲染体系（Tesselator/ShaderInstance/LibShaders），与 #P7 的新渲染模块无依赖（已切断 dev 侧 rendering jarJar 依赖）；独立执行。

### 第 8 步：#P14（registrum datagen 增强）

- 独立于 #P13（可不依赖其 entry API）；GeneratorType 拆分列为可选单独提交（消费方 1.21.1 不存在，价值低）。

### 第 9 步：#P18（module.test 可选测试）

- T2 随 #P9（TranslatableEnum）、T8 随 #P1、T9 随 #P3、T10 随 #P17；各测试项随对应功能落地，作为功能项的集成验收补充。

### 第 10 步：#P19（构建体系）——收尾

- roseauCheck 独立可随时执行，安排在最后作为全量 API 差分验收；module.gradle 集中化为可选工程决策（port 侧现有独立构建脚本自洽，是否重构由维护者决定，**不属于本计划硬性范围**）。

### 顺序之外的两条纪律

1. 每步提交前先跑一遍该模块 `build`（含 `jar`），避免只验 `compileJava` 漏掉资源处理（mods.toml 替换、jarjar 内嵌）。
2. 涉及 `runData`/运行类验证的项（#P3/#P6/#P7/#P9/#P14/#P15/#P17/#P18），如本地环境不便跑客户端，可先以编译 + CI 为准，把运行验收登记为「待 CI/人工补验」并给出复现步骤，**不得**把未验证项标记为已完成（见第 7 章）。

---

## 7. 总体验收标准

### 7.1 构建验收（硬性门槛）

| #P 涉及 | 验收项 | 命令 / 判据 |
|---|---|---|
| 全部 | port/1.21.1 全量构建 | 在 port/1.21.1 分支 `gradlew build`（根级，聚合全部模块含新增 8 个）**全部成功** |
| 全部 | 模块独立构建 | 依次 `gradlew :module.rpc:build` `:module.sync:build` `:module.explosion:build` `:module.collision:build` `:module.space-select:build` `:module.font:build` `:module.rendering:build` `:module.test:build` `:module.main:build` 均通过 |
| #P1–#P18 | 依赖完整性 | 各新模块 jar 的 `META-INF/jarjar/` 内含 `anvillib-*-neoforge-1.21.1-*.jar`（如 module.main 聚合产物）；无 `-26.1` 坐标残留 |
| #P2 | SPI 重写零残留 | `grep -rn "neoforgespi.transformation" module.sync` 无匹配；CoreMod services 注册生效 |
| #P19 | roseauCheck | `gradlew roseauCheck`（或对应任务）在 1.21.1 基线上运行通过，输出 API 差分报告 |

### 7.2 功能验收清单（行为核对）

| 覆盖项 | 验收项 | 判据 | 执行方式 |
|---|---|---|---|
| #P1/#P18 | RPC 远程调用 | 客户端注册 `@RemoteCallable` 方法并下发索引 → 服务端经 `invoke` 远程调用返回 `CompletableFuture` 结果；`@CallableParam` 自定义编解码器生效；tick 超时路径正确 | module.test T8 / 手工 |
| #P2 | sync 同步 | `@Sync` 注解字段两端同步；LazySync 惰性差分同步生效；Configuration 阶段配置表下发正常；processor 字节码注入目标类生效（启动日志 + 反编译核对） | module.test / 手工 |
| #P3/#P18 | 爆炸执行器 | 分层球壳破坏按概率半径执行；熔化替换（熔炼/高炉配方表）生效；实体 `hurt` 与掉落/熔化路径正确 | module.test T9 / 手工 |
| #P4 | 碰撞检测 | 静态 SAT 13 轴 / 三轴独立扫掠 / 真扫掠碰撞三个入口断言通过；`overlapOnAxis` 工具正确 | 手动运行测试入口 |
| #P5 | 选区渲染 | 框选创建区域；`DistrictRenderer` 各 stage 线框正确（含平移量/颜色语义）；滚轮缩放/移动生效；网络包双向收发正常 | 运行 runClient |
| #P6 | SDF 文字 | SDF 图集生成正确；文字分段样式（粗斜体/下划线/删除线/混淆）与居中/换行正确；`anvillib$text` 扩展可用；配置/测试界面正常 | 运行 runClient |
| #P7b | 后处理三件套 | Bloom 5 级金字塔效果正确（含 resize 跟随、帧稳定）；Blur/Glitch 效果正确；glitch 时间驱动动画正常 | 运行 runClient |
| #P7c | SDF GUI | 9 种距离函数图形绘制正确；命中测试准确 | 运行 runClient |
| #P7d | CachedBER | 缓存 BER 渲染正确（视锥剔除/半透明排序）；失效重建正确；bloom 双画正常；Iris 开启时回退正常 | 运行 runClient（无 Iris 可跳过最后一项） |
| #P7e | Compute | pipeline 注册、program 编译链接、dispatch 与内存屏障执行正确 | 运行 runClient |
| #P9 | 配置分组/枚举翻译 | `@Config(group)` 生成 `<config-dir>/<group>/<modid>-common.toml`；枚举翻译 key（`enum.<canonical name>`）正确；TOML section 键为点分隔 | runData + 运行 |
| #P10 | 集成数据加载 | 客户端/服务端数据加载器各自生效；进度条随实例递增 | 运行验证 |
| #P11 | 网络工具 | 多静态字段注册解析正确；`sendToAllPlayersIncluded` 过滤发送正确 | 编译 + 行为用例 |
| #P12 | util 演进 | OutlineUtil 轮廓提取正确；HolderGetter 谓词匹配正确；`withCount` 不改原实例；`with(BlockState)`（保留项）正常 | 编译 + 行为用例 |
| #P13 | 注册表入口 | 17 个入口注册后可在 `BuiltInRegistries`/`NeoForgeRegistries` 查到；CreativeTab 生成标签页；RecipeSerializer 可加载配方 | 编译 + 行为用例 |
| #P14 | datagen 增强 | 公开包装方法可生成配方（shelf/colorItemWithDye）；`dataMap(type, provider, factory)` 捕获 HolderLookup 正确 | runData |
| #P15 | multiblock 四项 | 成形/解散事件可取消（取消后回滚）；未加载区块快照复用不触发强加载；BlockPos 键化正确；定义懒解析正确 | 运行 runServer |
| #P16 | recipe 修复 | 随机 count 掷 0 不生成物品实体；JSON 缺 `nbt` 字段正常放置（无 NPE） | 行为用例 |
| #P17/#P18 | 环形扇区选择效果 | 菜单显示 DOT/ANNULAR_SECTOR 两种形态；选中点随进度缩放；颜色配置生效；旧渲染路径无回归 | 运行 runClient + T10 |

### 7.3 收尾核对

1. **提交完整性**：port/1.21.1 分支上 #P1–#P19 各为独立提交，提交信息含来源 issue/PR 号（#60/#63/#87/#65/#22/#56/#21/#41/#33/#25/#27/#28/#30/#31/#47/#52/#69/#73/#75/#49 等）。
2. **范围守卫**：`git log --oneline port/1.21.1..dev/26.1` 中，除第 5 章「不可移植清单」与 #P1–#P19 外，不应有「实际回移植但未立项」的内容；如执行中发现新功能，先补充本计划再执行。
3. **文档同步**：附录 B 的「待验证事项」在回移植执行中逐项销账；本计划中的「待验证」标注同样逐一关闭，关闭记录可追加到附录 B。
4. **遗留登记**：任何因环境限制未完成的运行类验收登记为遗留项，附复现步骤，不得默认通过。
5. **与姊妹文档一致性**：本计划不覆盖 `docs/migration-plan-dev-26.1.md` 已处理的 port→dev 方向内容（yukkuri 等 #1–#8）；若两侧计划对同一文件的修改意图冲突（如 #P12 保留 `with(BlockState)` vs dev 侧删除），以「port 侧保留现状」为准，并在 commit 说明中标注。

---

## 附录

### A. 常用只读 git 命令（本计划全部事实均来自以下命令）

```bash
# 提取 dev 侧文件内容（全部为只读操作）
git show dev/26.1:module.rpc/src/main/java/dev/anvilcraft/lib/v2/rpc/AnvilLibRpc.java
git show dev/26.1:module.sync/src/main/java/dev/anvilcraft/lib/v2/sync/AnvilLibSync.java
# 查看来源提交的完整 diff（代表性提交）
git show d153e58              # #P1（RPC 主体）
git show 1063ee9              # #P2（processor 新增）
git show 435e23f              # #P3（explosion 初建）
git show 82b4f6a              # #P4（collision）
git show 1335fc5              # #P5（space-select）
git show 1d56a4e              # #P6（font）
git show 7121bf6              # #P7（rendering Bloom 系）
git show 7d07f20              # #P7（rendering Compute/Buffer 布局）
git show 494274e              # #P7（SDF GUI）
git show 2751be4              # #P8（renderdoc-loader 初建）
git show 3d50d76              # #P9（Config.group）
git show 946938f              # #P9 / #P6 / #P19（TranslatableEnum + TOML 点分隔 + roseau）
git show dc4d583              # #P10（integration 数据加载拆分）
git show 20721b1              # #P11（PacketData 泛型检查）
git show e4c09fb              # #P12（OutlineUtil）
git show bb09805              # #P12（predicate HolderGetter 重构）
git show 9b2d0e3              # #P13（注册表更多种类）
git show 58f01d4              # #P14（datagen 重构）
git show 1e5bfb3              # #P15（DynamicMultiblockEvent）
git show d888714              # #P15（未加载区块快照）
git show 22091d4              # #P15（Long→BlockPos 键化）
git show 1b0a652              # #P15（定义懒解析）
git show 40b8f84              # #P16（SpawnItem 零数量守卫）
git show 5d0b285              # #P17（环形扇区选择效果）
git show a6a24f1              # #P18 / #P19（roseauCheck + T2 配置测试）
git show ea4301f              # #P19（module.gradle 集中化）
# 两侧同路径对比
git diff port/1.21.1 dev/26.1 -- module.network/src/main/java/dev/anvilcraft/lib/v2/network/util/NetworkUtil.java
# 某模块 dev 独有提交清单（回移植候选扫描）
git log --oneline port/1.21.1..dev/26.1 -- module.registrum
# dev 侧落点现状
git show dev/26.1:module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/event/DynamicMultiblockEvent.java
# 模块文件清单（新增/删除核对）
git diff --name-status port/1.21.1 dev/26.1 -- module.rpc
git ls-tree -r --name-only dev/26.1 -- module.space-select
```

### B. 待验证事项汇总（执行时逐项销账）

| 编号 | 事项 | 涉及项 | 验证方法 | 状态 |
|---|---|---|---|---|
| V-1 | `Mth.roundToward`/`VertexSorting`/`CompactVectorArray`/`MeshData.sortQuads` 在 1.21.1 的存在性（报告依据「1.20.2+ 稳定 API」判断；本机 gradle 缓存无 1.21.1 client jar 可反查） | #P7a | 移植首个编译时以 1.21.1 client sources 复核 | 未验证 |
| V-2 | Iris 1.7.x 的 `ImmediateState` 字段（`skipExtension`/`isRenderingLevel`）与 `IrisApi` 方法签名（dev compileOnly 依赖为 Iris 1.10.9+26.1） | #P7d | 按 1.21.1 Iris 版本（1.7.x）sources 复核 | 未验证 |
| V-3 | `MultiBufferSource.BufferSource` 构造器参数（dev 侧 `super(null, null)` 需小调） | #P7a | 1.21.1 sources + 编译 | 未验证 |
| V-4 | `LevelRenderer.renderVoxelShape`/`renderLineBox` 的颜色语义（RGBA 0-1 float）与 dev `ARGB` int 色的转换 | #P5 | 实现后目测确认（report §6） | 未验证 |
| V-5 | port 侧各模块 build.gradle 的 run 配置自建模式（port 无 module.gradle，`anvillib.needRunConfig*` 不生效） | #P3/#P5/#P8 | 参照 port `module.config/build.gradle`（含 data run 段） | 未验证 |
| V-6 | Iris 1.21.1 对应版本（1.7.x）的 Modrinth 依赖坐标 | #P7d | 移植时查询 Modrinth | 未验证 |
| V-7 | mods.toml 模板变量（`${loader_version_range}` 等）在 port 侧注入方式 | #P7 | 按 port 现有模块模板对齐 | 未验证 |
| V-8 | `ShaderDefines`（26.1 类）改为 `Map<String,String>` defines 后 `ALRComputePipeline`/`ALRComputePass` 编译与运行 | #P7e | 编译 + 运行验证 | 未验证 |
| V-9 | CoreMod transformer 在 jar-in-jar（`jarJar(api(...))` 携带 processor）场景下 services 文件可见性 | #P2 | 打包后实测启动 | 未验证 |
| V-10 | CoreMod transformer 中 `AnvilLibSync.LAZY_SYNC_MANAGER` GETSTATIC 引用的类可解析性（game layer 类加载时机） | #P2 | 启动日志 + 反编译核对注入点 | 未验证 |
| V-11 | `getSecureJar().getPath(classPath)` 在 1.21.1 securejarhandler 3.0.8 的签名（返回 `Path`） | #P2 | securejarhandler 3.0.8 sources + 编译 | 未验证 |
| V-12 | `AnvilLibFontConfig` 中 `FMLLoader.getCurrent().getGameDir()` 在 1.21.1 可用性（`getCurrent()` 的例外调用点） | #P6 | 编译；报错则改 `FMLPaths.GAMEDIR.get()` | 未验证 |
| V-13 | jspecify 依赖在 port 构建链的可用性（#P15 M6 可选注解项） | #P15 | port `module.test` 已用 `org.jspecify.annotations.NullMarked` 且 gradle 缓存存在 `org.jspecify/jspecify`——以编译为准 | 未验证 |
| V-14 | `VillagerProfession` 6 元 record 与 `Potion(@Nullable String, MobEffectInstance...)` 构造在 1.21.1 的确认 | #P13 | 1.21.1 源码（报告已核实，落位时以编译复核） | 未验证 |
| V-15 | `RecipeSerializer` 匿名类实现（`codec()`/`streamCodec()`）在 1.21.1 的编译通过性 | #P13 | `gradlew :module.registrum:compileJava` | 未验证 |
| V-16 | `District.color()` 手写 int 打包（`(a<<24)|(r<<16)|(g<<8)|b`）在渲染端的颜色语义 | #P5 | 渲染目测 + 与 dev 行为对比 | 未验证 |
| V-17 | `GatherDataEvent`（单一类）的 `includeClient()`/`getPackOutput()` 在 21.1.226 的签名 | #P3/#P6/#P14 | 21.1.226 sources（报告已核实）+ 编译 | 未验证 |
| V-18 | 根构建脚本（`build.gradle`/`settings.gradle`）模块注册方式与 port 侧现有模块（module.wheel 等）对齐 | #P1–#P8 | 参照 port 现有模块 | 未验证 |

### C. 参考资料

| 资料 | 位置 |
|---|---|
| **8 个并行子代理模块级调研报告（本文全部事实依据，1395 行）** | `C:/Users/Administrator/.kimi-code/sessions/wd_anvillib_031cb785b7e3/session_6033f19b-f4ef-481a-b18c-d626cc22c314/agents/main/tool-results/AgentSwarm-call_00_ET_e0hBtNS4BowwxHQBCso99671-b31c451d-23ff-46b7-8aa0-0ba516956d9d.txt` |
| 反向迁移计划（port→dev，编号 #1–#8，编号体系与本计划无关） | `docs/migration-plan-dev-26.1.md` |
| 分支对比报告（两分支整体关系与分类） | `docs/branch-comparison.md` |
| dev 侧关键文件实测基准 | `module.rpc/`、`module.sync/`、`module.explosion/`、`module.collision/`、`module.space-select/`、`module.font/`、`module.rendering/`、`renderdoc-loader/`（均以 `0b7e45f` 为准） |
| port 侧现状基准 | port/1.21.1 @ `bd25229`；`module.config`（ConfigManager/Config/Comment/ConfigData）、`module.network`（NetworkRegistrar/IPacket/IServerboundPacket/NetworkUtil）、`module.util`（ShapeUtil/UnlimitedItemStack）、`module.wheel`（LibShaders/ring.json/selection.json）、`module.multiblock`（AnvilLibDatagen） |
| NeoForge 21.1.226 sources jar（事件/网络/配置 API 核实依据） | 本地 gradle 缓存（`net.neoforged:neoforge:21.1.226-sources.jar`）与 `module.config/build/moddev/artifacts/` 下 patched 源码 |
| 1.21.1 混淆 jar / official mappings（javap 核实依据） | `neoformruntime/artifacts/minecraft_1.21.1_client.jar`、`minecraft_1.21.1_client_mappings.txt` |

### D. 回移植提交清单（PR 提交模板）

> 每个回移植项在 port/1.21.1 分支上对应一个独立提交。下表可直接作为 PR 描述模板，勾选「状态」列后随 PR 提交。

| 提交 | 回移植项 | 涉及文件（变更面） | 关键验证命令 | 状态 |
|---|---|---|---|---|
| commit 1 | #P1 rpc | `module.rpc/**`（25 文件）+ `settings.gradle` | `gradlew :module.rpc:compileJava`；`grep -rn "Identifier\|accessFlags" module.rpc/src`（0 匹配） | ☐ |
| commit 2 | #P2 sync | `module.sync/**`（31 文件）+ processor CoreMod 重写 + `settings.gradle` | `gradlew :module.sync:compileJava`；`grep -rn "neoforgespi.transformation" module.sync`（0 匹配） | ☐ |
| commit 3 | #P3 explosion | `module.explosion/**`（10 文件）+ `settings.gradle` | `gradlew :module.explosion:compileJava` + `runData` | ☐ |
| commit 4 | #P4 collision | `module.collision/**`（3 文件）+ `settings.gradle` | `gradlew :module.collision:compileJava` + 手动测试 | ☐ |
| commit 5 | #P5 space-select | `module.space-select/**`（18 文件）+ `settings.gradle` | `gradlew :module.space-select:compileJava` + runClient | ☐ |
| commit 6 | #P7a rendering 纯逻辑 | `module.rendering/**`（19 项纯逻辑 + 骨架）+ `settings.gradle` | `gradlew :module.rendering:compileJava` + `:module.rendering:test` | ☐ |
| commit 7 | #P6 font | `module.font/**`（25 文件）+ `settings.gradle` | `gradlew :module.font:compileJava` + `runData` + runClient | ☐ |
| commit 8 | #P7b–#P7e rendering 执行层 | `module.rendering/**`（后处理/SDF GUI/CachedBER/Compute 四批） | 各批 `compileJava` + 运行验证（7.2 清单） | ☐ |
| commit 9 | #P8 renderdoc-loader | `renderdoc-loader/**`（2 文件） | `gradlew :renderdoc-loader:compileJava` + jar manifest 核对 | ☐ |
| commit 10 | #P9 config | `module.config/**`（Config/ConfigManager/ConfigRecord/ConfigData/FormattingUtil/TranslatableEnum） | `gradlew :module.config:compileJava` + `runData` | ☐ |
| commit 11 | #P10 integration | `module.integration/**`（3 文件） | `gradlew :module.integration:compileJava` + 行为验证 | ☐ |
| commit 12 | #P11 network | `module.network/**`（3 文件） | `gradlew :module.network:compileJava` | ☐ |
| commit 13 | #P12 util | `module.util/**`（4 处演进）+ `module.recipe/**`（4 个调用方） | `gradlew :module.util:compileJava` + `:module.recipe:compileJava` + `runShapeUtilJoinTimingTest` | ☐ |
| commit 14 | #P13 registrum | `module.registrum/**`（builders/util.entry 新增 + AbstractRegistrum） | `gradlew :module.registrum:compileJava` | ☐ |
| commit 15 | #P16 recipe | `module.recipe/**`（SpawnItem/SetBlock） | `gradlew :module.recipe:compileJava` + 行为用例 | ☐ |
| commit 16 | #P15 multiblock | `module.multiblock/**`（M1–M4 + M7 注解） | `gradlew :module.multiblock:compileJava` + runServer | ☐ |
| commit 17 | #P17 wheel | `module.wheel/**`（WheelSelectionEffect + fsh + 渲染适配） | `gradlew :module.wheel:compileJava` + runClient | ☐ |
| commit 18 | #P14 registrum datagen | `module.registrum/**`（RecipeProvider/DataMapProvider/Builder） | `gradlew :module.registrum:compileJava` + `runData` | ☐ |
| commit 19 | #P18 test | `module.test/**`（T2/T8/T9/T10） | `gradlew :module.test:compileJava` + 运行验证 | ☐ |
| commit 20 | #P19 构建 | 根 `roseau.yaml` + `gradle/scripts/roseau.gradle`（+ 可选 module.gradle） | `gradlew roseauCheck`；`gradlew build` | ☐ |
| 收尾 | 全量回归 | 全仓 | `gradlew build`（根级）全绿；附录 B 待验证清单销账；与 `docs/migration-plan-dev-26.1.md` 无冲突 | ☐ |
