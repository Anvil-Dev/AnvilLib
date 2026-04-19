# AnvilLib [中文](README.md) | **English**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net/)
[![Maven Central](https://img.shields.io/maven-central/v/dev.anvilcraft.lib/anvillib-neoforge-1.21.1)](https://central.sonatype.com/search?q=anvillib)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT%20License-blue.svg)](https://opensource.org/licenses/MIT)

**AnvilLib** is a NeoForge mod library developed by [Anvil Dev](https://github.com/Anvil-Dev), providing Minecraft mod developers with a
series of practical tools and frameworks.

## Features

AnvilLib adopts a modular design and includes the following functional modules:

| Module                    | Description                                       |
|---------------------------|---------------------------------------------------|
| **Config**                | Annotation-based configuration system             |
| **Codec**                 | Data codecs and network serialization helpers     |
| **Integration**           | Mod compatibility integration framework           |
| **Network**               | Networking API with automatic packet registration |
| **Recipe**                | In-world recipe system                            |
| **Moveable Entity Block** | Support for block entities movable by pistons     |
| **Multiblock**            | Dynamic multiblock system                         |
| **Registrum**             | Simplified registration system                    |
| **Util**                  | Shareable utilities                               |
| **Wheel**                 | Radial wheel menu client API                      |
| **Main**                  | Aggregated module that bundles all submodules     |

## Module Introduction

### Config Module

Provides an annotation-based configuration management system to simplify the definition and management of mod configurations.

**Key Features:**

- Define configuration classes using `@Config` annotation
- Add configuration comments with `@Comment`
- Define numerical ranges with `@BoundedDiscrete`
- Create nested configurations with `@CollapsibleObject`
- Automatically generate client configuration GUI

**Usage Example:**

```java

@Config(name = "my_mod", type = ModConfig.Type.COMMON)
public class MyModConfig {
    @Comment("Enable debug mode")
    public boolean debugMode = false;

    @Comment("Maximum count")
    @BoundedDiscrete(min = 1, max = 100)
    public int maxCount = 10;
}

// Register configuration
MyModConfig config = ConfigManager.register("my_mod", MyModConfig::new);
```

### Codec Module

Provides practical helpers around Mojang `Codec` and `StreamCodec` to reduce boilerplate in
packet payloads, registry object serialization, and data-driven systems.

**Key Features:**

- Common game-domain codecs: `Item` / `Block` / `BlockState` / `EntityType` / `Vec3` / `Vec3i`
- `Codec` <-> `StreamCodec` bridges (registry-aware, NBT intermediate form)
- Compact network encoding for `NumberProvider`
- High-arity `composite(...)` overloads (`Function7` through `Function16`)

**Usage Example:**

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

### Integration Module

Provides a framework for mod integrations, supporting automatic loading of integration code based on the presence of other mods.

**Key Features:**

- Declare integration classes with `@Integration` annotation
- Support for version range matching
- Support for different runtime environments (CLIENT / DEDICATED_SERVER / DATA)

**Usage Example:**

```java

@Integration(value = "jei", version = "[19.0,)")
public class JEIIntegration {
    public void init() {
        // JEI integration logic
    }
}
```

### Moveable Entity Block Module

Allows blocks with block entities to be pushed by pistons while preserving their data.

**Usage Example:**

```java
public class MyBlock extends Block implements IMoveableEntityBlock {
    @Override
    public CompoundTag clearData(Level level, BlockPos pos) {
        // Return block entity data to preserve
        BlockEntity be = level.getBlockEntity(pos);
        return be != null ? be.saveWithoutMetadata(level.registryAccess()) : new CompoundTag();
    }

    @Override
    public void setData(Level level, BlockPos pos, CompoundTag nbt) {
        // Restore block entity data at new position
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.loadAdditional(nbt, level.registryAccess());
        }
    }
}
```

### Multiblock Module

Provides a flexible system for creating and managing dynamic multiblock structures — assemblies of blocks that behave as a single logical unit and can change shape at runtime.

**Key Features:**

- Define multiblock patterns using a declarative builder or data-driven JSON
- Support for dynamic reconfiguration (add / remove parts at runtime)
- Integration with block entities and custom rendering
- Event hooks for placement, validation and activation

**Usage Example:**

```java
// Register definition when datapack bootstrapping
public static void bootstrap(BootstrapContext<MultiblockDefinition> ctx) {
    // Define a simple multiblock from a builder
    MultiblockDefinition furnaceArray = MultiblockDefinition.seriaBuilder()
        .layer( // bottom layer
            "###",
            "#0#",
            "###"
        )
        .mapController(Blocks.DISPENSER)
        .map('#', Blocks.STONE)
        .build();
    ctx.register(
        RESOURCE_KEY, // The resource key of this multiblock
        furnaceArray
    );
}

// Register controller when initializing
public static void init() {
    ControllerRecord.register(new SimpleController(
        Blocks.DISPENSER,
        RESOURCE_KEY // The resource key of this multiblock
    ) {
        @Override
        public void onFormed(Level level, MultiblockState state) {
            // when formed...
        }

        @Override
        public void onUnformed(Level level, MultiblockState state) {
            // when unformed...
        }
    });
}
```

### Network Module

Provides a NeoForge networking abstraction with package-based packet auto-registration.

**Key Features:**

- Define packet direction using `IClientboundPacket` / `IServerboundPacket` / `IInsensitiveBiPacket`
- Automatically register packet classes in a package via `NetworkRegistrar.register(...)`
- Supports `PLAY`, `CONFIGURATION`, and `COMMON` protocols

**Usage Example:**

```java

@SubscribeEvent
public static void onRegisterPayload(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");
    NetworkRegistrar.register(registrar, "my_mod");
}
```

### Recipe Module

Provides an in-world recipe system, allowing recipes to be executed in the world (rather than in crafting tables).

**Key Features:**

- Supports custom recipe triggers (Trigger)
- Supports recipe predicates (Predicate) for conditional checks
- Supports multiple recipe outcomes (Outcome)
- Built-in priority system
- Full datapack support

**Recipe Components:**

- **Trigger**: Conditions to trigger the recipe (e.g., item dropping, explosions)
- **Predicate**: Recipe matching conditions
- **Outcome**: Recipe execution results (e.g., spawning items, setting blocks)

### Registrum Module

A registration system based on [Registrate](https://github.com/IThundxr/Registrate), simplifying the registration process for items, blocks,
entities, etc.

**Key Features:**

- Chain-style API design
- Automatic language file generation
- Automatic datapack generation
- Support for various builders

**Usage Example:**

```java
public static final Registrum REGISTRUM = Registrum.create("my_mod");

public static final RegistryEntry<Item> MY_ITEM = REGISTRUM
    .item("my_item", Item::new)
    .properties(p -> p.stacksTo(16))
    .register();
```

### Util Module

The `util` module contains a set of small, well-tested helper utilities that are commonly useful across mods. It focuses on concise, reusable primitives to reduce boilerplate.

**Key Features:**

- Collection and iteration helpers (nullable-safe operations, indexed transforms)
- NBT and Tag utilities for safe read/write and migration helpers
- Common math and geometry helpers (Vec helpers, angle/math utilities)
- Item / inventory helpers for common inventory operations

**Usage Example:**

```java
// Example: safely casting object
public AClass(Level level, BlockPos pos) {
    this(Util.castSafely(level.getBlockEntity(pos), ChestBlockEntity.class).orElse(null));
}

// Example: use ShapeUtil to construct VoxelShape
VoxelShape shape = ShapeUtil.merge(
    new AABB(0, 0, 0, 10, 10, 10),
    new AABB(1, 10, 1, 9, 16, 9)
);
```

### Wheel Module

Provides a client-side radial menu API for quick action selection.

**Key Features:**

- Two open modes: `TAP` and `HOLD` (trigger on release)
- Built-in pagination (`slotsPerPage`, default `8`)
- Optional submenu support for TAP mode
- Entry renderer + callback model via `WheelMenuBuilder`

**Usage Example:**

```java
WheelMenuModel model = WheelMenuBuilder.create()
    .slotsPerPage(8)
    .action("heal", Component.literal("Heal"), iconRenderer, ctx -> {})
    .build();

WheelScreenController controller = new WheelScreenController();
controller.openTap(model);
// HOLD mode: call on key press/release edges
controller.onHoldKeyPressed(model);
controller.onHoldKeyReleased();
```

### Main Module

`anvillib-neoforge-1.21.1` is the aggregate artifact. It bundles and re-exports:

- `config`
- `codec`
- `integration`
- `network`
- `recipe`
- `moveable-entity-block`
- `registrum`
- `wheel`

`anvillib-test-neoforge-1.21.1` is a development/testing module and is not part of the aggregate runtime artifact.

## Dependency Integration

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral() // This project is already uploaded to Maven Central
}

dependencies {
    // Full library
    implementation "dev.anvilcraft.lib:anvillib-neoforge-1.21.1:2.0.0"

    // Or import individual modules as needed
    implementation "dev.anvilcraft.lib:anvillib-config-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-codec-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-integration-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-moveable-entity-block-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-multiblock-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-network-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-recipe-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-registrum-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-util-neoforge-1.21.1:2.0.0"
    implementation "dev.anvilcraft.lib:anvillib-wheel-neoforge-1.21.1:2.0.0"
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral() // This project is already uploaded to Maven Central
}

dependencies {
    implementation("dev.anvilcraft.lib:anvillib-neoforge-1.21.1:2.0.0")

    // Optional single-module example
    implementation("dev.anvilcraft.lib:anvillib-config-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-codec-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-integration-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-moveable-entity-block-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-multiblock-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-network-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-recipe-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-registrum-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-util-neoforge-1.21.1:2.0.0")
    implementation("dev.anvilcraft.lib:anvillib-wheel-neoforge-1.21.1:2.0.0")
}
```

> Keep the dependency version aligned with release tags (current project property is `mod_version=2.0.0`).

## Building the Project

```bash
# Clone repository
git clone https://github.com/Anvil-Dev/AnvilLib.git
cd AnvilLib

# Build on macOS / Linux
./gradlew build

# Build on Windows (PowerShell / CMD)
gradlew.bat build
```

## Requirements

- Java 21+
- Minecraft 1.21.1
- NeoForge 21.1.x

## License

This project is licensed under the [MIT License](https://www.opensource.org/licenses/MIT).

Part of the Registrum module code is based on [Registrate](https://github.com/IThundxr/Registrate) and follows the Mozilla Public License
2.0.

## Author

- **Gugle** - Main developer
- **Abslb** - Contributor
- **QiuShui1012** - Contributor
- **ZhuRuoLing** - Contributor

## Links

- [GitHub Repository](https://github.com/Anvil-Dev/AnvilLib)
- [Issue Tracking](https://github.com/Anvil-Dev/AnvilLib/issues)
