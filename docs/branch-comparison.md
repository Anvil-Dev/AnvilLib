# AnvilLib 分支对比报告：`port/1.21.1` ↔ `dev/26.1`

> 文档信息
>
> - 生成日期：2026-08-10
> - 分析对象：`port/1.21.1` @ `fab551b`（Minecraft 1.21.1 / NeoForge 21.1.226）、`dev/26.1` @ `0b7e45f`（Minecraft 26.1.2 /
    NeoForge 26.1.2.76）
> - 共同祖先（merge-base）：`81c5a9c`
> - 数据来源：git 实测（`git log` / `git diff` / `git show` 只读命令）+ 8 个并行子代理模块级调研（见附录 D）
> - 关联文档：本文是 **分支对比报告**，聚焦「两侧关系与差异分类」；回迁执行细节见迁移计划文档（其中回迁项使用与本报告一致的
    canonical 编号 #1–#8）
> - 结论性质：除标注「待验证」处外，均基于实测提交与文件内容得出

---

## 1. 概述与结论

AnvilLib 的两个长期分支在共同祖先 `81c5a9c` 处分化后走向了不同的生命周期：`dev/26.1` 是 **演进主线**（在 merge-base 之后新增
170 个提交），它沿 1.21.2 → 1.21.11 → 26.1 的版本链一路升级、引入 `module.gradle` 聚合构建体系、新建 rendering / rpc /
sync / explosion / font / collision / space-select 等一批新模块，并通过 `7019b38` 一次性移植了当时 1.21.1 侧的
multiblock / util / network 后持续演进；`port/1.21.1` 则是 **维护与特性分支**（在 merge-base 之后新增 46 个提交），它在冻结的
1.21.1 平台上继续落地了网络库、wheel 模块、multiblock 动态系统、油库（yukkuri）模块等特性并修复了一批 bug。迁移方向为
**单向（port → dev）**：port 侧 46 个独有提交中，除约 23 个与 dev 早期提交同主题的镜像提交外，真正需要回迁的独有内容收敛为
**8 项（#1–#8）**，其中 `module.yukkuri` 整模块（#1）是唯一的大项，其余为小型 bugfix 与可选 API 补充；codec / network /
config / integration / registrum / wheel / moveable-entity-block 七个共享模块判定为 **dev 超集或同源重写，无需回迁**
。结论是：以 dev/26.1 为最终目标，仅按 #1–#8 执行受控回迁，不做整分支合并。

### 1.1 结论要点

| #  | 结论                                                                                                                 | 依据                                                                              |
|----|----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| C1 | 迁移方向单向：`port/1.21.1` → `dev/26.1`，不做反向合并                                                               | dev/26.1 为演进主线（170 独有提交），port 侧为 1.21.1 平台维护分支（46 独有提交） |
| C2 | 回迁收敛为 8 项（#1–#8），唯一大项是 `module.yukkuri` 整模块（#1）                                                   | 逐模块双向 diff 分类结果（见第 5 章）                                             |
| C3 | 七个共享模块无需回迁：codec / network / config / integration / registrum / wheel / moveable-entity-block             | dev 侧为超集或同源重写；port 侧 bugfix 在新架构下天然规避                         |
| C4 | 共享模块中 util / recipe / multiblock / test / main 存在回迁点，共 8 项中的 7 项（#2–#8）                            | 逐文件 diff 定位（见 5.2）                                                        |
| C5 | `module.yukkuri` 迁移需在「继续使用已弃用流体 API」与「适配新 `ResourceHandler<FluidResource>`」之间二选一，推荐后者 | NeoForge 26.1.2.76 sources jar 实测（见 5.3）                                     |
| C6 | port 侧独立构建脚本、lang datagen 产物、`ASYNC_MULTIBLOCK_CHECK_PLAN.md` 不回迁                                      | 构建体系已被 `module.gradle`（`ea4301f`）取代；文档已标注「已完成实现」           |

---

## 2. 分支拓扑

### 2.1 拓扑数据

以下数据均为 2026-08-10 在本地仓库实测（只读命令）：

| 项                                           | 值                                                      |
|----------------------------------------------|---------------------------------------------------------|
| merge-base（共同祖先）                       | `81c5a9c`（`81c5a9c5246a8d3642ea5cfddaf5298f885f7872`） |
| `port/1.21.1` HEAD                           | `fab551b`（`fab551bbda48837bf43b4423006b391e4f049221`） |
| `dev/26.1` HEAD                              | `0b7e45f`（`0b7e45f1bd52561f4ad42e468a72bfdcbee5d23c`） |
| dev 侧独有提交数（`port/1.21.1..dev/26.1`）  | **170**                                                 |
| port 侧独有提交数（`dev/26.1..port/1.21.1`） | **46**                                                  |

即：`port/1.21.1` 领先 `dev/26.1` **46 个提交**（port 独有），落后 **170 个提交**（dev 独有）。两侧从 merge-base
之后不再有共同提交，处于完全分叉状态。

```text
                        merge-base
                        81c5a9c
                           │
        ┌──────────────────┼──────────────────┐
        │ (46 commits)     │ (170 commits)     │
        ▼                  │                   ▼
  port/1.21.1              │              dev/26.1
  fab551b                  │              0b7e45f
  MC 1.21.1                │              MC 26.1.2
  NeoForge 21.1.226        │              NeoForge 26.1.2.76
                           │
              port 独有 46 提交 / dev 独有 170 提交
```

### 2.2 版本与构建环境对比

| 项                | `port/1.21.1`                                                            | `dev/26.1`                                                                                                            |
|-------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Minecraft         | 1.21.1（`gradle.properties`：`minecraft_version_range=[1.21.1,1.21.2)`） | 26.1.2（`gradle/libs.versions.toml`：`minecraft = "26.1.2"`）                                                         |
| NeoForge          | 21.1.226                                                                 | 26.1.2.76（`gradle/libs.versions.toml`：`neoForge = "26.1.2.76"`）                                                    |
| Java              | 21（`gradle.properties`：`java_version=21`）                             | 25（`gradle.properties`：`java_version=25`）                                                                          |
| 映射（Parchment） | 1.21.1 / 2024.11.17                                                      | 无 Parchment（26.1 时代不再使用）                                                                                     |
| 模组版本          | 2.0.0                                                                    | 2.0.0                                                                                                                 |
| 许可证            | MIT                                                                      | MIT                                                                                                                   |
| 构建体系          | 各模块独立 `build.gradle` + 根级 `gradle.properties` 传参                | 根级 `module.gradle` 聚合（`ea4301f`）+ `createModule` 任务（`16b7bb5`）+ 依赖坐标后缀 `-neoforge-26.1`               |
| 依赖版本管理      | 无统一版本目录                                                           | `gradle/libs.versions.toml`（modDevGradle 2.0.141、lombok 9.2.0、machete 2.0.1、jreleaser 1.23.0、mod-publish 1.1.0） |
| 模块结构          | 13 个 `module.*`（含 `module.yukkuri`）                                  | 19 个 `module.*` + `module.gradle` + `renderdoc-loader`（不含 yukkuri）                                               |

