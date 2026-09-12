# 1.21.1 → 1.21.8 特性对照与移植记录

来源：本地 `fff/1.21.1`，`fe26ea6`。目标：当前 `fff/1.21.8-port`，起点 `7894b97`。共同祖先 `81c5a9c`。

核对提交历史、所有模块、源文件和资源；参考本地 `fff/1.21.11-port` 的现代模型/GUI 实现，再按 1.21.8 的实际 API 适配。保留 Minecraft **1.21.8**、NeoForge **21.8.53**、Java **21**。

## 逐模块对照

| 模块 | 移植结果 |
| --- | --- |
| Codec | 迁入 1–16 参数组合、条件流编解码、零/一/多元素列表、MapCodec 编码和标签键工具；保留新版组件谓词与注册表编码。 |
| Collision | 新增 AABB、三角形和三角形之间的碰撞检测及相关数学工具。 |
| Config | 补齐配置分组、嵌套对象、枚举翻译、点分隔配置键及格式化处理。 |
| Cube | 新增完整精确模型拾取与斜棱线模块：凸几何、BVH、轮廓缓存、父模型、旋转/镜像、随机变体、multipart、动态部件、邻格补扫、资源重载和运行时排除规则。包含最新仿射逆矩阵与双精度变换修复。 |
| Explosion | 新增分层球壳爆炸、熔化配方缓存、方块破坏和掉落处理。 |
| Font | 新增系统字体选择、SDF 字形图集与缓存、测量/布局、GUI 绘制和配置界面。 |
| Integration | 补齐分阶段集成和加载判断，数据生成接入适配 Client/Server 两类事件。 |
| Main | 聚合全部功能模块与同步处理器；本地项目依赖与发布依赖均导出 API，补齐发布环境声明。 |
| Moveable Entity Block | 保留活塞搬运、方块实体数据、回调与渲染状态；改用实际调用参数和类型序号捕获局部变量，消除依赖其他映射变量名造成的启动失败。 |
| Multiblock | 新增动态多方块、异步快照检查、未加载区块处理、形成/解体事件、控制器、配置和网络同步；保留公开定义序列化、BlockState 构建器和 formed 持久化。 |
| Network | 补齐 BoolAndInt、条件广播、公开协议和自动注册改进。 |
| Recipe | 迁入方块约束搜索树剪枝、匹配快照回滚、组件保存、缓存和物品输出修复；使用 1.21.8 的 IItemHandler，保留输出槽差量同步和拒绝插入处理。 |
| Registrum | 补齐更多注册表构建器、能力注册、创造栏分区横幅、文字/提示、物品变体折叠、16 色面板、快捷栏/副手/丢弃/克隆操作；数据生成保持 1.21.8 模型、流体、配方和标签接口。恢复公开颠倒英语转换及物品栈原料工具。 |
| RPC | 新增远程方法注册、参数编码、返回值、失败/超时/断连传播、配置阶段协商与 invokeSync 系列同步阻塞调用。 |
| Space Select | 新增空间选区、滚轮调整、选区管理、事件、网络与客户端描边。 |
| Sync / processor | 新增代理字段、静态字段、惰性差分与配置同步；使用 1.21.8 的 CoreMod / ITransformer SPI。修复扫描结果 EnumHolder 被错误转为字符串、导致 S2C/C2S 方向退回 BOTH 的问题。 |
| Util | 迁入工具库拆分、集合/组件/背包/形状/轮廓工具、加权方块状态、无限堆栈和客户端 tick 修复；组件子谓词使用新版 DataComponentMatchers，保留链式构建能力。 |
| Wheel | 迁入环扇、毛玻璃、动画、翻页、中心标题、方向标记、颜色定制和跨 0° 选区修复；使用 1.21.8 GUI 渲染状态。 |
| Test | 迁入来源测试示例，新增独立世界的模型、界面、配方、持久化、RPC、同步注入、字体、轮盘及活塞回归入口。 |
| CI / 文档 | 补齐模块矩阵、同步处理器构建顺序、Maven/模组发布、Roseau 工作流和中英文模块清单。 |

`Rendering` 是 Font/Wheel 共用的 GUI 支持模块，仅包含本次所需的 SDF、模糊和缓冲布局。来源分支已删除的 Yukkuri，以及 26.1 独有的 Bloom/Compute/缓存 BER 均不在移植范围内。

