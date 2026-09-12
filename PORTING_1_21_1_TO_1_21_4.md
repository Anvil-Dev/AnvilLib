# 1.21.1 → 1.21.4 特性对照与移植记录

来源为本地 `fff/1.21.1`（`fe26ea6`），目标为当前 `fff/1.21.4-port`（起点 `d79d653`）。工作区开始时无未提交改动。以来源分支的完整文件树、提交历史和公开方法为核对依据，保留目标版的平台适配；参考本地 `fff/1.21.8-port` 的回归用例和已经验证的通用修复。

运行环境保持 Minecraft **1.21.4**、NeoForge **21.4.157**、Java **21**。

## 功能对照

| 模块 | 对照和移植结果 |
| --- | --- |
| Codec | 补齐 1–16 参数组合、条件流编解码、ZOM 列表、MapCodec 编码、标签工具、谓词 StreamCodec、VarInt 方块坐标；保留 1.21.4 注册表与原料接口。 |
| Collision（新增） | AABB、三角形和三角形之间的碰撞检测与数学工具。 |
| Config | 配置分组、嵌套对象、枚举翻译、点分隔配置键和格式化。 |
| Cube（新增） | 精确模型拾取、斜棱线高亮、凸几何、BVH、轮廓缓存、动态部件、父模型/根变换、旋转/镜像、随机变体和 multipart、邻格补扫、资源重载、运行时排除规则；包含最新双精度坐标转换和仿射逆矩阵修复。 |
| Explosion（新增） | 分层球壳爆炸、熔化配方缓存、方块破坏和掉落处理。 |
| Font（新增） | 系统字体选择、SDF 字形图集、缓存、测量/布局、GUI 绘制与配置界面。 |
| Integration | 加载判断、分阶段集成和数据生成接入。 |
| Moveable Entity Block | 保留方块实体搬运、回调、数据和渲染；修正局部变量捕获，使用调用参数和类型序号。 |
| Multiblock（新增） | 动态多方块、异步快照检查、未加载区块处理、形成/解体事件、控制器、配置与网络同步；保留公开定义序列化和 formed 持久化。 |
| Network | BoolAndInt、条件广播、公开协议、网络包自动注册改进。 |
| Recipe | 方块约束搜索树剪枝、匹配快照回滚、组件保存、缓存与物品输出修复；适配 RecipeMap、ResourceKey 配方 ID、服务端配方管理和目标版数据生成。 |
| Registrum | 新增注册表构建器、能力注册、创造栏分区横幅/文字/提示、物品变体折叠、16 色面板、快捷栏/副手/丢弃/克隆操作；保留 1.21.4 模型/物品模型/配方生成器，补齐公开语言转换与物品栈原料工具。 |
| RPC（新增） | 方法注册、参数/返回值编码、异常/超时/断连传播、配置阶段协商，以及 invokeSync 同步阻塞调用。 |
| Space Select（新增） | 空间选区、滚轮调整、选区管理、事件、网络和客户端描边。 |
| Sync / processor（新增） | 代理字段、静态字段、惰性差分与配置同步；使用 CoreMod / ITransformer SPI。正确读取扫描数据中的枚举方向。 |
| Util（新增） | 工具库拆分、集合/组件/背包/形状/轮廓工具、加权方块状态、无限堆栈和客户端 tick 修复。 |
| Wheel | 环扇、毛玻璃、动画、翻页、中心标题、方向标记、颜色定制与跨 0° 选区修复；使用目标版着色器 API。 |
| Main / Test / CI | 聚合全部 17 个功能模块及同步处理器；迁入示例、模块矩阵、处理器发布顺序、Maven/模组发布、Roseau 工作流和中英文文档。 |

## 平台差异