dev/26.1 侧 `gradle.properties` 另有关键开关 `interface_injection=false`，port 侧无此项。

### 2.3 历史脉络与里程碑提交

两侧在 merge-base `81c5a9c` 之后的关键里程碑（按时间顺序）：

| 日期       | 提交             | 分支 | 事件                                                                                                                          |
|------------|------------------|------|-------------------------------------------------------------------------------------------------------------------------------|
| 2026-03-26 | `6f2685a`        | dev  | `chore: update project to use Minecraft and NeoForge version 26.1`——正式切入 26.1                                             |
| 2026-04-14 | `5fc6f65`        | port | `feat(multiblock): add dynamic multiblock system`——port 侧原创动态多方块系统                                                  |
| 2026-05-02 | `7019b38`        | dev  | `chore: Port multiblock and util from 1.21.1`——一次性移植当时 1.21.1 侧的 multiblock / util / network，随后完成 26.1 API 适配 |
| 2026-05-11 | `ea4301f`        | dev  | `feat(build): update build configuration and add module.gradle for modular setup`——构建体系重构                               |
| —          | `569e01d`（#88） | port | `添加油库（里）`——新增 `module.yukkuri` 整模块                                                                                |
| —          | `fab551b`（#91） | port | `修油库`——port 侧当前 HEAD                                                                                                    |

两侧在 merge-base 之后的完整升级链（dev 侧）与特性链（port 侧）见第 3 章提交主题对比。

### 2.4 拓扑解读

1. **dev/26.1 的 170 个独有提交**中，前段（`81d1f59` → `6f2685a`）是 1.21.2 → 26.1 的连续版本升级链（共 11 次版本更新提交），中段是
   26.1 API 适配与模块演进，后段（`7019b38` 之后）是移植与新建模块的持续开发。170 的数字里包含大量重构/清理提交，实际「功能面」比表面小。
2. **port/1.21.1 的 46 个独有提交**中，约 23 个与 dev 早期提交 **同主题镜像**（如网络库、license 改 MIT、bump 2.0.0、wheel
   等，对照表见附录 C），是两侧在分叉后分别重做/挑选的相同内容；真正 port 独有、dev 无对应物的内容集中在 multiblock
   动态系统、yukkuri、piston bugfix 与 util/recipe 的少量修复上——这正是回迁清单 #1–#8 的来源。
3. 由于两侧无共同提交， **直接 merge 会引入大规模冲突且方向错误**（会把 dev 侧 26.1 演进全部回退到 1.21.1
   语义），故采用「受控回迁」而非分支合并。

---

## 3. 提交主题对比

本章基于两条只读命令的完整输出归纳：

```bash
git log --oneline port/1.21.1..dev/26.1   # dev 侧独有，170 条
git log --oneline dev/26.1..port/1.21.1   # port 侧独有，46 条
```

### 3.1 dev/26.1 侧关键演进主题（170 提交，7 个主题）

#### 主题 D1：26.1 移植与版本升级链

dev 分支在 merge-base 之后的第一要务是版本升级：从 1.21.2 起步，经 1.21.3 → 1.21.11 共 11 次版本提交，最终在 `6f2685a`
（2026-03-26）切入 26.1；随后 `7019b38`（2026-05-02）移植 port 侧 multiblock / util / network，并做了一系列 26.1 API
适配（ItemStackTemplate、ResourceHandler 缓存、ResourceKey 配方标识、FMLLoader.getCurrent () 环境检查等）。

| 提交      | 主题                                                                                                           |
|-----------|----------------------------------------------------------------------------------------------------------------|
| `81d1f59` | `chore(build): 更新所有模块及相关配置至Minecraft 1.21.2版本`——升级链起点                                       |
| `d6fb290` | 1.21.5 升级 + `ItemSubPredicate → DataComponentPredicate` 重构                                                 |
| `b3728b6` | 1.21.11 / 21.11.40-beta 升级                                                                                   |
| `6f2685a` | `chore: update project to use Minecraft and NeoForge version 26.1`——正式切入 26.1                              |
| `7019b38` | `chore: Port multiblock and util from 1.21.1`——一次性移植三模块                                                |
| `6e00420` | `refactor: replace ItemStack with ItemStackTemplate in various classes`——26.1 API 适配                         |
| `2033fd9` | `refactor(recipe-cache): 替换物品处理器缓存为资源处理器缓存`——26.1 资源 API 适配                               |
| `f14cde3` | `refactor: update RecipeMapMixin to use ResourceKey for recipe identification`                                 |
| `f03fbd7` | `refactor: update BlockEntityBuilder and related classes to use FMLLoader.getCurrent() for environment checks` |

#### 主题 D2：构建体系重构（`module.gradle` 聚合）

dev 侧将各模块独立构建脚本统一为根级 `module.gradle` 聚合，配套 CI、发布配置同步重构。该体系 **不回迁**（port 侧不回迁 dev
的构建体系，见 5.4）。

| 提交      | 主题                                                                                                |
|-----------|-----------------------------------------------------------------------------------------------------|
| `ea4301f` | `feat(build): update build configuration and add module.gradle for modular setup`——聚合构建核心提交 |
| `16b7bb5` | `feat(build): add createModule task for modular project setup`                                      |
| `f97ec94` | `feat(build): add exclusive content configuration for Anvil Lib repository`                         |
| `1063ee9` | `Add sync processor module and refactor build configuration`                                        |
| `aa59e37` | `refactor(ci): 重构CI工作流以支持模块化构建`                                                        |
| `7f27908` | `feat(ci): add synchronization workflows for build and Maven Central deployment`                    |
| `a26fde5` | `feat(build): 添加Maven发布仓库配置支持`                                                            |
| `ae2acd0` | `feat(build): add publishing configuration for CurseForge and Modrinth`                             |