## 平台适配

- 模型捕获接入 `SimpleModelWrapper` / `ResolvedModel`，支持父模型、根变换、随机变体、multipart 和动态部件。拾取接入 `GameRenderer.pick`，描边接入 `RenderHighlightEvent.Block`，按透明度阶段绘制并保留原版高对比轮廓选项。
- 保留缓存、几何内存和任务预算；仿射逆矩阵及坐标乘加精度修复一并迁入。
- GUI 使用 1.21.8 的渲染状态和层级 Z 参数；键鼠使用旧式坐标/键码参数。纹理采样使用 `GpuTexture` 的过滤设置与 `bindSampler`。
- 方块实体读写使用 `ValueInput` / `ValueOutput`。动态多方块使用 SavedData Codec，保存名沿用 `anvillib_multiblocks`，formed 字段支持缺省。
- 同步处理器通过 `META-INF/services/net.neoforged.neoforgespi.coremod.ICoreMod` 注册，类扫描读取 SecureJar，保持实例/静态字段及惰性同步。
- 配方和组件谓词使用 1.21.8 的 `DataComponentPredicate`；Registrum 数据生成保留目标版模型生成器接口。未迁入 1.21.11 独有的 `ignoreSwapAnimation` 平台方法。

## 验证

客户端回归通过 `-PportSmoke` 启用，使用 `module.test/build/port-1.21.8-smoke-run` 独立目录，每次创建新世界并输出 `port-smoke-result.txt`。普通启动不自动运行这些测试。

验证命令：

```powershell
.\gradlew.bat build :anvillib-neoforge-1.21.8:generateMetadataFileForMavenJavaPublication --no-parallel --max-workers=2 '-ProseauBaselineVersion=2.0.0' --console=plain
.\gradlew.bat :anvillib-test-neoforge-1.21.8:runClient -PportSmoke --no-parallel --max-workers=2 '-ProseauBaselineVersion=2.0.0' --console=plain
.\gradlew.bat :anvillib-test-neoforge-1.21.8:runData --no-parallel --max-workers=2 '-ProseauBaselineVersion=2.0.0' --console=plain
```

- 全模块构建与 Maven 元数据生成通过，251 个任务，包括源码/Javadoc 包、ShapeUtil 与 std140 布局验证。
- Windows 客户端与集成服务器回归通过：`PORT_SMOKE_PASSED checks=327030`。
- 72 个真实原版方块状态完成模型捕获，几何占用 260,328 字节，无不支持状态；48 种旋转/镜像、161,376 条射线、远坐标、4,096 个随机种子、准星、高亮、排除规则和资源重载均通过。
- 创造栏分区、完整 16 色面板、选择/松开消费/关闭/滚动、搜索保留通过；已查看创造栏、环扇轮盘和中英文字体三张截图。无 GPU 管线报错。
- 活塞实际推出并拉回箱子，保持同一方块实体、正确位置和十三颗钻石；实例/静态同步字段、方向注解、惰性登记和分组写回通过。
- 配方匹配回滚、约束剪枝、候选排序、拒绝插入的输出槽、多方块 formed 保存、无限堆栈、RPC 返回值/异常/超时/断连回环检查通过。
- `runData` 通过；补齐测试控制器的方块模型引用和物品模型，动态定义、掉落和中英文语言资源保持目标版格式。
- 聚合 JAR 包含 18 个模块及嵌套的同步处理器，747 个 AnvilLib 类的字节码均为 Java 21。发布坐标为 `dev.anvilcraft.lib:anvillib-neoforge-1.21.8:2.0.0`，模块 API 均已导出。
- CI 模块矩阵验证通过，共三层；`git diff --check` 通过。
- 正常保存玩家、区块和全部维度后关闭集成服务器。

这是功能移植，不承诺 1.21.1 二进制兼容。原版已变更的模型、物品染色、刷怪蛋、交互结果和数据生成 API 按 1.21.8 原生接口使用；原目标分支已停用的旧式 Registrum 染色/刷怪蛋便捷方法未恢复，相关能力可由新版模型生成器、客户端扩展和原版注册接口实现。来源新增的注册器和功能模块已纳入。

其他操作系统、独立专用服务器、外部模组组合和真实多人网络连接未包含在本地回归中。未执行发布或 Roseau 二进制兼容性报告。
