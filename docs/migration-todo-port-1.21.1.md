# AnvilLib 回移植执行跟踪：`dev/26.1` → `port/1.21.1`

> 本文件是 `docs/migration-plan-port-1.21.1.md`（#P1–#P19）的执行跟踪清单（另有计划外追加项 #P20，见「逐项日志」末尾）。每完成一项，将对应状态更新为「✅
> 已完成」并记录提交号；未完成项保持「⬜ 未开始 / 🔄 进行中」。待验证事项（附录 B 的 V-1…V-18）在验证后同步销账。

- 基线：`port/1.21.1`（执行起点 `96c3a34f`，计划基线 `bd25229`）
- 来源：`dev/26.1` @ `0b7e45f`
- 工具链：Java 21 / NeoForge 21.1.226 / Minecraft 1.21.1

---

## 执行状态总览

| #P   | 内容                                                         | 难度  | 状态      | 提交       |
|------|--------------------------------------------------------------|-------|-----------|------------|
| #P1  | module.rpc 整模块（25 文件）                                 | 低    | ✅ 已完成 | `26b11e18` |
| #P2  | module.sync + processor（CoreMod 重写）                      | 中-高 | ✅ 已完成 | `c7da98c1` |
| #P3  | module.explosion 整模块（10 文件）                           | 低    | ✅ 已完成 | `e896a1ea` |
| #P4  | module.collision 整模块（3 文件）                            | 极低  | ✅ 已完成 | `c25dd465` |
| #P5  | module.space-select 整模块（18 文件）                        | 中    | ✅ 已完成 | `1706feaa` |
| #P6  | module.font 整模块（25 文件）                                | 中    | ✅ 已完成 | `91533730` |
| #P7  | module.rendering（#P7a–#P7e 分批）                           | 高    | ✅ 已完成（已移除）| `b0e179e8` + `a6544e6b` |
| #P8  | renderdoc-loader（2 文件）                                   | 低    | ✅ 已完成（已移除）| `c62a262e` |
| #P9  | module.config：group + TranslatableEnum + TOML 点分隔        | 低    | ✅ 已完成 | `65aa5e11` |
| #P10 | module.integration：数据加载拆分 + meter 递增                | 低    | ✅ 已完成 | `adc23468` |
| #P11 | module.network：泛型修复 + Included + public                 | 低    | ✅ 已完成 | `20934ac4` |
| #P12 | module.util：OutlineUtil + HolderGetter + withCount + equals | 中    | ✅ 已完成 | `164f441f` |
| #P13 | module.registrum：14 种新 builder/entry                      | 低    | ✅ 已完成 | `2d1a8e3d` |
| #P14 | module.registrum datagen 增强                                | 中    | ✅ 已完成 | `dd8d79e5` |
| #P15 | module.multiblock：事件化 + 快照复用 + 键化 + 懒解析         | 中    | ✅ 已完成 | `2556a8e3` |
| #P16 | module.recipe：SpawnItem 守卫 + SetBlock nbt                 | 低    | ✅ 已完成 | `71bf9e54` |
| #P17 | module.wheel：环形扇区选择效果                               | 中-高 | ✅ 已完成 | `142e3296` |
| #P18 | module.test：T2/T8/T9/T10 可选测试                           | 低-中 | ✅ 已完成 | `a4ab3acd` |
| #P19 | 构建体系：roseauCheck                                        | 中    | ✅ 已完成（运行待补验） | `544c9eb0` |
| #P20 | module.codec：CodecUtil 便携 create/mapCodec（计划外追加）     | 低    | ✅ 已完成（待提交） | 来源 `173a454` |

> ⚠️ 2026-08-12 状态更新：#P7（module.rendering）与 #P8（renderdoc-loader）已从 `port/1.21.1` 分支整体移除；同时清理了 `module.font` / `module.main` 的渲染依赖、CI 工作流（ci / release / pull_request）中的渲染任务与相关文档引用。

## 执行顺序（计划第 6 章）

```
#P1/#P2 →（#P7a）→ #P6 → #P3/#P4/#P5/#P8 → #P9 → #P11/#P12/#P13/#P16 → #P15 → #P17 → #P14 → #P18 → #P19
```