#### 主题 D3：新模块建设（rendering / rpc / sync / explosion / font / collision / space-select）

dev 侧新建了一批 1.21.1 侧没有的模块：字体渲染（font）、SDF 渲染管线（rendering，含 RenderDoc 集成）、注解式 RPC、同步框架（sync，含独立
processor 子模块）、爆炸模拟（explosion）、AABB/三角形相交检测（collision）、空间选择（space-select）。

| 提交      | 主题                                                                                  |
|-----------|---------------------------------------------------------------------------------------|
| `1d56a4e` | `添加自定义文字渲染模块 module.font`（#22）                                           |
| `7121bf6` | `feat(rendering): Add bloom post-processing and add RenderDoc integration`（#17）     |
| `3ce2611` | `feat(sync): implement synchronization framework with SyncManager and annotations`    |
| `bf49789` | `feat(sync): implement SyncConfigManager and payload handling for synchronization`    |
| `d153e58` | `Implement RPC module with annotation-based remote method calls`（#60）               |
| `4910620` | `feat(rpc): 添加对更多数据类型的网络传输支持`（#87）                                  |
| `435e23f` | `Implement explosion module with features and optimizations`（#58）                   |
| `82b4f6a` | `feat(collision): add AnvilLibCollision for AABB and triangle intersection detection` |
| `1335fc5` | `Add space selection and district management features`（#24）                         |

#### 主题 D4：rendering 模块持续演进（SDF / GUI / Bloom / 兼容性）

rendering 是 dev 侧迭代最密集的模块：缓存型 Block Entity 渲染管线（视锥剔除、半透明排序、Bloom）、SDF 共享参数化、GUI 坐标
int→float 破坏性重构、AMD/Mac 与 Iris 兼容修复等。

| 提交                                          | 主题                                                                                               |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------|
| `2751be4`                                     | `feat(rendering): 引入缓存型 Block Entity Rendering 管线，支持视锥剔除、半透明排序与 Bloom`（#26） |
| `5b9cdcd`                                     | `refactor(rendering)!: migrate GUI coordinate parameters from int to float`（#34）——破坏性重构     |
| `b5f5420`                                     | `Implement bloom post-processing effect`（#20）                                                    |
| `8a818f0`                                     | `feat(rendering): add compound submit node infrastructure with dirty tracking for bloom`（#23）    |
| `6b5a544` / `6d5a97b`                         | SDF 渲染共享参数管理重构与索引修复（#44 / #45）                                                    |
| `d7a8f5d`                                     | `fix(sdf): fix sdf not working on AMD/Mac`                                                         |
| `d121047` / `48bfe55` / `fba1027` / `0e8290d` | Iris 兼容（#68）、日志刷屏修复（#74）、Iris 顶点格式（#76）、bloom/compute 稳定性（#92）           |

#### 主题 D5：registrum 模块演进

registrum 在 dev 侧经历了注册表项修复、数据组件谓词构建器、CreativeTabBuilder、音效/配方注册、类重命名与文档链接修复等一系列演进。

| 提交                              | 主题                                                                          |
|-----------------------------------|-------------------------------------------------------------------------------|
| `9b2d0e3`                         | `更多注册表注册`（#31）                                                       |
| `9627358` / `da801d2` / `8026804` | 修复误删的注册表项 + 补全 MIT 协议声明（#30）                                 |
| `252922e`                         | `Registrum 添加数据组件谓词构建器支持`（#47）                                 |
| `79965dc`                         | `refactor(registrum): 更新数据组件谓词方法的泛型类型定义`（#77）              |
| `9c89bfa`                         | `Add CreativeTabBuilder for custom tabs and update item registration`（#27）  |
| `c307a4c`                         | `添加了音效注册表，添加了配方注册`（#28）                                     |
| `655ad78`                         | `refactor: rename classes and update original file links in Registrum module` |

#### 主题 D6：multiblock 动态系统在 dev 侧的后续演进

`7019b38` 移植后，dev 侧继续演进动态多方块：定义获取改用 RegistryAccess、状态管理从 Long 换为
BlockPos、新增成形/解体事件（#69）、修复未加载区块快照问题（#73）。

| 提交      | 主题                                                                                                 |
|-----------|------------------------------------------------------------------------------------------------------|
| `1b0a652` | `fix(multiblock): update definition retrieval to use registry access in form and unform packets`     |
| `22091d4` | `fix(multiblock): update multiblock state management to use BlockPos instead of Long`                |
| `1e5bfb3` | `feat(multiblock): implement dynamic multiblock event handling for formation and unformation`（#69） |
| `d888714` | `Fix bug where DynamicMultiblockManager attempts to load unloaded blocks`（#73）                     |
| `f40b016` | `feat(network): add Network annotation to multiblock package-info`                                   |

#### 主题 D7：代码质量与生态统一

| 提交                  | 主题                                                                                                                                                                                  |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `f94126f`             | `refactor(nullness): replace javax.annotation with org.jspecify.annotations for nullability annotations`——@NullMarked 统一（注：另有 `3bf9b6c` 为「注册表更多种类添加 #25」，勿混淆） |
| `6fb6335`             | `chore(license): update mod license from GNU LGPL 3.0 to MIT and change copyright holder`                                                                                             |
| `c72aa30`             | `chore(annotations): 添加 ApiStatus.Internal 注解到内部接口和类`（#93）                                                                                                               |
| `0b7e45f`             | `refactor(common): 优化代码导入`（#96）——dev 当前 HEAD                                                                                                                                |
| `fd18fe9` / `b1484bc` | UnlimitedItemStack 弃用与构造优化（#55 / #86）                                                                                                                                        |
| `113423a`             | `feat(mod-description): update mod_description to provide a detailed overview of the space selection library`                                                                         |

### 3.2 port/1.21.1 侧关键演进主题（46 提交，6 个主题）

#### 主题 P1：网络模块落地（1.21.1 平台）

port 侧在分叉后完成了网络库的完整落地：模块创建、CI 补充、打包/mixin/自动注册三个修复。该系列与 dev 早期提交（`8d8ac8c`
等）同主题镜像，dev 侧为等价实现， **不回迁**。