- 模型捕获接入 1.21.4 `BlockModel.bake`，根变换从 `NeoForgeModelProperties` 读取。加权模型按实际 `SimpleWeightedRandomList` 的 `nextInt` 算法选择，保持拾取与渲染一致。
- GUI 着色器注册 `ShaderProgram`，运行时通过 `ShaderManager` 取得重载后的 `CompiledShaderProgram`。资源引用使用 `core/` 路径；SDF 绘制先提交 GUI 队列。
- 1.21.4 最终 NeoForge 尚无原生附件自动同步，因此新增 `dev.anvilcraft.lib.v2.registrum.attachment.AttachmentSyncHandler` 和 `AttachmentSync`。`AttachmentBuilder.sync(...)` 保留处理器、StreamCodec、接收者过滤三个入口；支持实体、方块实体、区块、世界的初始同步、默认创建、设置和移除。修改可变对象后调用 `AttachmentSync.syncData(holder, type)`。
- 1.21.4 将物品/方块交互结果统一为 `InteractionResult`，转换工具因此返回恒等函数。
- 数据生成使用目标版生成器与 Client/Server 事件。旧式物品染色、渲染层和 loot 便捷接口按 1.21.4 原生模型/数据生成 API 使用；这次移植不承诺 1.21.1 的二进制兼容性。
- 来源已删除的 Yukkuri 和其他版本独有的 Rendering/Bloom/Compute 功能不属于本次范围。

## 验证方式

`-PportSmoke` 在 `module.test/build/port-1.21.4-smoke-run` 中创建独立测试世界，并输出 `port-smoke-result.txt`；普通客户端启动不运行自动回归。

```powershell
.\gradlew.bat build :anvillib-neoforge-1.21.4:generateMetadataFileForMavenJavaPublication --no-parallel --max-workers=2 '-ProseauBaselineVersion=2.0.0' --console=plain
.\gradlew.bat :anvillib-test-neoforge-1.21.4:runClient -PportSmoke --no-parallel --max-workers=2 '-ProseauBaselineVersion=2.0.0' --console=plain
.\gradlew.bat :anvillib-test-neoforge-1.21.4:runData --no-parallel --max-workers=2 '-ProseauBaselineVersion=2.0.0' --console=plain
```

## 验证结果

- 全量 `build`、源码/Javadoc 包和 Maven 发布元数据生成通过；ShapeUtil 的既有形状与 AABB 检查通过。
- `runData` 通过；定义、掉落、语言、方块状态和物品模型均生成成功。测试控制器的方块与物品均引用实际存在的 `minecraft:block/iron_block`，并修复通用方块物品默认生成链路。
- Windows 客户端与集成服务器回归通过：`PORT_SMOKE_PASSED checks=434901`。72 个真实原版方块状态全部捕获，几何占用 260,328 字节，不支持状态为 0。
- 几何检查覆盖 48 种正交旋转/镜像（含边界）和 16 种原版模型旋转（表面内部采样），共 215,168 条站立/潜行射线；另有远坐标、4,096 个随机种子、实际准星拾取、高亮、排除规则及资源重载检查。1.21.4 的旧 `SymmetricGroup3` 矩阵构造存在多余单位对角项，测试使用原版方向映射构造正确正交基；未修改原版数学类。
- 创造栏分区、16 色面板、选择/松开消费/关闭/滚动和搜索保留通过；补齐小窗口下的面板位置约束。已查看创造栏、环扇轮盘与中英文字体截图。
- 活塞真实推出和拉回箱子，保持同一方块实体、正确位置与十三颗钻石。同步字段的实例/静态注入、方向枚举、惰性登记和分组写回通过。
- 附件兼容层通过实体（玩家）、方块实体、区块和世界四类持有者的真实网络更新、移除、默认创建与接收者过滤检查。初次跟踪/进入区块/登录等生命周期路径已接入，未另建独立服务器连接测试。
- 配方匹配回滚、约束剪枝、候选排序、重复注册、`RecipeMap` 的 ResourceKey 索引、拒绝插入的输出槽、多方块 formed 保存、无限堆栈，以及 RPC 返回值/异常/超时/断连回环检查通过。同步修复重复注册的剪枝索引和变体折叠后分区边界问题。
- 聚合 JAR 包含 17 个功能模块及嵌套同步处理器，693 个 AnvilLib 类均为 Java 21 字节码。发布坐标为 `dev.anvilcraft.lib:anvillib-neoforge-1.21.4:2.0.0`，17 个模块依赖均从 API 变体导出。没有混入其他版本的 Rendering 类。
- CI 模块矩阵验证通过，共三层。`git diff --check` 通过。
- 客户端完成后正常保存玩家、区块和全部维度，并关闭集成服务器。

其他操作系统、独立专用服务器、外部模组组合和真实远程多人连接未包含在本地验证中。未执行发布或 Roseau 二进制兼容性报告。
