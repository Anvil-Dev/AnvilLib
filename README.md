# AnvilLib **中文** | [English](README.en.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1-green.svg)](https://minecraft.net/)
[![Maven Central](https://img.shields.io/maven-central/v/dev.anvilcraft.lib/anvillib-neoforge-26.1)](https://central.sonatype.com/search?q=anvillib)
[![NeoForge](https://img.shields.io/badge/NeoForge-26.1.x-orange.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT%20License-blue.svg)](https://opensource.org/licenses/MIT)

**AnvilLib** 是一个由 [Anvil Dev](https://github.com/Anvil-Dev) 开发的 NeoForge 模组库，为 Minecraft 模组开发者提供一系列实用的工具和框架。

## 特性

AnvilLib 采用模块化设计，包含以下功能模块：

| 模块                        | 说明             |
|---------------------------|----------------|
| **Config**                | 基于注解的配置系统      |
| **Codec**                 | 数据编解码与网络序列化工具  |
| **Integration**           | 模组兼容性集成框架      |
| **Network**               | 网络通信与数据包自动注册框架 |
| **Recipe**                | 世界内配方系统        |
| **Moveable Entity Block** | 可被活塞推动的方块实体支持  |
| **Multiblock**            | 动态多方块系统        |
| **Registrum**             | 简化的注册系统        |
| **Util**                  | 可共享的工具方法       |
| **Wheel**                 | 轮盘菜单客户端 API    |
| **Main**                  | 聚合模块（包含全部子模块）  |

## 模块介绍

### Config 模块

提供基于注解的配置管理系统，简化模组配置的定义和管理。

**主要特性：**

- 使用 `@Config` 注解定义配置类
- 使用 `@Comment` 添加配置注释
- 使用 `@BoundedDiscrete` 定义数值范围
- 使用 `@CollapsibleObject` 创建嵌套配置
- 自动生成客户端配置界面

**使用示例：**

```java

@Config(name = "my_mod", type = ModConfig.Type.COMMON)
public class MyModConfig {
    @Comment("启用调试模式")
    public boolean debugMode = false;

    @Comment("最大数量")
    @BoundedDiscrete(min = 1, max = 100)
    public int maxCount = 10;
}

// 注册配置
MyModConfig config = ConfigManager.register("my_mod", MyModConfig::new);
```

### Codec 模块

提供围绕 Mojang `Codec` 与 `StreamCodec` 的实用工具，减少网络包、配方数据与注册表对象序列化时的样板代码。

**主要特性：**

- 常用对象编解码：`Item` / `Block` / `BlockState` / `EntityType` / `Vec3` / `Vec3i`
- `Codec` 与 `StreamCodec` 互转（支持注册表上下文 + NBT 中间格式）
- `NumberProvider` 的紧凑网络编码
- `composite(...)` 高阶重载（支持 `Function7` 到 `Function16`）

**使用示例：**

```java
public record ExamplePayload(Item item, int count) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ExamplePayload> STREAM_CODEC =
        StreamCodec.composite(
            StreamCodecUtil.ITEM,
            ExamplePayload::item,
            ByteBufCodecs.VAR_INT,
            ExamplePayload::count,
            ExamplePayload::new
        );
}
```

### Integration 模块

提供模组间集成的框架，支持根据其他模组的存在与否自动加载集成代码。

**主要特性：**

- 使用 `@Integration` 注解声明集成类
- 支持版本范围匹配
- 支持不同运行环境（CLIENT / DEDICATED_SERVER / DATA）

**使用示例：**

```java

@Integration(value = "jei", version = "[19.0,)")
public class JEIIntegration {
    public void init() {
        // JEI 集成逻辑
    }
}
```

### Moveable Entity Block 模块

允许带有方块实体的方块被活塞推动，同时保留其数据。

**使用示例：**

```java
public class MyBlock extends Block implements IMoveableEntityBlock {
    @Override
    public CompoundTag clearData(Level level, BlockPos pos) {
        // 返回需要保留的方块实体数据
        BlockEntity be = level.getBlockEntity(pos);
        return be != null ? be.saveWithoutMetadata(level.registryAccess()) : new CompoundTag();
    }

    @Override
    public void setData(Level level, BlockPos pos, CompoundTag nbt) {
        // 在新位置恢复方块实体数据
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.loadAdditional(nbt, level.registryAccess());
        }
    }
}
```

### Multiblock 模块

提供用于创建与管理动态多方块结构的灵活系统——将多个方块组合为一个逻辑单元，并支持在运行时改变形态。

**主要特性：**

- 使用声明式构建器或数据驱动（JSON）定义多方块结构
- 支持动态重配置（在运行时添加/移除构件）
- 与方块实体与自定义渲染的集成
- 提供放置、校验与激活等事件钩子

**使用示例：**

```java
// 数据包初始化时注册定义
public static void bootstrap(BootstrapContext<MultiblockDefinition> ctx) {
    // 从构建器构建一个简单的多方块
    MultiblockDefinition furnaceArray = MultiblockDefinition.seriaBuilder()
        .layer( // 底层
            "###",
            "#0#",
            "###"
        )
        .mapController(Blocks.DISPENSER)
        .map('#', Blocks.STONE)
        .build();
    ctx.register(
        RESOURCE_KEY, // 多方块的资源键
        furnaceArray
    );
}

// 初始化时注册控制器
public static void init() {
    ControllerRecord.register(new SimpleController(
        Blocks.DISPENSER,
        RESOURCE_KEY // 多方块的资源键
    ) {
        @Override
        public void onFormed(Level level, MultiblockState state) {
            // 成形时……
        }

        @Override
        public void onUnformed(Level level, MultiblockState state) {
            // 未成形时……
        }
    });
}
```

### Network 模块

提供面向 NeoForge 的网络通信抽象，支持按包扫描并自动注册数据包。

**主要特性：**

- 使用 `IClientboundPacket` / `IServerboundPacket` / `IInsensitiveBiPacket` 定义通信方向
- 通过 `NetworkRegistrar.register(...)` 自动注册同一包下的数据包
- 支持 `PLAY` / `CONFIGURATION` / `COMMON` 三种协议通道

**使用示例：**

```java

@SubscribeEvent
public static void onRegisterPayload(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");
    NetworkRegistrar.register(registrar, "my_mod");
}
```

### Recipe 模块

提供世界内配方系统，允许定义在世界中（而非工作台）执行的配方。

**主要特性：**

- 支持自定义配方触发器 (Trigger)
- 支持配方谓词 (Predicate) 进行条件判断
- 支持多种配方结果 (Outcome)
- 内置优先级系统
- 完整的数据包支持

**配方组成：**

- **Trigger**: 触发配方的条件（如物品落地、爆炸等）
- **Predicate**: 配方匹配条件
- **Outcome**: 配方执行结果（如生成物品、设置方块等）

### Registrum 模块

基于 [Registrate](https://github.com/IThundxr/Registrate) 的注册系统，简化物品、方块、实体等的注册流程。

**主要特性：**

- 链式 API 设计
- 自动生成语言文件
- 自动生成数据包
- 支持各类 Builder

**使用示例：**

```java
public static final Registrum REGISTRUM = Registrum.create("my_mod");

public static final RegistryEntry<Item> MY_ITEM = REGISTRUM
    .item("my_item", Item::new)
    .properties(p -> p.stacksTo(16))
    .register();
```

### Util 模块

`util` 模块包含一组小巧且经过良好测试的实用工具，这些工具在不同模组间通用，旨在减少样板代码并提供可靠的原语。

**主要特性：**

- 集合与迭代辅助（空安全操作、带索引的转换）
- NBT 与 Tag 工具（安全读写、迁移助手）
- 常用数学与几何助手（向量工具、角度/数学工具）
- 物品 / 仓位辅助（常见的物品搬运、合并逻辑）

**使用示例：**

```java
// 示例：安全地进行类型转换
public AClass(Level level, BlockPos pos) {
    this(Util.castSafely(level.getBlockEntity(pos), ChestBlockEntity.class).orElse(null));
}

// 示例：使用 ShapeUtil 构造体素形状
VoxelShape shape = ShapeUtil.merge(
    new AABB(0, 0, 0, 10, 10, 10),
    new AABB(1, 10, 1, 9, 16, 9)
);
```

### Wheel 模块

提供轮盘菜单的客户端 API，用于快速选择并触发操作。

**主要特性：**

- 两种打开方式：`TAP` 与 `HOLD`（松开触发）
- 内置分页（`slotsPerPage`，默认 `8`）
- `TAP` 模式支持子菜单
- 通过 `WheelMenuBuilder` 定义条目渲染与回调

**使用示例：**

```java
WheelMenuModel model = WheelMenuBuilder.create()
    .slotsPerPage(8)
    .action(
        "heal", Component.literal("Heal"), iconRenderer, ctx -> {
        }
    )
    .build();

WheelScreenController controller = new WheelScreenController();
controller.openTap(model);
// HOLD 模式：按键按下/松开边沿分别调用
controller.onHoldKeyPressed(model);
controller.onHoldKeyReleased();
```

### Main 模块

`anvillib-neoforge-1.21.1` 为聚合发行模块，默认打包并重导出以下子模块：

- `config`
- `codec`
- `integration`
- `network`
- `recipe`
- `moveable-entity-block`
- `multiblock`
- `registrum`
- `util`
- `wheel`

`anvillib-test-neoforge-1.21.1` 为开发/测试模块，不包含在聚合运行时产物中。

## 依赖引入

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral() // 本项目已经上传至 Maven Central
}

dependencies {
    // 完整库
    implementation "dev.anvilcraft.lib:anvillib-neoforge-26.1:2.0.0"

    // 或按需引入单独模块
    implementation "dev.anvilcraft.lib:anvillib-config-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-codec-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-integration-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-moveable-entity-block-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-multiblock-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-network-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-recipe-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-registrum-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-util-neoforge-26.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-wheel-neoforge-26.1:2.0.0"
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral() // 本项目已经上传至 Maven Central
}

dependencies {
    implementation("dev.anvilcraft.lib:anvillib-neoforge-26.1:2.0.0")

    // 按需引入示例
    implementation("dev.anvilcraft.lib:anvillib-config-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-codec-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-integration-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-moveable-entity-block-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-multiblock-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-network-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-recipe-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-registrum-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-util-neoforge-26.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-wheel-neoforge-26.1:2.0.0")
}
```

> 版本号建议与项目发布版本保持一致（当前工程配置为 `mod_version=2.0.0`）。

## 构建项目

```bash
# 克隆仓库
git clone https://github.com/Anvil-Dev/AnvilLib.git
cd AnvilLib

# macOS / Linux 构建
./gradlew build

# Windows PowerShell / CMD 构建
gradlew.bat build
```

## 环境要求

- Java 25+
- Minecraft 26.1
- NeoForge 26.1.x

## 许可证

本项目采用 [MIT License](https://www.opensource.org/licenses/MIT) 许可证。

Registrum 模块部分代码基于 [Registrate](https://github.com/tterrag1098/Registrate)，遵循 Mozilla Public License 2.0。

## 作者

- **Gugle** - 主要开发者

## 相关链接

- [GitHub 仓库](https://github.com/Anvil-Dev/AnvilLib)
- [问题反馈](https://github.com/Anvil-Dev/AnvilLib/issues)