| 提交                  | 主题                                             |
|-----------------------|--------------------------------------------------|
| `a222300`             | `feat(network): 添加网络库`                      |
| `19d98e3` / `10069a0` | 网络库描述与 javadoc 优化                        |
| `0d6c1b4`             | `feat(ci): 补充网络库缺失的ci`                   |
| `c7a790b`             | `fix(packaging): 修复网络库不会打包进主库的问题` |
| `00d65d0`             | `fix(mixin): 修复因缺失mixin文件导致的崩溃问题`  |
| `6d2741f`             | `fix(network): 修复网络包自动注册的问题`         |
| `23719fb` / `d88b8a6` | `BoolAndInt` 紧凑编解码 record 与 long 编码改进  |

#### 主题 P2：multiblock 动态系统（port 侧原创）

动态多方块系统最初在 port 侧实现（`5fc6f65`，2026-04-14），后被 `7019b38` 移植到 dev。port 侧的独特贡献集中在异步检查、错误处理、位置修正与
datagen 本地化。

| 提交      | 主题                                                                                           |
|-----------|------------------------------------------------------------------------------------------------|
| `5fc6f65` | `feat(multiblock): add dynamic multiblock system`——动态系统核心                                |
| `ba395e7` | `feat(multiblock): add data generation and localization support for multiblock configurations` |
| `2634e49` | `feat(multiblock): implement asynchronous multiblock checking with configurable parameters`    |
| `3832895` | `feat(multiblock): enhance error handling in controller retrieval and logging`                 |
| `4aa9d13` | `feat(multiblock): improve position correction logic in multiblock controller detection`       |

#### 主题 P3：wheel 模块

wheel 在两侧均有提交（port `18834b5`/`3d0a364` ↔ dev `0d73ab0`/`1ae3411` 同主题），dev 侧为 26.1 等价实现， **不回迁**。

| 提交      | 主题                                                                                          |
|-----------|-----------------------------------------------------------------------------------------------|
| `18834b5` | `feat(wheel): Implement Wheel module with API, documentation, and build updates`（#16）       |
| `3d0a364` | `feat(wheel): Refactor WheelMenu and WheelEntry for improved action handling and null safety` |

#### 主题 P4：util 模块扩展与修复

| 提交      | 主题                                                                                             |
|-----------|--------------------------------------------------------------------------------------------------|
| `9d61997` | `feat(util): add ClientTickRecorder and utility methods for dimension and player name retrieval` |
| `9874db7` | `fix(tick): ensure tick recording only occurs when the level is not null`——**回迁项 #4**         |
| `c8ee900` | `feat(util): update ISerializer package structure and add WeightedChanceBlockStates class`       |
| `0efcd3d` | `feat(shape): add cut methods for VoxelShape and AABB to remove specified shapes`                |
| `95bd5ca` | `feat(util): refactor utility classes to use UtilityClass annotation and improve organization`   |

#### 主题 P5：piston / moveable-entity-block 功能与 bugfix

| 提交      | 主题                                                                                        |
|-----------|---------------------------------------------------------------------------------------------|
| `53eaa11` | `feat(piston): implement moveable entity block functionality and rendering enhancements`    |
| `1acdcf8` | `fix(piston): Fixed the issue of PoseStack not being emptied due to premature returns`      |
| `9c437b1` | `feat(piston): enhance PistonBaseBlockMixin with shared block entity handling`              |
| `f2b60b6` | `fix(cache): make setting the simulated item stack would properly handle empty type stacks` |

结论：该模块 dev 侧以 26.1 渲染架构 **同源重写**（`83477ab` 等），port 侧 `1acdcf8` 修复的「PoseStack 提前 return
未清空」问题在新架构下天然规避， **无需回迁**（详见 5.4）。

#### 主题 P6：油库（yukkuri）模块与发布收尾

yukkuri 是 port 侧唯一整模块独有内容，也是回迁清单的最大项（#1）；`1311782` 的 CurseForge 环境声明是 main 模块的回迁项（#8）。

| 提交      | 主题                                                                                           |
|-----------|------------------------------------------------------------------------------------------------|
| `569e01d` | `添加油库（里）`（#88）——新增 `module.yukkuri`（14 个 Java 文件 + build + mods.toml + README） |
| `18b29ca` | `ci: publish yukkuri before aggregate module`（#89）                                           |
| `1311782` | `fix: declare CurseForge environments`（#90）——**回迁项 #8**                                   |
| `fab551b` | `修油库`（#91）——port 当前 HEAD，yukkuri 收尾修复                                              |

#### 主题 P7：recipe / codec 杂项修复

| 提交                  | 主题                                                                                                          |
|-----------------------|---------------------------------------------------------------------------------------------------------------|
| `edd71ec`             | `修复鱼缸加工bug`（#83）——**回迁项 #2**                                                                       |
| `e78d020`             | `修复鱼缸加工bug`（#84）——**回迁项 #3**                                                                       |
| `0b56109`             | `Add DamageSourcePredicate StreamCodec and its relative StreamCodecs`（#67）——dev 侧 `3b5dfea` 同主题，不回迁 |
| `50aeac7`             | `feat(codec): add zomListMap method ...`（#82）——dev 侧 `2e1b537` 同主题，不回迁                              |
| `6a0e8a9`             | `修复三个bug`（#79）——dev 侧 `922d523` 同主题，不回迁                                                         |
| `6078b7b` / `88d5800` | en_ud 颠倒英语修复（port/`6078b7b` ↔ dev/`88d5800` 镜像）                                                     |

### 3.3 两侧主题汇总对比

| 维度         | `port/1.21.1`                                                          | `dev/26.1`                                                                              |
|--------------|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| 平台         | 冻结在 MC 1.21.1 / NeoForge 21.1.226                                   | 升级链至 MC 26.1.2 / NeoForge 26.1.2.76                                                 |
| 模块建设     | yukkuri（整模块）、wheel、网络库落地                                   | rendering / rpc / sync / explosion / font / collision / space-select / renderdoc-loader |
| 构建         | 各模块独立构建脚本（不回迁）                                           | `module.gradle` 聚合 + `-neoforge-26.1` 坐标 + toml 版本目录（dev 特有）                |
| multiblock   | 动态系统原创、异步检查、datagen 本地化                                 | 移植后演进：成形/解体事件（#69）、未加载区块快照修复（#73）、BlockPos 状态              |
| bugfix 密度  | 高（网络打包、mixin 崩溃、鱼缸加工 ×2、PoseStack、ClientTickRecorder） | 高（误删注册表项、SDF AMD/Mac、Iris 兼容、log spam、gnome/xwayland 崩溃）               |
| 代码质量动作 | UtilityClass 注解化、license MIT                                       | @NullMarked（jspecify）统一、@ApiStatus.Internal、MIT                                   |