## 逐项日志

### #P1 module.rpc（低）

- [ ] 提取 25 个 Java 文件
- [ ] build.gradle / settings.gradle / mods.toml 落位
- [ ] 3 处 API 反替换（Identifier / FMLLoader / accessFlags）
- [ ] `gradlew :module.rpc:compileJava` 通过
- [ ] 提交（conventional commit）

### #P2 module.sync + processor（中-高）

- [ ] 主模块 25 文件提取 + 反替换
- [ ] SyncConfigManager 等价实现（getSecureJar ().getPath）
- [ ] processor 6 文件 + CoreMod 重写（ICoreMod + ITransformer<ClassNode>）
- [ ] `gradlew :module.sync:compileJava` 通过，`grep neoforgespi.transformation` 零残留
- [ ] 提交

### #P7 module.rendering（高 · 分批）

> ⚠️ 2026-08-12：该模块已从 `port/1.21.1` 移除，本清单仅保留历史记录。

- [ ] #P7a 纯逻辑 19 项 + 模块骨架
- [ ] #P7b 后处理三件套（Bloom/GaussianBlur/Glitch）
- [ ] #P7c SDF GUI
- [ ] #P7d CachedBER
- #P7e Compute
- [ ] #P7 收尾（shader json、mixins.json、AT、mods.toml）
- [ ] 提交（按批）

### #P6 module.font（中）

- [ ] 9 项直接移植 + 8 项反向适配
- [ ] `gradlew :module.font:compileJava` + runData 通过
- [ ] 提交

### #P3 module.explosion（低）

- [ ] 10 文件提取 + 6 处反替换
- [ ] build.gradle（依赖 port module.config，自建 run）
- [ ] `gradlew :module.explosion:compileJava` + runData 通过
- [ ] 提交

### #P4 module.collision（极低）

- [ ] 3 文件提取（零 MC/NF API）
- [ ] 提交

### #P5 module.space-select（中）

- [ ] 18 文件提取 + 反替换
- [ ] DistrictRenderer 渲染路径重写（LevelRenderer.renderVoxelShape）
- [ ] `gradlew :module.space-select:compileJava` 通过
- [ ] 提交

### #P8 renderdoc-loader（低）

> ⚠️ 2026-08-12：该模块已从 `port/1.21.1` 移除，本清单仅保留历史记录。

- [ ] 2 文件提取 + 构建脚本改写（Java 21）
- [ ] 提交

### #P9 module.config（低）

- [ ] Config/ConfigManager/ConfigRecord group 支持
- [ ] TranslatableEnum + ConfigData 枚举分支
- [ ] FormattingUtil.toPointSplitName + TOML key 点分隔
- [ ] `gradlew :module.config:compileJava` + runData 通过
- [ ] 提交

### #P10 module.integration（低 · 破坏性 API）

- [ ] applyData → applyClientData/applyServerData 拆分
- [ ] meter.increment ()
- [ ] `gradlew :module.integration:compileJava` 通过
- [ ] 提交（commit 说明注明破坏性 API）

### #P11 module.network（低）

- [ ] PacketData 泛型检查修复
- [ ] NetworkUtil Included 方法 + Objects.equals
- [ ] PacketProtocol public
- [ ] `gradlew :module.network:compileJava` 通过
- [ ] 提交

### #P12 module.util（中）

- [ ] OutlineUtil + Line + ShapeUtilJoinTimingTest
- [ ] 3 个 predicate HolderGetter 重构 + module.recipe 4 个调用方
- [ ] withCount + @EqualsAndHashCode
- [ ] 保留 port `with(BlockState)`（不可照搬 dev 删除）
- [ ] `gradlew :module.util:compileJava` + `:module.recipe:compileJava` + 测试通过
- [ ] 提交

### #P13 module.registrum（低）

- [ ] 直接移植 10 项 + 反向适配 9 项（RecipeSerializer 匿名类、VillagerProfession 6 元、Attachment Codec 等）
- [ ] 两个零碎修复（item 默认 lang、OneTimeEventReceiver 空值）
- [ ] 不迁 dataComponentPredicate 入口
- [ ] `gradlew :module.registrum:compileJava` 通过
- [ ] 提交

