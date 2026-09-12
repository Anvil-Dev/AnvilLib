# 1.21.1 → 26.1 功能对照与移植记录

对比日期：2026-09-12。来源为本地 `fff/1.21.1`（`fe26ea6`），目标为当前 `fff/26.1-port`（起点 `f37471b`）。Minecraft 保持 `26.1.2`，NeoForge 保持 `26.1.2.76`，Java 保持 25。

两条分支共同祖先为 `81c5a9c`。`0f3b26b` 曾把 26.1 的大量功能回移植至 1.21.1，因此仅按提交是否存在或整文件替换会重复引入旧版本 API。此次结合提交历史、模块树、公开方法清单、源码差异和真实运行路径核对。

## 逐模块对照

| 模块 | 1.21.1 最新功能与 26.1 处理结果 |
| --- | --- |
| Codec | 保留已有的零一多列表、1–16 参数组合及条件流编解码；补齐 `encodeStart(MapCodec, ...)` 和 `tagKey(...)`。使用 26.1 的注册表、组件谓词和原生流编码。 |
| Collision | AABB/三角形数学碰撞能力一致，保留目标实现。 |
| Config | 注解配置、分组、嵌套配置、枚举翻译与点分隔键均已存在，保留目标 API。 |
| Cube | **新增完整模块**：凸多面体、BVH 拾取、斜棱线生成、后台轮廓缓存、旋转/反向元素、父模型、随机变体、multipart、资源重载、动态 BER 部件、超界邻格补扫和运行时目标排除规则。包含 `fe26ea6` 的缓存仿射逆矩阵与双精度坐标修复。 |
| Explosion | 分层球壳爆炸、熔化配方缓存及掉落实体处理已有；保留 26.1 配方结果和伤害 API。 |
| Font | 自定义字体、SDF 字形缓存与配置已有；保留 26.1 GUI 提取和着色器实现。 |
| Integration | 模组版本检测与分阶段集成已有；保留目标的客户端/服务端数据生成拆分及 FMLLoader API。 |
| Main | 聚合依赖与内嵌包加入 Cube；补齐 CurseForge 客户端/服务端必需环境声明。 |
| Moveable Entity Block | 活塞搬运、数据保留、回调与方块实体渲染已有；保留 26.1 ValueIO 与渲染状态实现。 |
| Multiblock | 异步快照、未加载区块复用、形成/解体事件已有；补齐 `BlockState` 构建器重载、公开 `DefinitionSerialization`，并在现有 SavedData Codec 中保存 `formed`，兼容缺失字段的旧目标存档。 |
| Network | 包自动注册、双向处理、公开协议类型和条件广播已有，保留新网络发送 API。 |
| Recipe | **新增共享方块约束剪枝**；补齐失败匹配的谓词回滚、成功匹配只清理本轮快照，以及输出槽同步差量。保留目标已有 `SaveComponentToTag` 泛型修复、ItemStackTemplate 和事务式资源 API。 |
| Registrum | **新增创造栏分区及变体折叠/选择**：横幅宽度、文字范围/对齐/滚动、提示、16 色识别、配置开关、提供器、4×4 叠加层、快捷栏/副手/丢弃/克隆操作与松开消费。补齐方块实体能力注册回调。现有 26.1 注册/模型/数据生成接口保持。 |
| Rendering | 1.21.1 已移除其旧版渲染模块；26.1 的缓存 BER、Bloom、Compute、SDF 与 Iris 接入继续保留，供 Font/Wheel 等模块使用。 |
| RPC | **新增同步阻塞调用**：`invokeSync` 的 0–16 参数及 `allowNonVirtual` 重载、`invokeSyncByName`、返回值、失败、超时和断连传播；补齐演示命令。 |
| Space Select | 选区、区块管理和网络同步已有；CI 拓扑修正为依赖 Network。 |
| Sync / processor | 字段、惰性差分和配置同步已有；保留 26.1 ClassProcessor SPI、ItemStackTemplate 和新版访问接口，不回退到 1.21.1 CoreMod。 |
| Util | 补齐 `BlockStatePredicate.with(BlockState)`、保守 `cannotMatch`、无世界时不累计客户端 tick 及退出重置。保留 26.1 `ItemInstance` 版 UnlimitedItemStack 和旧包兼容类。 |
| Wheel | 环扇效果已存在；移入 `712b747` 的跨 0° 选区判断，修复顺时针返回正上方时延迟选择。 |
| Test | 新增独立测试世界、真实模型/Mixin 加载、几何、创造栏交互、配方、持久化和 RPC 回环验证；普通测试运行保持原有外部 AnvilCraft 依赖。 |