---

## 4. 模块清单对比

基于 `git ls-tree --name-only <分支>` 实测（见附录 A）。模块目录前缀均为 `module.*`；`module.gradle` 为 dev 侧构建聚合文件，不属于模块。

### 4.1 dev/26.1 独有模块（8 项）

| 模块                  | 一句话职责                                                                                                                                   |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `module.collision`    | AABB 与三角形相交检测（`82b4f6a` 引入）                                                                                                      |
| `module.explosion`    | 爆炸模拟与优化（`435e23f` 引入）                                                                                                             |
| `module.font`         | 自定义文字渲染（`1d56a4e` 引入）                                                                                                             |
| `module.rendering`    | SDF 渲染管线、GUI 渲染、Bloom 后处理、RenderDoc 集成（`7121bf6` 等）                                                                         |
| `module.rpc`          | 注解式远程方法调用（`d153e58` 引入）                                                                                                         |
| `module.space-select` | 空间选择与 DistrictKey 管理（`1335fc5` / `e75b10f`）                                                                                         |
| `module.sync`         | 同步框架：SyncManager、SyncConfigManager、payload 处理，含独立 `processor/` 子模块（`3ce2611` / `1063ee9`）                                  |
| `renderdoc-loader`    | RenderDoc 原生加载器（`renderdoc-loader/src/main/java/dev/anvilcraft/lib/renderdoc/loader/RenderDocAgent.java`，与 `module.rendering` 配套） |

### 4.2 port/1.21.1 独有模块（1 项）

| 模块             | 一句话职责                                                                                                            | 状态          |
|------------------|-----------------------------------------------------------------------------------------------------------------------|---------------|
| `module.yukkuri` | 油库/汽化 API（`569e01d` #88 引入，14 个 Java 文件 + build.gradle + gradle.properties + META-INF/mods.toml + README） | **回迁项 #1** |

### 4.3 共享模块（12 项）

| 模块                           | 结论（详见第 5 章）              | 回迁项     |
|--------------------------------|----------------------------------|------------|
| `module.codec`                 | dev 超集                         | 无         |
| `module.config`                | dev 演进                         | 无         |
| `module.integration`           | dev 演进                         | 无         |
| `module.main`                  | dev 演进 + port 侧 2 行回迁      | #8         |
| `module.moveable-entity-block` | 同源重写，无需回迁               | 无         |
| `module.multiblock`            | dev 演进 + 可选回迁              | #6（可选） |
| `module.network`               | dev 超集                         | 无         |
| `module.recipe`                | dev 演进 + 2 处 bugfix 回迁      | #2、#3     |
| `module.registrum`             | dev 演进                         | 无         |
| `module.test`                  | dev 演进 + multiblock 测试组回迁 | #7         |
| `module.util`                  | dev 演进 + 2 处回迁              | #4、#5     |
| `module.wheel`                 | dev 超集                         | 无         |

---

## 5. 逐模块差异分类总表

### 5.1 共享模块差异分类总表

对每个共享模块执行 `git diff port/1.21.1 dev/26.1 -- module.xxx`（见附录 B）并逐文件核对后，分类结论如下。标注【需回迁】的模块存在
port 侧独有、dev 侧缺失的内容；标注【dev 超集/同源】的模块无需回迁。

| 模块                           | 结论                            | 关键依据                                                                                                                                                                                                               | 回迁项         |
|--------------------------------|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|
| `module.codec`                 | **dev 超集**                    | port 侧 `0b56109`/`50aeac7`/`6a0e8a9` 均有 dev 同主题提交（`3b5dfea`/`2e1b537`/`922d523`）；dev 另有 `173a454`（portable Codec/MapCodec 方法）、`52c3f8b`/`cf75837`（PositionPredicate AT 补充）、`3b5dfea` 等独有内容 | 无             |
| `module.network`               | **dev 超集**                    | port 侧网络提交与 dev 早期提交同主题镜像（附录 C）；dev 侧继续演进：`20721b1`（泛型检查优化）、`5733a9d`（发包工具）、`6220427`（PacketProtocol 公开）、`395495d`（双向注册 handler 参数）                             | 无             |
| `module.config`                | **dev 演进**                    | dev 侧独有：`3d50d76`（Config 注解 group 属性）、`c0a619b`（配置管理器模组容器获取修复）、`946938f`/`1ddfd24`（配置管理增强与文档）；port 侧无独有内容                                                                 | 无             |
| `module.integration`           | **dev 演进**                    | dev 侧独有：`dc4d583`（data loader 重命名与类型处理）、`e8efbd4`（实例计数器自增）；port 侧无独有内容                                                                                                                  | 无             |
| `module.registrum`             | **dev 演进**                    | dev 侧大幅演进（见主题 D5）；port 侧无独有内容                                                                                                                                                                         | 无             |
| `module.wheel`                 | **dev 超集**                    | 两侧同主题提交（port `18834b5`/`3d0a364` ↔ dev `0d73ab0`/`1ae3411`），dev 侧在 26.1 API 上等价实现；port 侧无独有内容                                                                                                  | 无             |
| `module.moveable-entity-block` | **同源重写，无需回迁**          | dev 侧 `83477ab`/`b5b27aa`/`2d4cd84` 以 26.1 渲染架构重写；port 侧 `1acdcf8`（PoseStack 未清空）在新架构下天然规避；`53eaa11`/`9c437b1` 与 dev `2d4cd84`/`b5b27aa` 对应                                                | 无             |
| `module.util`                  | **dev 演进 + 2 处回迁**         | port 独有：`9874db7`（ClientTickRecorder 修复）、`5fc6f65` 中的 `BlockStatePredicate.Builder.with(BlockState)`；其余 port 提交（`9d61997`/`c8ee900`/`0efcd3d`/`95bd5ca`）dev 侧已有等价或超集实现                      | **#4、#5**     |
| `module.recipe`                | **dev 演进 + 2 处 bugfix 回迁** | port 独有：`edd71ec`（InWorldRecipe 谓词栈回滚）、`e78d020`（ItemResourceHandlerCacheElement.sync() 短路/差量写入）；dev 侧其余为演进（`2033fd9` 资源处理器缓存、`0d0649f` 配方代码优化等）                            | **#2、#3**     |
| `module.multiblock`            | **dev 演进 + 可选回迁**         | dev 侧为移植后的超集（事件、快照修复、RegistryAccess）；port 独有：`5fc6f65` 中 `MultiblockDefinition` 的 16 个 `BlockState` 重载——dev 侧是否保留待与维护者确认（见 #6）                                               | **#6（可选）** |
| `module.test`                  | **dev 演进 + 测试组回迁**       | dev 侧 module.test 为 26.1 新结构；port 侧独有的 multiblock 动态系统测试组（TestControllerBlock + LibBlocks/LibItemGroups/LibMultiblockControllers/LibMultiblocks + datagen 注册）在 dev 侧缺失                        | **#7**         |
| `module.main`                  | **dev 演进 + 2 行回迁**         | port 独有：`1311782` 在 publishMods 块声明的 `clientRequired=true` / `serverRequired=true` 两行 CurseForge 环境配置；dev 侧 publishMods 块无此两行                                                                     | **#8**         |