### #P14 module.registrum datagen（中）

- [ ] RecipeProvider 方法 public 化（公开包装内调静态方法）
- [ ] dataMap (provider) 重载
- [ ] `gradlew :module.registrum:compileJava` + runData 通过
- [ ] 提交

### #P15 module.multiblock（中）

- [ ] M1 DynamicMultiblockEvent（可取消事件）
- [ ] M2 未加载区块快照复用
- [ ] M3 Long→BlockPos 键化（含 Java 21 lambda `_` 改名）
- [ ] M4 定义懒解析（ResourceKey + registryAccess）
- [ ] M7 Config.group（依赖 #P9）
- [ ] `gradlew :module.multiblock:compileJava` + `:module.recipe:compileJava` 通过
- [ ] 提交

### #P16 module.recipe（低）

- [ ] R1 SpawnItem 零数量守卫（可选 R2 必填校验）
- [ ] R3 SetBlock nbt 默认值（null → 空 CompoundTag）
- [ ] `gradlew :module.recipe:compileJava` 通过
- [ ] 提交

### #P17 module.wheel（中-高）

- [ ] WheelSelectionEffect + WheelMenuBuilder/Model 链式方法
- [ ] WheelWidget.renderSelectionEffect + WheelScreen 适配
- [ ] annular_sector.fsh + ShaderInstance 注册（切断 rendering 依赖）
- [ ] `gradlew :module.wheel:compileJava` 通过
- [ ] 提交

### #P18 module.test（低-中）

- [ ] T2 配置测试（随 #P9）
- [ ] T8 RPC 测试（随 #P1）
- [ ] T9 爆炸测试（随 #P3）
- [ ] T10 wheel 按键扩展（随 #P17，KeyMapping 1.21.1 API）
- [ ] `gradlew :module.test:compileJava` 通过
- [ ] 提交

### #P19 构建体系（中）

- [ ] roseau.yaml + gradle/scripts/roseau.gradle + 根 build.gradle 接入
- [ ] `gradlew roseauCheck` 运行通过
- [ ] 提交

### #P20 module.codec：CodecUtil 便携 create/mapCodec（计划外追加 · 2026-08-11）

> 来源：`dev/26.1` 提交 `173a454`（feat(codec): add portable methods for creating Codec and MapCodec with varying parameters）
> 落点：`module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/CodecUtil.java`

- [x] 提取改动：16 个 `create`（Codec）+ 16 个 `mapCodec`（MapCodec）重载 + `App`/`Function3–16`/`StreamCodec` 导入
- [x] 冲突消解：26.1 `Identifier` → 1.21.1 `ResourceLocation`；保留 port 侧既有 `zomListMap`/`encodeStart` 并追加在其后
- [x] API 核实：1.21.1 DFU 6.0.8 含 `Function3–16`（javap 实测），无需反向替换
- [x] `gradlew :anvillib-codec-neoforge-1.21.1:compileJava --rerun-tasks` 通过
- [ ] 提交（conventional commit）

---

## 待验证事项销账（计划附录 B）