Yukkuri 已由来源分支 `23fef16` 删除，最新树中不存在，不恢复已退休模块。RenderDoc 是目标已有内部开发工具，继续保留。

## 26.1 适配要点

- Cube 从旧 `BakedModel` 接入改为捕获 `SimpleModelWrapper` 的 `CuboidModelElement`，解析 `SingleVariant`、`WeightedVariants` 与 `MultiPartModel`。随机数消费顺序跟随 26.1 `WeightedList`，并支持实际烘焙矩阵中的欧拉旋转与根变换。
- 准星入口由 `GameRenderer.pick` 移至 `LocalPlayer.pick`；普通碰撞、服务端和投射物射线继续使用原行为。
- 高亮使用 `ExtractBlockOutlineRenderStateEvent`，提取不可变几何后绘制，遵循透明通道、实际线宽和高对比度设置；绘制回调不访问世界。
- 保留源版 16 MiB 模型几何预算、32,768 状态、32 部件、4,096 线段等限制与后台任务预算。
- 创造栏使用 `GuiGraphicsExtractor`、渲染层以及 `MouseButtonEvent` / `KeyEvent`。物品组在初始化时只保存定义，待默认组件绑定后才构造物品栈。面板在上方空间不足时改放下方，防止小窗口裁掉选项。
- 分区内分别折叠变体，防止前一区缩短导致后续横幅丢失。折叠/布局结果保持可修改，搜索内容不折叠。
- 剪枝保留现有高优先级先执行的行为，并用配方 ID 确定同优先级顺序；重复注册、移除或直接加入候选集不会导致错误提前终止。资源同步在事务中验证实际变化量，失败时回滚，避免半完成写入。

## 验证

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat :anvillib-test-neoforge-26.1:runClient -PportSmoke --console=plain
git diff --check
```

`-PportSmoke` 使用 `module.test/build/port-smoke-run`，不加载外部 AnvilCraft 测试依赖。创建独立超平坦创造世界，完成测试后保存并关闭。结果写入该目录的 `port-smoke-result.txt`；缺失或失败结果会使 Gradle 任务失败。界面截图写入 `screenshots/port-creative-variants.png`。

最终验证结果：

- 全模块 `build` 通过（258 个任务），包含编译、资源处理、JAR、源码/Javadoc 包和已有几何/布局检查。
- 独立客户端和集成服务器运行通过，`PORT_SMOKE_PASSED checks=327016`。包括 48 种实际模型旋转/镜像、两种视点高度、161,376 条顶面射线、远距离双精度变换及 4,096 个随机模型种子。
- 石头、橡木围栏、拉杆、砂轮、原木共 72 个状态全部成功捕获，几何占用 260,328 字节，无不支持状态。控制杆实景准星与高亮、运行时排除规则及资源重载后的恢复均通过；后台轮廓无失败或预算超限。
- 实际创造栏横幅、完整 16 色面板、左键选择、松开消费、右键关闭、滚动关闭和搜索内容保留均通过。已查看小窗口截图，16 个选项完整可见。
- 配方匹配快照回滚、共享约束剪枝、重复注册、同优先级候选、直接修改集合、拒绝插入的输出槽、多方块状态 Codec 和 RPC 返回值/异常/超时/断连测试通过。
- 客户端退出时集成服务器完成玩家、区块与全部维度保存。
- 聚合 JAR 内嵌 `anvillib-cube-neoforge-26.1` 的元数据及实际 JAR 已核对。CI 矩阵生成通过，Cube 位于第 0 层，Space Select 位于 Network 之后的第 1 层。
- `git diff --check` 通过。移植保留在 `fff/26.1-port`，未发布。

实际多人网络、其他平台和外部模组组合不包含在本次自动回归范围内。