### 5.2 回迁项详解（canonical 编号 #1–#8）

> 编号、来源提交、落点与难度为既定事实（两侧调研一致）；「迁移步骤 / 验收」为执行建议，供迁移计划文档引用。

#### #1 `module.yukkuri` 整模块（高 · 最大项）

- **内容**：`module.yukkuri` 全部 14 个 Java 文件 + `build.gradle` + `gradle.properties` + `META-INF/mods.toml` + README。
- **来源提交**：`569e01d`（#88，添加油库）、`18b29ca`（#89，CI 发布顺序——CI 配置部分不回迁，见 5.4）。
- **落点**：dev/26.1 新模块 `module.yukkuri`（按 dev 侧 `module.gradle` 聚合结构落位，注册到根级构建）。
- **API 适配点**（已核实，见 5.3）：
    - `ResourceLocation` → `Identifier`（26.1 重命名）；
    - 流体能力 API 二选一： (A) 继续使用已弃用的 `IFluidHandler`/`FluidStack`（可编译、有告警）； (B) 适配新
      `ResourceHandler<FluidResource>`（推荐，与 dev/26.1 物品能力迁移方向一致）；
    - `Level.getCapability(...)` 调用签名核对（26.1 下 capability 查询方式有调整，需对照
      `BlockCapability.createSided(Identifier, Class<T>)` 现状）。
- **迁移步骤**：① 从 `port/1.21.1:module.yukkuri/` 导出全部源码与资源 → ② 按 dev 侧包结构（`dev.anvilcraft.lib.v2`
  体系）落位 → ③ 全局替换 `ResourceLocation`/`Identifier` → ④ 按选项 B 重写流体交互层 → ⑤ 注册进 `module.gradle`
  聚合与主模块依赖 → ⑥ 编译 + 运行时冒烟。
- **验收**：`gradlew :module.yukkuri:build` 通过；游戏内油库/汽化功能在 26.1.2 客户端与服务器端可正常交互；无弃用 API 告警（若选
  B）。
- **难度**：高（涉及 26.1 流体新 API，需对照 sources jar 逐方法适配）。

#### #2 `module.recipe`：`InWorldRecipe.matches()` 谓词栈回滚（低）

- **来源提交**：`edd71ec`（#83）。
- **落点**：`module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java`（dev 侧同名文件）。
- **内容**：`matches()` 中谓词计算栈的回滚修复（纯逻辑搬运，无 API 变更）。
- **适配点**：无；两侧文件同名同包，按 diff 直接搬逻辑。
- **验收**：port 侧 #83 对应的鱼缸加工回归场景在 dev 侧复现测试通过（dev 侧 module.test 补一条回归用例）。

#### #3 `module.recipe`：`ItemResourceHandlerCacheElement.sync()` 短路/差量写入（中）

- **来源提交**：`e78d020`（#84）。
- **落点**：`module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/item/ItemResourceHandlerCacheElement.java`
  （port 侧原名 `ItemHandlerCacheElement.java`，dev 侧已随 `2033fd9` 改名并换用资源处理器缓存体系）。
- **内容**：`sync()` 中避免无谓写入的短路与差量写入逻辑。
- **适配点**：dev 侧 `ResourceHandler` 无 `setResource` 方法，需改为在 Transaction 内 extract + insert 实现差量写入（与 dev
  侧现有缓存写入模式对齐）。
- **验收**：缓存同步逻辑在 dev 侧单元/集成测试通过；无空转写入路径（可日志/断言验证调用次数）。

#### #4 `module.util`：`ClientTickRecorder` 修复（低）

- **来源提交**：`9874db7`（主菜单计 tick / 退出归零修复）。
- **落点**：`module.util/src/main/java/dev/anvilcraft/lib/v2/util/ClientTickRecorder.java`（dev 侧同路径）。
- **内容**：仅当 level 非空时记录 tick；退出时归零（`9874db7` 为 +9/-2 行的小修复）。
- **适配点**：保留 dev 侧 `@EventBusSubscriber(modid = AnvilLibUtil.MOD_ID, ...)` 写法， **不要**搬 port 侧无 modid 的写法。
- **验收**：主菜单不累计 tick、进世界正常计数、退出世界归零；dev 侧该文件编译通过。

#### #5 `module.util`：`BlockStatePredicate.Builder.with(BlockState)`（低–中）

- **来源提交**：`5fc6f65`（与 #6 同提交）。
- **落点**：`module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/BlockStatePredicate.java`。
- **内容**：恢复 `Builder.with(BlockState)` 便捷重载（16 个 `BlockState` 重载的底层支撑）。
- **适配点**：port 实现使用 `Property.getName(T)`（26.1 已移除）→ 改用 dev 侧现有泛型重载 `with(Property<T>, T)` 实现。
- **联动决策**：与 #6 联动——若 #6 判定不迁（dev 有意精简 API），#5 应一并放弃；反之 #5 是 #6 的前置。
- **验收**：`BlockStatePredicate.Builder` 以 `BlockState` 入参构造谓词编译通过并可正确匹配。

#### #6 `module.multiblock`：`MultiblockDefinition` 的 16 个 `BlockState` 重载（中 · 可选）

- **来源提交**：`5fc6f65`。
- **落点**：
  `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/definition/MultiblockDefinition.java`（dev
  侧同路径）。