| 编号 | 事项                                                                               | 涉及项          | 状态   |
|------|------------------------------------------------------------------------------------|-----------------|--------|
| V-1  | `Mth.roundToward`/`VertexSorting`/`CompactVectorArray`/`MeshData.sortQuads` 存在性 | #P7a            | ✅ 已验证（roundToward 存在；CompactVectorArray 不存在改 VertexSorting.sort，V-1 计划修正） |
| V-2  | Iris 1.7.x `ImmediateState` 字段                                                   | #P7d            | ✅ 已验证（Iris 1.8.12 签名一致，改反射访问） |
| V-3  | `MultiBufferSource.BufferSource` 构造器参数                                        | #P7a            | ✅ 已验证（super(null,null) 兼容） |
| V-4  | `renderVoxelShape` 颜色语义                                                        | #P5             | 未验证（需 runClient 目测） |
| V-5  | port 侧 run 配置自建模式                                                           | #P3/#P5/#P8     | ✅ 已验证（#P3 runData 通过、#P5 processResources 通过） |
| V-6  | Iris 1.21.1 Modrinth 坐标                                                          | #P7d            | ✅ 已验证（Modrinth maven 停服，改反射访问，坐标注释占位） |
| V-7  | mods.toml 模板变量注入                                                             | #P7             | ✅ 已验证（port 模板展开正常） |
| V-8  | ShaderDefines → Map<String,String>                                                 | #P7e            | ✅ 已验证（编译通过） |
| V-9  | CoreMod jar-in-jar services 可见性                                                 | #P2             | 未验证（需打包启动实测） |
| V-10 | CoreMod GETSTATIC 类可解析性                                                       | #P2             | 未验证（需启动日志核对） |
| V-11 | `getSecureJar().getPath` 签名                                                      | #P2             | ✅ 已验证（javap 确认返回 Path） |
| V-12 | `FMLLoader.getCurrent().getGameDir()`                                              | #P6             | ✅ 已验证（改 FMLPaths.GAMEDIR.get() 编译+runData 通过） |
| V-13 | jspecify 在 port 构建链可用性                                                      | #P15（M6 可选） | ✅ 已验证（多模块 compileOnly 使用通过） |
| V-14 | VillagerProfession 6 元 record / Potion 构造                                       | #P13            | ✅ 已验证（编译通过） |
| V-15 | RecipeSerializer 匿名类编译                                                        | #P13            | ✅ 已验证（编译通过） |
| V-16 | District.color() int 打包颜色语义                                                  | #P5             | 未验证（需 runClient 目测） |
| V-17 | GatherDataEvent includeClient/getPackOutput 签名                                   | #P3/#P6/#P14    | ✅ 已验证（#P3 runData、#P6 runData 通过） |
| V-18 | 根构建脚本模块注册方式对齐                                                         | #P1–#P8         | ✅ 已验证（全部新模块注册并编译） |

## 遗留登记（环境限制未完成的运行类验收）

| 编号 | 事项 | 说明 | 复现/补验步骤 |
|------|------|------|----------------|
| L-1 | roseauCheck 运行验证（#P19） | 本机运行 roseauCheck 时 Gradle daemon 崩溃（疑似 roseau-cli 0.6.0 与本地 Java 21 环境兼容问题）；任务可注册、配置阶段通过 | CI 或人工：`./gradlew :anvillib-<模块>:roseauCheck` |
| L-2 | RPC 行为验证（#P1/#P18 T8） | 远程调用往返、@CallableParam 编解码、tick 超时 | runClient/runServer，`-Danvillib.test.rpc.auto` 系统属性 |
| L-3 | sync 行为验证（#P2） | @Sync 字段同步、LazySync、processor 字节码注入生效 | runClient 启动日志 + 反编译核对注入点 |
| L-4 | explosion 行为验证（#P3/#P18 T9） | 分层球壳破坏、熔化替换、实体 hurt | runClient/runServer 执行测试命令 |
| L-5 | space-select 渲染（#P5，V-4/V-16） | 线框绘制颜色/平移语义、滚轮交互、网络收发 | runClient 目测 |
| L-6 | font 运行验证（#P6） | SDF 文字渲染、anvillib$text 扩展、FontConfigScreen | runClient |
| L-7 | rendering 运行验证（#P7a） | ⚠️ 2026-08-12：module.rendering 已移除，不再适用 | — |
| L-8 | multiblock 行为验证（#P15） | 事件可取消回滚、快照复用、懒解析 | runServer |
| L-9 | wheel 运行验证（#P17） | 环形扇区效果、颜色配置、旧路径无回归 | runClient + T10 |
| L-10 | Iris 集成（#P7d，V-2/V-6） | ✅ 已解决：#P7d 完成，Iris 经反射访问（签名一致），运行时需装 Iris 客户端验证 | — |
| L-11 | Compute（#P7e，V-8） | ✅ 已解决：#P7e 完成（编译通过），dispatch/内存屏障运行验证待 runClient | — |
| L-12 | rendering 执行层运行验证（#P7b-e） | ⚠️ 2026-08-12：module.rendering 已移除，不再适用 | — |