- **内容**：16 个以 `BlockState` 直接入参的重载构造/方法。
- **前置**：依赖 #5 恢复 `with(BlockState)`。
- **决策点**：若判定 dev 侧删除是有意的 API 精简（`@ApiStatus.Internal` 化趋势，见 `c72aa30`），则 **放弃整项**——保留 dev
  现状，不引入 port 侧便捷层。
- **验收**（若迁）：dev 侧 `MultiblockDefinition` 以 `BlockState` 入参编译通过；动态多方块测试组（#7）覆盖该路径。

#### #7 `module.test`：multiblock 动态系统测试组（中–高）

- **内容**：port 侧独有的动态多方块测试组：`TestControllerBlock` + `LibBlocks` / `LibItemGroups` /
  `LibMultiblockControllers` / `LibMultiblocks` + datagen 注册。
- **落点**：按 dev 侧 `module.test` 新结构落位（dev 侧 module.test 已按 26.1 重构）。
- **适配点**：`@Mod` 构造器去 ModContainer 参数（26.1 签名变化）；`REGISTRUM.addDataGenerator(...)` 替代 GatherDataEvent（dev
  侧现有注册模式）；`ResourceLocation` → `Identifier`。
- **验收**：dev 侧 `:module.test` 构建与测试运行通过，动态多方块成形/解体用例覆盖 #2/#6 涉及的逻辑。

#### #8 `module.main`：curseforge 环境两行（低）

- **来源提交**：`1311782`（#90）。
- **落点**：`module.main/build.gradle` 的 `publishMods` 块。
- **内容**：`clientRequired = true` / `serverRequired = true` 两行 CurseForge 环境声明。
- **适配点**：按 dev 侧 `publishMods` 现有结构落位（`ae2acd0` 引入的发布配置）； **不要**把 port 侧 `jarJar(api ...)` 写法搬回。
- **验收**：`module.main` 发布任务 dry-run 通过，CurseForge 元数据含环境声明。

### 5.3 油库（yukkuri）流体能力 API 迁移选项（已核实事实）

以下事实来自本地 NeoForge **26.1.2.76 sources jar**（`neoforge-26.1.2.76-sources.jar`，路径见附录 D），可直接作为 #1 的适配依据：

1. **旧 API 仍存在但已弃用**：`net.neoforged.neoforge.fluids.capability.IFluidHandler` 与
   `net.neoforged.neoforge.fluids.FluidStack` 仍可编译，但 `IFluidHandler` 标注
   `@Deprecated(since = "1.21.9", forRemoval = true)`，并提供了迁移包装器 `IFluidHandler.of(ResourceHandler)`。
2. **新 API**：
    - `net.neoforged.neoforge.transfer.ResourceHandler<T>`：方法为 `getResource` / `extract` / `insert` / `isValid` /
      `getCapacityAsInt` / `size`， **无 `setResource`**；
    - `net.neoforged.neoforge.transfer.fluid.FluidResource`：不可变，实现 `DataComponentHolderResource<Fluid>`，静态字段
      `CODEC` / `OPTIONAL_CODEC` / `STREAM_CODEC` / `EMPTY`。
3. **Capability 查询**：`BlockCapability.createSided(Identifier, Class<T>)` 在 26.1 仍存在，仅标识符类型改为 `Identifier`。

| 选项                 | 做法                                                                 | 代价                                                                   | 建议               |
|----------------------|----------------------------------------------------------------------|------------------------------------------------------------------------|--------------------|
| A 最小移植           | 继续使用已弃用的 `IFluidHandler` / `FluidStack`                      | 可编译但有弃用告警；未来 `forRemoval` 后必须再迁                       | 不推荐（仅作兜底） |
| B 适配新 API（推荐） | 改用 `ResourceHandler<FluidResource>`，Transaction 内 insert/extract | 与 dev/26.1 侧物品能力迁移方向（`2033fd9` 资源处理器化）一致，一步到位 | **推荐**           |

### 5.4 不回迁清单与理由

| 内容                                                                                                             | 理由                                                                                                                                                                                                         |
|------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `module.codec` / `module.network` / `module.config` / `module.integration` / `module.registrum` / `module.wheel` | dev 侧为超集或已独立演进（见 5.1 各模块行）                                                                                                                                                                  |
| `module.moveable-entity-block`（含 `1acdcf8` PoseStack 修复）                                                    | dev 侧以 26.1 渲染架构同源重写（`83477ab` 等），port 侧 bugfix 在新架构下天然规避                                                                                                                            |
| port 侧各模块独立构建脚本、`gradle.properties`                                                                   | 已被 dev 侧 `module.gradle` 聚合 + toml 版本目录取代（`ea4301f`），回迁即倒退                                                                                                                                |
| lang datagen 产物（`zh_cn.json` / `en_us.json`）                                                                 | dev 侧统一由 datagen 生成（`e3ba2cd` 起块物品自行提供 lang），直接提交产物与 dev 工作流冲突                                                                                                                  |
| `module.multiblock/ASYNC_MULTIBLOCK_CHECK_PLAN.md`                                                               | 文档已标注「已完成实现」；dev 侧异步检查实现等价且更完善（`d888714` 等），建议不迁移                                                                                                                         |
| `18b29ca`（CI 发布顺序）                                                                                         | CI 体系 dev 侧已整体重构（`aa59e37`/`7f27908`），port 侧 CI 片段不适用                                                                                                                                       |
| dev 侧特有演进（不回迁方向）                                                                                     | 动态多方块事件 `1e5bfb3`（#69）、未加载区块快照修复 `d888714`（#73）、@NullMarked 统一 `f94126f`、`module.gradle` 构建体系 `ea4301f`、@ApiStatus.Internal `c72aa30`（#93）——均为 dev 独有，port 侧不反向吸收 |

---

## 6. 附录：研究方法

### A. merge-base 分析

```bash
# 共同祖先与两侧 HEAD
git merge-base port/1.21.1 dev/26.1          # → 81c5a9c5246a8d3642ea5cfddaf5298f885f7872
git rev-parse port/1.21.1 dev/26.1           # → fab551bbda... / 0b7e45f1bd...

# 双向独有提交计数
git rev-list --count port/1.21.1..dev/26.1   # → 170（dev 独有）
git rev-list --count dev/26.1..port/1.21.1   # → 46（port 独有）

# 模块清单（顶层 module.* 目录）
git ls-tree --name-only dev/26.1 | grep '^module\.'
git ls-tree --name-only port/1.21.1 | grep '^module\.'
```

解读：merge-base 之后两侧完全分叉；dev 独有 170 提交中前段为版本升级链，port 独有 46 提交中约一半为 dev 早期提交的镜像（见附录
C）。

### B. 双向 diff（逐模块差异分类）

对每个共享模块执行双向 diff，人工核对「port 独有 vs dev 独有」的实质内容：

```bash
# 两侧差异（粗粒度定位）
git diff port/1.21.1 dev/26.1 -- module.util
# port 侧独有修改（回迁候选）
git diff port/1.21.1..dev/26.1 -- module.util    # 即 dev 相对 port 的变更
# 按文件核对提交归属
git log --oneline dev/26.1..port/1.21.1 -- module.recipe
```

差异分类规则：

| 分类          | 判据                                                              | 动作                  |
|---------------|-------------------------------------------------------------------|-----------------------|
| port 独有内容 | port 侧有修改/文件，dev 侧无对应                                  | 进入回迁候选（#1–#8） |
| dev 超集      | dev 侧包含 port 侧全部实质内容且有更多演进                        | 不回迁                |
| 同源重写      | 两侧同一功能在不同 API 架构上分别实现（如 moveable-entity-block） | 不回迁                |
| 同主题镜像    | port 提交与 dev 早期提交主题一致（如网络库、wheel）               | 不回迁，以 dev 为准   |

### C. 提交对照（port 独有 46 提交的镜像/独有判定）

对 `dev/26.1..port/1.21.1` 的 46 条提交逐条与 dev 侧 170 条比对主题后，同主题镜像关系如下（节选）：

| port 提交                                                                         | 镜像主题                               | dev 侧对应提交                                                                    |
|-----------------------------------------------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------|
| `a222300` / `19d98e3` / `10069a0` / `0d6c1b4` / `c7a790b` / `00d65d0` / `6d2741f` | 网络库落地/优化/CI/打包/mixin/自动注册 | `8d8ac8c` / `548cecd` / `0fc80e5` / `baac9a0` / `d85e3d5` / `0e86a9d` / `09e435c` |
| `18834b5` / `3d0a364`                                                             | wheel 模块                             | `0d73ab0` / `1ae3411`                                                             |
| `bcefcb9`                                                                         | license LGPL→MIT                       | `6fb6335`                                                                         |
| `a74671a`                                                                         | README 格式                            | `371d5db`                                                                         |
| `1dca02f`                                                                         | bump 2.0.0                             | `65a605d`                                                                         |
| `6078b7b` / `109a935`                                                             | en_ud 修复 / 风格优化                  | `88d5800` / `42f816d`                                                             |
| `23719fb` / `d88b8a6`                                                             | BoolAndInt 编解码                      | `3fc6054` / `8e01120`                                                             |
| `d7985f6` / `f2b60b6`                                                             | ItemCache 范围/空栈处理                | `ec16ccb` / `1c523ee`                                                             |
| `53eaa11` / `9c437b1`                                                             | moveable entity block                  | `83477ab` / `2d4cd84`                                                             |
| `0b56109`                                                                         | DamageSourcePredicate StreamCodec      | `3b5dfea`                                                                         |
| `50aeac7`                                                                         | zomListMap                             | `2e1b537`                                                                         |
| `6a0e8a9`                                                                         | 三个 bug 修复                          | `922d523`                                                                         |

判定为 port 真正独有（无 dev 对应）的提交：`5fc6f65`（动态多方块系统，dev 经 `7019b38` 移植后已改写）、`ba395e7` / `2634e49` /
`3832895` / `4aa9d13`（异步检查/错误处理/位置修正/datagen 本地化——其中部分已被 dev 侧等价实现吸收）、`9874db7`
（ClientTickRecorder 修复）、`edd71ec` / `e78d020`（鱼缸加工 bugfix ×2）、`569e01d` / `18b29ca` / `1311782` / `fab551b`
（yukkuri 系列）、`9d61997` / `c8ee900` / `0efcd3d` / `95bd5ca`（util 扩展——dev 侧已有等价或超集，不回迁）。

### D. 参考资料

| 资料                                                                                                                                                              | 位置                                                                                                                                                                                                                             |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 8 个并行子代理模块级调研报告（codec/util/network/recipe/registrum/multiblock/moveable-entity-block/main/config/integration/wheel/test 差异分类与 API 变化点明细） | `C:/Users/Administrator/.kimi-code/sessions/wd_anvillib_031cb785b7e3/session_6033f19b-f4ef-481a-b18c-d626cc22c314/agents/main/tool-results/AgentSwarm-call_00_nh47UIFAaqo9d96levr46332-594fb004-9f1f-4c9e-b230-43bc1440211d.txt` |
| NeoForge 26.1.2.76 sources jar（流体能力 API 核实依据）                                                                                                           | `C:/Users/Administrator/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/1d1dabe31afb953e46fb278e1cd8f04ab8e0d9a0/neoforge-26.1.2.76-sources.jar`                                                             |
| 只读 git 命令（随时可用）                                                                                                                                         | `git show <分支>:<路径>`、`git diff port/1.21.1 dev/26.1 -- <路径>`、`git log --oneline port/1.21.1..dev/26.1 -- <路径>`、`git grep -n <关键字> <分支> -- <路径>`                                                                |

### E. 待验证事项

| #  | 事项                                                                                                   | 验证方法                                                                 |
|----|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| V1 | #5/#6 的 `with(BlockState)` 在 26.1 的可用性（`Property.getName(T)` 已移除，改用泛型重载后行为等价性） | dev 分支上编译运行 `BlockStatePredicate` 相关测试                        |
| V2 | #1 选项 B 的 `ResourceHandler<FluidResource>` 迁移完整性（14 个 Java 文件逐一核对 API 用法）           | `gradlew :module.yukkuri:compileJava` 编译输出对照 26.1.2.76 sources jar |
| V3 | #7 测试组在 dev 侧 `module.test` 新结构的落位方式（`REGISTRUM.addDataGenerator` 注册模式）             | 对照 dev 侧 module.test 现有注册代码 + 构建运行                          |
| V4 | `module.yukkuri` 的 mods.toml 依赖声明与 `module.main` 聚合依赖是否需要调整                            | 检查 dev 侧其他模块的 mods.toml 依赖写法                                 |
| V5 | #8 两行配置在 dev 侧 `publishMods` 块的结构兼容性                                                      | `module.main` 发布任务 dry-run                                           |
