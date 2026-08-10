# Minecraft / NeoForge 版本差异适配文档：1.21.1 → 26.1

> 文档信息
>
> - 生成日期：2026-08-10
> - 分析对象：`port/1.21.1` @ `fab551b`（Minecraft 1.21.1 / NeoForge 21.1.226）↔ `dev/26.1` @ `0b7e45f`（Minecraft 26.1.2 /
    NeoForge 26.1.2.76），merge-base `81c5a9c`
> - 数据来源：`git diff port/1.21.1 dev/26.1` 逐模块 diff 实测 + dev/26.1 侧源码抽查 + NeoForge 26.1.2.76 sources jar 实测（
    `net.neoforged.neoforge.transfer.*`、`net.neoforged.neoforge.fluids.capability.*`、
    `net.neoforged.neoforge.capabilities.*`）
> - 关联文档：本文是 **版本差异适配文档**，专注「差异中属于 Minecraft/NeoForge API 版本差异的必要适配」；两侧分支关系与回迁项判定见
    `docs/branch-comparison.md`
> - 结论性质：除标注「待验证」处外，均基于实测提交、文件内容与 26.1.2.76 sources jar 源码得出

---

## 1. 概述与结论

AnvilLib 的 `dev/26.1` 分支沿 1.21.2 → 1.21.11 → 26.1 的版本链逐级升级（`6f2685a` 正式切入 26.1），而 `port/1.21.1` 冻结在
1.21.1 平台。对共享模块执行 `git diff port/1.21.1 dev/26.1` 后，把差异中「Minecraft/NeoForge API 版本差异」的部分单独抽出，即得到本文整理的
13 个差异面。这些差异是 **单向必要适配**：dev/26.1 侧已是新 API 形态，任何从 port/1.21.1 回迁的代码都必须按此清单改写，否则无法在
26.1 编译。

### 1.1 差异面总览

| #  | 差异面             | 一句话概括                                                                                                                                                                                 |
|----|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2  | 标识符与基础类型   | `ResourceLocation` → `Identifier`（含 CODEC/STREAM_CODEC、buffer 读写方法），`ResourceKey.location()` → `identifier()`，官方包 `critereon` → `criterion`                                   |
| 3  | 物品栈体系         | 可变 `ItemStack` 让位给不可变 `ItemStackTemplate`（`typeHolder()`/`withCount(n).create()`/`components()`），`Inventory` 公开字段移除                                                       |
| 4  | 谓词体系           | `ItemSubPredicate`/`DataComponentPredicate` 谓词参数让位给 `net.minecraft.advancements.criterion.DataComponentMatchers`，`matches(ItemStack)` → `matches(DataComponentGetter)`             |
| 5  | 配方体系           | `RecipeSerializer` 变 final 类（`new RecipeSerializer<>(CODEC, STREAM_CODEC)`），`RecipeManager` 内部改 `RecipeMap`，`save` 改收 `ResourceKey`，`recipeAccess()` 取代 `getRecipeManager()` |
| 6  | 网络               | `PayloadRegistrar` 双向注册 3 参 → 4 参（client/server handler 拆分），`FMLLoader.getCurrent()` 实例化访问，`IPacket.type()` 返回 `Identifier`                                             |
| 7  | 注册表与延迟注册   | `Registry.get` → `getOptional`/`getValue`，tag 解析需 `HolderGetter`，`getTag` → `getTagOrEmpty`                                                                                           |
| 8  | 渲染管线           | `extractRenderState`/`submit` 两段式 RenderState 架构，`GuiGraphics` → `GuiGraphicsExtractor`，`PoseStack` → `Matrix3x2fStack`，`RenderPipeline`+UBO 取代 `ShaderInstance`/shader json     |
| 9  | 事件与加载器       | `FMLLoader.getDist()` → `getCurrent().getDist()`，`GatherDataEvent` 按端拆分，`BreakBlockEvent`，`KeyEvent`/`MouseButtonEvent` 输入事件                                                    |
| 10 | NBT 与持久化       | `CompoundTag`+`HolderLookup` → `ValueInput`/`ValueOutput`，`SavedData.Factory` → `SavedDataType<T>`+Codec                                                                                  |
| 11 | 能力（capability） | 物品 `IItemHandler` → `ResourceHandler<ItemResource>`（无 `setResource`），流体 `IFluidHandler` 弃用 → `ResourceHandler<FluidResource>`，`BlockCapability.createSided(Identifier, ...)`    |
| 12 | 注解体系           | javax/neoforge 空注解 → jspecify `@NullMarked` + `org.jspecify.annotations.Nullable`                                                                                                       |
| 13 | 数据生成与构建     | `REGISTRUM.addDataGenerator` 取代 `GatherDataEvent` 注册，根级 `module.gradle` 聚合，坐标 `-neoforge-26.1`，`anvillib.needRunConfig`                                                       |

### 1.2 回迁代码的共性适配清单

以下适配项与具体业务无关， **任何从 port/1.21.1 回迁的代码几乎必然触及**，是回迁前的统一预处理清单：

1. `ResourceLocation` → `Identifier`：import 与全部用法（含 `ResourceLocation.CODEC/STREAM_CODEC`、`fromNamespaceAndPath`、
   `writeResourceLocation/readResourceLocation`）。
2. 空注解体系切换：`@MethodsReturnNonnullByDefault`/`@ParametersAreNonnullByDefault`/`javax.annotation.Nullable` → 包级
   `@NullMarked` + `org.jspecify.annotations.Nullable`。
3. `FMLLoader`/`LoadingModList` 静态访问 → `FMLLoader.getCurrent()` 实例访问（`getDist()`、`getLoadingModList()`）。
4. 构建文件不搬运：port 侧各模块独立 `build.gradle`（约 270 行）不回迁，按 dev 侧 `module.gradle` 聚合体系（依赖坐标
   `-neoforge-26.1`）落位。
5. `@Mod` 构造器去掉 `ModContainer` 参数（`(IEventBus bus, ModContainer container)` → `(IEventBus modBus)`）。
6. 渲染相关代码（若涉及 GUI/方块实体渲染）：`render(GuiGraphics, ...)` → `extractRenderState(GuiGraphicsExtractor, ...)`、
   `PoseStack` → `Matrix3x2fStack`、shader 注册改 `RenderPipeline`。

### 1.3 判定规则与依据

- 本文只收录「属于 Minecraft/NeoForge API 版本差异」的部分（dev 侧已是新 API 的必要适配），不收录 dev
  侧业务演进（事件系统、构建重构、新模块等），也不收录 port 侧独有 bugfix 回迁（见 `docs/branch-comparison.md` 的 #1–#8）。
- 每个条目给出 dev/26.1 侧真实文件路径，可通过 `git show dev/26.1:<路径>` 复核。
- 涉及 26.1.2.76 官方 API 的表述，优先以本地 sources jar 源码为准。

---

## 2. 标识符与基础类型

### 2.1 `ResourceLocation` → `Identifier`

**变化**：Minecraft 1.21.5+ 官方映射将 `net.minecraft.resources.ResourceLocation` 重命名为
`net.minecraft.resources.Identifier`，是本仓库 diff 中出现频率最高的机械替换项，具体包括：

| 旧（1.21.1）                                               | 新（26.1）                                         |
|------------------------------------------------------------|----------------------------------------------------|
| `net.minecraft.resources.ResourceLocation`                 | `net.minecraft.resources.Identifier`               |
| `ResourceLocation.fromNamespaceAndPath(ns, path)`          | `Identifier.fromNamespaceAndPath(ns, path)`        |
| `ResourceLocation.CODEC` / `ResourceLocation.STREAM_CODEC` | `Identifier.CODEC` / `Identifier.STREAM_CODEC`     |
| `buf.writeResourceLocation(RL)` / `readResourceLocation()` | `buf.writeIdentifier(id)` / `buf.readIdentifier()` |

dev/26.1 侧实际形态（`InWorldRecipe.java` 的 `STREAM_CODEC`）：

```java
buf.writeIdentifier(Objects.requireNonNull(recipe.trigger().

getId()));
// ...
Identifier location = buf.readIdentifier();
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java`
- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/CodecUtil.java`
- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/StreamCodecUtil.java`
- `module.network/src/main/java/dev/anvilcraft/lib/v2/network/packet/IPacket.java`
- `module.test/src/main/java/dev/anvilcraft/lib/v2/test/AnvilLibTest.java`

**迁移注意**：这是纯机械替换，但注意 26.1 下 `Identifier` 不再接受字符串「直接构造」的旧写法（1.21.1 的
`new ResourceLocation(String)` 已移除），统一走 `Identifier.fromNamespaceAndPath` / `Identifier.parse`。

### 2.2 `ResourceKey.location()` → `ResourceKey.identifier()`

**变化**：`ResourceKey` 的资源位置访问方法随 1.21.5 改名：`key.location()` → `key.identifier()`。dev/26.1 侧实际形态（
`MultiblockFormPacket.java`）：

```java
IController controller = ControllerRecord.get(state.getBlock(), this.state.getDefinitionKey().identifier());
```

**涉及文件**：

- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/network/MultiblockFormPacket.java`
- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/network/MultiblockUnformPacket.java`
- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/DynamicMultiblockManager.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/mixin/RecipeMapMixin.java`

**迁移注意**：`ResourceKey.create(Registries.X, Identifier)` 的构造方式不变，只有 `location()` 访问方法改名；grep 时以
`\.location()` 结尾且接收方为 `ResourceKey` 的调用点为搜索目标。

### 2.3 官方包改名：`critereon` → `criterion`

**变化**：`net.minecraft.advancements.critereon` 包整体更名为 `net.minecraft.advancements.criterion`（26.1 官方改名），影响该包下全部约
20 个 predicate 类的 import（`module.codec/StreamCodecUtil.java` 全部 import 变更即为此事）。两个具体影响点：

- `RecipeUnlockedTrigger`：`net.minecraft.advancements.critereon.RecipeUnlockedTrigger` →
  `net.minecraft.advancements.criterion.RecipeUnlockedTrigger`（见 5.4）。
- AT 文件目标描述符同步改名：`module.codec/src/main/resources/META-INF/accesstransformer.cfg` 中
  `net.minecraft.advancements.critereon.LocationPredicate$PositionPredicate` →
  `net.minecraft.advancements.criterion.LocationPredicate$PositionPredicate`。

**涉及文件**：

- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/StreamCodecUtil.java`
- `module.codec/src/main/resources/META-INF/accesstransformer.cfg`

**迁移注意**：回迁代码中所有 `critereon` 包 import 必须改为 `criterion`，否则编译失败；此条与 4.1 的
`DataComponentMatchers`（新包名下的新类型）配套出现。

---

## 3. 物品栈体系

### 3.1 `ItemStack` → 不可变 `ItemStackTemplate`

**变化**：26.1 中用于「配方产出、缓存、序列化」等场景的物品栈主力形态从可变的 `ItemStack` 改为不可变的 `ItemStackTemplate`
，核心差异：

| 旧（1.21.1）                                   | 新（26.1）                                                                                 |
|------------------------------------------------|--------------------------------------------------------------------------------------------|
| `ItemStack stack = new ItemStack(item, count)` | `ItemStackTemplate`（record 式不可变，字段为 `typeHolder()` + `count()` + `components()`） |
| `stack.copy()` / `stack.setCount(n)`           | `template.withCount(n).create()`（`create()` 产出 `ItemStack`）                            |
| `ItemStack.CODEC` / `STREAM_CODEC`             | `ItemStackTemplate.CODEC` / `ItemStackTemplate.STREAM_CODEC`                               |
| `stack.getItem()`                              | `template.typeHolder().value()`                                                            |
| `stack.getComponents()`                        | `template.components()`                                                                    |

dev/26.1 侧实际形态（`InWorldRecipe.java`，icon 字段与序列化）：

```java
private final @Unmodifiable ItemStackTemplate icon;

// ...
public ItemStack getResultItem() {
    return this.icon.create();
}
// ...
ItemStackTemplate.CODEC.

fieldOf("icon").

orElseGet(() ->new

ItemStackTemplate(Items.ANVIL)).

forGetter(InWorldRecipe::icon),
// ...
ItemStackTemplate.STREAM_CODEC.

encode(buf, recipe.icon());
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/outcome/SpawnItem.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/builder/InWorldRecipeBuilder.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/ChanceItemStack.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/stack/UnlimitedItemStack.java`（旧版，
  `@Deprecated(forRemoval = true)`）

**迁移注意**：模板化后「先改数量再使用」必须走 `withCount(n).create()` 链式调用；`create()` 每次产出新 `ItemStack`
，不要再假设可以原地修改共享模板。

### 3.2 `ItemStack.ITEM_NON_AIR_CODEC` → `Item.CODEC`

**变化**：26.1 移除了 `ItemStack.ITEM_NON_AIR_CODEC`，需要「只编解码物品类型、不带数量」的场景改用 `Item.CODEC`（非空校验）或
`Item.CODEC_WITH_BOUND_COMPONENTS`（带组件）。dev/26.1 侧实际形态（`SpawnItem.java`，配合 `typeHolder()` 使用）：

```java
// 旧：ItemStack.ITEM_NON_AIR_CODEC → 新：Item.CODEC / Item.CODEC_WITH_BOUND_COMPONENTS（配合 typeHolder()）
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/outcome/SpawnItem.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/ChanceItemStack.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/stack/UnlimitedItemStack.java`

**迁移注意**：`Item.CODEC` 与 `Item.CODEC_WITH_BOUND_COMPONENTS` 的语义差异在于组件是否参与编解码，按业务场景选择，不要一律替换为
`Item.CODEC`。

### 3.3 `Inventory` 公开字段移除

**变化**：`net.minecraft.world.entity.player.Inventory` 的 `items`/`armor`/`offhand` 公开字段在 26.1 移除，遍历玩家背包改为走
`getContainerSize()` + `getItem(i)` 循环。dev/26.1 侧 `InventoryUtil.getItems` 即按此改写。

**涉及文件**：

- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/InventoryUtil.java`

**迁移注意**：若回迁代码直接引用 `inventory.items` 等字段，改写成按槽位循环；注意 `Inventory` 的槽位区间（主手 0–35、盔甲
36–39、副手 40）在 26.1 未变。

### 3.4 相关：`LootParams` 第二参数改为 `ContextMap`

**变化**：`LootParams(Level, Map<String, Object>)` 构造中第二参数在 26.1 改为 `ContextMap`：
`new LootParams(level, Map.of(), ...)` → `new LootParams(level, ContextMap.EMPTY, ...)`（或
`new ContextMap.Builder().create(new ContextKeySet.Builder().build())`）。出现在 `ChanceBlockState`/
`WeightedChanceBlockStates`/`ChanceItemStack`/`InWorldRecipeContext` 的 loot table 上下文构造中。

**涉及文件**：

- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/ChanceBlockState.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/WeightedChanceBlockStates.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipeContext.java`

**迁移注意**：`ContextMap.EMPTY` 只在业务不依赖自定义参数时可用；需要向 loot context 传参的场景须改用 `ContextKeySet`/
`ContextMap.Builder`。

---

## 4. 谓词体系

### 4.1 `ItemSubPredicate`/`DataComponentPredicate` → `DataComponentMatchers`

**变化**：1.21.1 的「物品子谓词」体系在 26.1 中拆为两条线：

1. **谓词参数类型**（`ItemPredicate` 构造时传入的组件匹配器）：`ItemSubPredicate` 与早期 `DataComponentPredicate` 参数 →
   `net.minecraft.advancements.criterion.DataComponentMatchers`（含 `CODEC.codec()`、`ANY` 常量、
   `Builder.components().exact(DataComponentExactPredicate.builder()...)` 链）。
2. **谓词实现载体**：`net.minecraft.core.component.predicates.DataComponentPredicate` 保留并成为「可注册的组件谓词」接口，AnvilLib
   的 `AndPredicate`/`NotPredicate`/`OrPredicate` 直接实现它。

dev/26.1 侧实际形态：

```java
// module.recipe/predicate/item/HasItem.java

import net.minecraft.advancements.criterion.DataComponentMatchers;
// ItemPredicate 以 DataComponentMatchers 作为参数字段
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/predicate/item/HasItem.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/predicate/item/HasItemIngredient.java`
- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/StreamCodecUtil.java`（`ITEM_PREDICATE` 字段：
  `DataComponentPredicate.STREAM_CODEC` → `DataComponentMatchers.STREAM_CODEC`）
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/ItemPredicate.java`

**迁移注意**：这是「类型归属」迁移——1.21.1 的 `DataComponentPredicate` import（`net.minecraft.advancements.critereon` 或旧
`component` 包）需要按用途分流：匹配器参数走 `DataComponentMatchers`，谓词实现走
`net.minecraft.core.component.predicates.DataComponentPredicate`，两者不可混用。

### 4.2 `matches(ItemStack)` → `matches(DataComponentGetter)`

**变化**：组件谓词的匹配入口从「整个物品栈」收窄为「组件读取器」：`matches(ItemStack)` → `matches(DataComponentGetter)`
。dev/26.1 侧实际形态（`AndPredicate.java`）：

```java

@Override
public boolean matches(DataComponentGetter getter) {
    return this.subPredicates().stream().allMatch(it -> it.getValue().matches(getter));
}
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/data/advancement/predicate/item/AndPredicate.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/data/advancement/predicate/item/OrPredicate.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/data/advancement/predicate/item/NotPredicate.java`

**迁移注意**：调用方需要「先取组件再匹配」——`stack.getComponents()`（`ItemStack` 实现了 `DataComponentGetter`
）传入；谓词内不再直接读取栈的数量/物品类型信息。

### 4.3 `ItemPredicate.withSubPredicate` 删除

**变化**：`ItemPredicate` 的 `subPredicates` 字段与 `Builder.withSubPredicate(...)` 在 26.1 一并删除（1.21.1 的
`ItemSubPredicate` 无对应替代），dev 侧全部调用方已清理（grep 无残留）。`module.util` 的 `IItemStackPredicate` 同样删除了
`subPredicates()`。

**涉及文件**：

- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/ItemPredicate.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/IItemStackPredicate.java`

**迁移注意**：不要尝试保留 `withSubPredicate` 兼容层；如需组合谓词，走 `AndPredicate`/`OrPredicate`/`NotPredicate`（实现
`DataComponentPredicate`）的组合方式。

### 4.4 注册表 `ITEM_SUB_PREDICATE_TYPE` → `DATA_COMPONENT_PREDICATE_TYPE`

**变化**：1.21.5+ 引入 `DATA_COMPONENT_PREDICATE_TYPE` 注册表取代 `ITEM_SUB_PREDICATE_TYPE`；注册条目类型由
`ItemSubPredicate.Type` 改为 `DataComponentPredicate.TypeBase`（匿名子类），codec/streamCodec 签名随之变化。dev/26.1 侧
`LibItemSubPredicates.java` 已被 `LibDataComponentPredicates.java` 取代。

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/init/LibDataComponentPredicates.java`

**迁移注意**：1.21.1 侧 `LibItemSubPredicates` 不迁移；新注册走 `DATA_COMPONENT_PREDICATE_TYPE`，谓词类型自身实现
`DataComponentPredicate`（见 4.2）。

---

## 5. 配方体系

### 5.1 `RecipeSerializer` 变 final 类，注册改 `new RecipeSerializer<>(CODEC, STREAM_CODEC)`

**变化**：1.21.1 中可被继承实现 `Serializer<T>` 的 `RecipeSerializer` 接口在 26.1 成为 final
类，自定义配方的序列化器不再「implements」，而是直接实例化：`new RecipeSerializer<>(CODEC, STREAM_CODEC)`。CODEC/STREAM_CODEC
从接口方法变为构造参数，须以 `public static final` 字段形式挂在自定义配方类上。dev/26.1 侧实际形态（`LibRecipeTypes.java`）：

```java
public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InWorldRecipe>> IN_WORLD_RECIPE_SERIALIZER =
    RECIPE_SERIALIZERS.register(
        "in_world_recipe",
        () -> new RecipeSerializer<>(InWorldRecipe.Serializer.CODEC, InWorldRecipe.Serializer.STREAM_CODEC)
    );
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/init/recipe/LibRecipeTypes.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java`（`Serializer` 内 `CODEC` 改为
  `public static final`）

**迁移注意**：回迁自定义配方时把「序列化器类」改为「序列化器常量」，`RecipeType` 的注册方式（
`DeferredRegister<RecipeType<?>>` + `new RecipeType<>() {}`）不变。

### 5.2 `RecipeManager` 内部结构 `RecipeMap`

**变化**：1.21.2+ 起 `RecipeManager` 内部按 key 索引的
`byName: Map<ResourceLocation, RecipeHolder<?>>` 与 `byType: Multimap<...>` 重构为 `net.minecraft.world.item.crafting.RecipeMap`（按 `ResourceKey<Recipe<?>>`
索引），对 `RecipeManager` 的注入/访问逻辑迁移到 `RecipeMapMixin`。dev/26.1 侧实际形态（`RecipeMapMixin.java`）：

```java

@Mixin(RecipeMap.class)
public class RecipeMapMixin implements IRecipeMapExtension {
    @Mutable
    @Final
    @Shadow
    private Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey; // 遍历 key 时经 key.identifier() 取 Identifier
}
```

同时 `RecipeHolder.id()` 返回 `ResourceKey`，取 `Identifier` 需 `.id().identifier()`（如 `InWorldRecipeManager` 中
`recipe.id().identifier()`）。

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/mixin/RecipeMapMixin.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/injection/IRecipeMapExtension.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/mixin/RecipeManagerMixin.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipeManager.java`

**迁移注意**：注入目标从 `RecipeManager` 换到 `RecipeMap` 后，`@Shadow` 字段类型与索引 key 类型（`ResourceKey<Recipe<?>>`
而非 `Identifier`）必须同步；`anvillib$getRegistries` 在 dev 侧已 `@Deprecated`，新代码不要依赖。

### 5.3 `RecipeBuilder.save(RecipeOutput, ResourceKey<Recipe<?>>)`

**变化**：26.1 中 `RecipeBuilder.save(...)` 与进度触发均从「资源位置」改为「配方 key」：

| 旧（1.21.1）                                          | 新（26.1）                                                                   |
|-------------------------------------------------------|------------------------------------------------------------------------------|
| `builder.save(recipeOutput, ResourceLocation)`        | `builder.save(recipeOutput, ResourceKey<Recipe<?>>)`，配套新增 `defaultId()` |
| `RecipeUnlockedTrigger.unlocked(ResourceLocation)`    | `RecipeUnlockedTrigger.unlocked(ResourceKey<Recipe<?>>)`                     |
| `AdvancementRewards.Builder.recipe(ResourceLocation)` | `AdvancementRewards.Builder.recipe(ResourceKey<Recipe<?>>)`                  |

dev/26.1 侧实际形态（`InWorldRecipeBuilder.java`）：

```java
public ResourceKey<Recipe<?>> defaultId() { ...}

public void save(RecipeOutput recipeOutput, ResourceKey<Recipe<?>> key) { ...}
// 为兼容保留 save(RecipeOutput, Identifier) 重载
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/builder/InWorldRecipeBuilder.java`

**迁移注意**：dev 侧保留了 `save(RecipeOutput, Identifier)` 重载作兼容，回迁代码可沿用旧签名；但进度解锁路径必须传
`ResourceKey`。

### 5.4 `RecipeUnlockedTrigger` 包移动

**变化**：`RecipeUnlockedTrigger` 随 `critereon` → `criterion` 官方包改名（见 2.3）移动：
`net.minecraft.advancements.critereon.RecipeUnlockedTrigger` →
`net.minecraft.advancements.criterion.RecipeUnlockedTrigger`。

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/builder/InWorldRecipeBuilder.java`

**迁移注意**：纯 import 替换，无 API 形态变化；与 5.3 的 `unlocked(ResourceKey)` 参数变化配套出现。

### 5.5 `Level.getRecipeManager()` → `serverLevel.recipeAccess()`

**变化**：26.1 中获取配方访问入口改为 `ServerLevel#recipeAccess()`（`RecipeAccess`），`Level.getRecipeManager()`
在配方相关访问路径中不再使用。dev/26.1 侧实际形态（`ItemEntityEventListener.java`）：

```java
InWorldRecipeManager manager = serverLevel.recipeAccess().anvillib$getInWorldRecipeManager();
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/event/listener/ItemEntityEventListener.java`

**迁移注意**：仅在持有 `ServerLevel` 时可用；`ClientLevel` 侧没有 `recipeAccess()`，客户端若需访问配方走其他入口（datapack
同步后的客户端缓存）。另注意 `Recipe` 接口在 26.1 的签名变化：`assemble(RecipeInput, HolderLookup.Provider)` →
`assemble(RecipeInput)`；`canCraftInDimensions(...)` 等旧方法被 `showNotification()` / `group()` / `placementInfo()`（
`PlacementInfo` 字段，须显式提供）/ `recipeBookCategory()` 取代（`module.recipe/.../InWorldRecipe.java`）；客户端
`RecipesUpdatedEvent` 在 26.1 已移除（`module.recipe/.../event/listener/ResourceEventListener.java` 仅保留
`ServerStartedEvent`/`OnDatapackSyncEvent` 监听）。

---

## 6. 网络

### 6.1 `PayloadRegistrar` 双向注册 3 参 → 4 参

**变化**：NeoForge 26.1 的 `PayloadRegistrar` 双向（BIDIRECTIONAL）注册方法从 3 参数变为 4 参数，client/server 侧 handler
拆开传入：`playBidirectional(type, codec, handler)` → `playBidirectional(type, codec, handler, handler)`（其余
`configurationBidirectional`/`commonBidirectional` 同理）。dev/26.1 侧实际形态（`NetworkRegistrar.java`）：

```java
case BIDIRECTIONAL ->
    registrar.

playBidirectional(data.type(),data.

streamCodec(),data.

handler(),data.

handler());
```

**涉及文件**：

- `module.network/src/main/java/dev/anvilcraft/lib/v2/network/register/NetworkRegistrar.java`

**迁移注意**：AnvilLib 的 `IPacket` 体系单 handler 即可满足双向语义，4 参写法是同一 handler 传两次；若回迁代码自定义双向包，需按
client/server 分别提供。

### 6.2 `LoadingModList.get()` → `FMLLoader.getCurrent().getLoadingModList()`

**变化**：`LoadingModList.get()` 静态访问在 26.1 不可用，改为 `FMLLoader.getCurrent()` 实例的 `getLoadingModList()`
。dev/26.1 侧实际形态（`NetworkRegistrar.java`，用于扫描 `@Network` 注解包）：

```java
IModFileInfo fileInfo = FMLLoader.getCurrent().getLoadingModList().getModFileById(modId);
```

**涉及文件**：

- `module.network/src/main/java/dev/anvilcraft/lib/v2/network/register/NetworkRegistrar.java`
- `module.integration/src/main/java/dev/anvilcraft/lib/v2/integration/IntegrationManager.java`（同款改造，另配套
  `@SuppressWarnings("UnstableApiUsage")`）

**迁移注意**：`FMLLoader.getCurrent()` 返回的实例在加载期外仍可调用，但部分方法标注 `UnstableApiUsage`，需保留 dev 侧的抑制注解。

### 6.3 `IPacket.type()` 返回 `Identifier`

**变化**：`CustomPacketPayload.Type<T>` 的构造参数从 `ResourceLocation` 改为 `Identifier`（网络包类型标识随 2.1
改名）。dev/26.1 侧实际形态（`IPacket.java`）：

```java
import net.minecraft.resources.Identifier;

static <T extends IPacket> Type<T> type(Identifier id) { ...}
```

**涉及文件**：

- `module.network/src/main/java/dev/anvilcraft/lib/v2/network/packet/IPacket.java`

**迁移注意**：`CustomPacketPayload` 接口在 26.1 本身要求 `type()` 返回 `Type<Identifier>`，随 2.1 机械替换即可，网络注册侧（
`NetworkRegistrar`）无需额外适配。

---

## 7. 注册表与延迟注册

### 7.1 `Registry.get(RL)` → `getOptional` / `getValue`

**变化**：`Registry.get(ResourceLocation)` 在 26.1 改名/移除，dev 侧按使用场景二选一：

| 旧（1.21.1）                     | 新（26.1）                                                                 | 场景          |
|----------------------------------|----------------------------------------------------------------------------|---------------|
| `BuiltInRegistries.ITEM.get(id)` | `BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR)`                 | 有默认值兜底  |
| `ENTITY_TYPE.get(id)`            | `ENTITY_TYPE.getValue(id)`（或 `containsKey` 预检 + `getOptional` 双保险） | 直接取值/判空 |

dev/26.1 侧实际形态（`InWorldRecipe.java` STREAM_CODEC 解码侧）：

```java
IRecipeTrigger trigger = Objects.requireNonNull(LibRegistries.TRIGGER_REGISTRY.getValue(buf.readIdentifier()));
```

**涉及文件**：

- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/CodecUtil.java`（`ITEM`/`BLOCK`/`ENTITY` 字段）
- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/StreamCodecUtil.java`（`ENTITY` 字段）
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java`（`getValue`）

**迁移注意**：`getOptional` 返回 `Optional`（26.1 移除 `get` 后无「返回 null」语义的旧入口），所有 `get(id)` 调用点必须按「兜底
orElse / 显式判空」改写，不能只换方法名。

### 7.2 tag 解析需 `HolderGetter`

**变化**：26.1 延迟注册体系要求 tag 解析持有 `HolderGetter`（不能静态访问 `BuiltInRegistries`），相关 `Builder.of(...)`
入口从「tag key」升级为「`HolderGetter` + tag key」：

```java
// 旧：BlockStatePredicate.Builder.of(TagKey)
// 新（dev/26.1 BlockStatePredicate.java）：
public Builder of(HolderGetter<Block> blocks, TagKey<Block> tag) { ...}
```

`module.recipe` 侧 `HasBlock`/`HasBlockIngredient`/`HasItem`/`HasItemIngredient` 的 builder 层 `hasItem`/`hasBlock` 系列
12 个方法签名同步增加 `HolderGetter` 参数。

**涉及文件**：

- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/BlockStatePredicate.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/predicate/item/HasBlock.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/predicate/item/HasItem.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/ItemPredicate.java`

**迁移注意**：`HolderGetter` 来源一般是 `level.registryAccess()`（服务器/数据包环境）或 `registry` 自身；datapack 环境不能再走
`BuiltInRegistries` 静态路径。

### 7.3 `Registry.getTag` → `getTagOrEmpty`

**变化**：`Registry.getTag(TagKey)`（返回 `Optional`）在 26.1 改名/移除，dev 侧改用 `getTagOrEmpty(TagKey)`（返回 `HolderSet`
可空语义）。`ItemCache.java` 的 tag 命中判断与 `Entity.getType().is(TagKey)` → `Entity.is(TagKey)` 同批适配。

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/ItemCache.java`

**迁移注意**：`getTagOrEmpty` 不再返回 `Optional`，判空逻辑从 `if (opt.isPresent())` 改为对 `HolderSet` 本身判空。

---

## 8. 渲染管线

### 8.1 RenderState 架构：`extractRenderState` / `submit` 两段式

**变化**：1.21.2+ 引入的 RenderState 架构把「渲染器每帧提交」拆为两段：`extractRenderState(...)`
在帧开始阶段从实体/方块实体提取渲染状态（不可变快照），
`submit(RenderState, PoseStack, SubmitNodeCollector, CameraRenderState)` 在渲染提交阶段消费快照。旧 `render(...)`
注入点全部改写。dev/26.1 侧实际形态（`PistonHeadRendererMixin.java`）：

```java
// 注入 extractRenderState（在 createMovingBlock 调用点，预生成内部方块实体的 RenderState）
private void extractRenderState(
    PistonMovingBlockEntity blockEntity, PistonHeadRenderState state,
    float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer$CrumblingOverlay breakProgress
) { ...}
// 注入 submit（TAIL，负责平移并提交渲染）：@Inject(method = "submit(", at = @At("TAIL"))
```

新类型：`net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState`、`BlockEntityRenderState`、
`MovingBlockRenderState`、`SubmitNodeCollector`、`state.level.CameraRenderState`、
`feature.ModelFeatureRenderer$CrumblingOverlay`。位移数据从 `blockEntity.getXOff(partialTick)` 改为
`state.xOffset/yOffset/zOffset`。

**涉及文件**：

- `module.moveable-entity-block/src/main/java/dev/anvilcraft/lib/v2/piston/mixin/PistonHeadRendererMixin.java`
-
`module.moveable-entity-block/src/main/java/dev/anvilcraft/lib/v2/piston/injection/IPistonHeadRenderStateExtension.java`
（扩展 `PistonHeadRenderState`，`anvillib$setExtraState`/`anvillib$getExtraState`）
- `module.moveable-entity-block/src/main/java/dev/anvilcraft/lib/v2/piston/mixin/PistonHeadRenderStateMixin.java`
- `module.moveable-entity-block/src/main/resources/interface_injections.json`

**迁移注意**：port 侧 `1acdcf8` 的「PoseStack 提前 return 未清空」bug 在新架构下天然规避（`submit` 中 `return` 位于
`pushPose()` 之前、`popPose()` 无条件执行）；回迁 mixin 时 `@Local` 变量名也随反混淆名变化（如 `blockpos3` → `pos`、
`blockstate` → `newState`）。

### 8.2 `BlockEntityRenderer` 泛型化为 `<BE, RenderState>`

**变化**：`BlockEntityRenderer<BE>` → `BlockEntityRenderer<BE, RenderState>`，
`getBlockEntityRenderDispatcher().getRenderer()` 相应提供双泛型重载。dev/26.1 侧实际形态：

```java
abstract class PistonHeadRendererMixin implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
    // renderer.extractRenderState(blockEntity1, renderState, partialTicks, cameraPosition, breakProgress)
}
```

**涉及文件**：

- `module.moveable-entity-block/src/main/java/dev/anvilcraft/lib/v2/piston/mixin/PistonHeadRendererMixin.java`

**迁移注意**：`extractRenderState` 需要在 RenderState 已创建后调用（dev 侧在 `createMovingBlock` 调用点注入保证时序），不要自行
new RenderState。

### 8.3 `GuiGraphics` → `GuiGraphicsExtractor`

**变化**：GUI 渲染提取阶段的操作入口从 `GuiGraphics` 改为 `GuiGraphicsExtractor`（`net.minecraft.client.gui`），文本绘制
`drawCenteredString(...)` → `centeredText(...)`。dev/26.1 侧实际形态：

```java
// WheelScreen.java：guiGraphics.centeredText(font, pageInfo, width / 2, height - 22, 0xFFFFFFFF)
```

**涉及文件**：

- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/screen/WheelScreen.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/component/WheelWidget.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/api/WheelEntryRenderer.java`

**迁移注意**：`GuiGraphicsExtractor` 只能收集「绘制描述」不直接操作 GL；跨层传递时携带 `peekScissorStack()`（dev 侧
`submitGuiElementRenderState(...)` 的配套调用）。

### 8.4 `PoseStack` → `Matrix3x2fStack`

**变化**：GUI/2D 渲染的矩阵栈在 26.1 改为 `org.joml.Matrix3x2fStack`（ **无 z 分量**）：`pushPose/popPose` →
`pushMatrix/popMatrix`，`translate/scale` 调用去掉 z 参数。dev/26.1 侧 `WheelWidget.java` 全文按此改写（SDF
图形、环形选择特效的绘制路径）。

**涉及文件**：

- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/component/WheelWidget.java`

**迁移注意**：带 z 的 3D 变换（如方块实体渲染）仍用 `PoseStack`，只有 GUI 2D 路径换 `Matrix3x2fStack`；回迁时按调用上下文区分，不要全局替换。

### 8.5 `RenderPipeline` + UBO + `DynamicUniformStorage` 取代 `ShaderInstance`/`RegisterShadersEvent`/shader json

**变化**：26.1 移除 `ShaderInstance` 加载与 `RegisterShadersEvent`，改由 `RenderPipeline` 声明式管线 + uniform block（UBO）+
`DynamicUniformStorage` 管理 shader 参数：

| 旧（1.21.1）                                                 | 新（26.1）                                                                                 |
|--------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `RegisterShadersEvent` + `ShaderInstance` + `safeGetUniform` | `com.mojang.blaze3d.pipeline.RenderPipeline.builder()` 声明管线                            |
| shader json（`assets/.../shaders/core/*.json`）              | 管线注册资源位置（`withLocation(...)`），shader 源文件保留 `.fsh`/`.vsh`                   |
| `RenderSystem.setShader(...)`                                | `GuiGraphicsExtractor.submitGuiElementRenderState(...)` 提交渲染状态                       |
| 顶点 uniform 逐帧 set                                        | `UniformType.UNIFORM_BUFFER`（`layout(std140)` uniform block）+ `DynamicUniformStorage<U>` |

dev/26.1 侧实际形态（`LibRenders.java` + `LibDynamicUniforms.java`）：

```java
public static final RenderPipeline RING_PIPELINE = RenderPipeline.builder(SNIPPET_COMMON)
    .withLocation(AnvilLibWheel.of("pipeline/ring"))
    .withVertexShader("core/position_color")
    .withFragmentShader(AnvilLibWheel.of("core/ring"))
    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
    .withUniform("RingUniform", UniformType.UNIFORM_BUFFER)
    .build();

public record RingUniform(Vector2fc center, float innerDiameter, float outerDiameter, float antiAliasingRadius)
    implements DynamicUniformStorage.DynamicUniform { ...
}

private final DynamicUniformStorage<RingUniform> ringUbo = new DynamicUniformStorage<>(...);
```

配套的 `Std140Builder`/`Std140SizeCalculator`（`com.mojang.blaze3d.buffers`）负责 UBO 内存布局；
`ConfigureMainRenderTargetEvent` 初始化 UBO、`RenderGuiEvent.Post` 的 `endFrame` 重置 UBO。

**涉及文件**：

- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/init/LibRenders.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/init/LibDynamicUniforms.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/component/WheelWidget.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/AnvilLibWheel.java`

**迁移注意**：旧 `LibShaders.java` 与 shader json 不迁移（dev 侧已删除）；自定义 shader 需按 `std140` 布局改写 uniform
声明并登记到 `DynamicUniformStorage`。

### 8.6 `AbstractWidget.render` → `extractRenderState`

**变化**：`AbstractWidget.render(GuiGraphics, int, int, float)` →
`extractRenderState(GuiGraphicsExtractor, int, int, float)`，`renderWidget` → `extractWidgetRenderState`；`Screen.render`
同步拆为 `extractRenderState` + 新增 `extractBackground` 覆写。AT 文件目标方法名同步改名。dev/26.1 侧实际形态（
`WheelWidget.java` + `WheelScreen.java`）：

```java
// WheelWidget.java:511
public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
    // ...
    this.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
}

// WheelScreen.java:91
public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) { ...}
```

**涉及文件**：

- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/component/WheelWidget.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/screen/WheelScreen.java`
- `module.wheel/src/main/resources/META-INF/accesstransformer.cfg`（
  `public-f ... extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V`）

**迁移注意**：自定义控件回迁时覆写方法名与 AT 目标必须同步改为 `extractRenderState`；绘制逻辑放在提取阶段，GL 提交由
`submit` 阶段（控件内部或 `submitGuiElementRenderState`）完成。

---

## 9. 事件与加载器

### 9.1 `FMLLoader.getDist()` → `FMLLoader.getCurrent().getDist()`

**变化**：`FMLLoader.getDist()` 静态方法在 26.1 移除，统一走 `FMLLoader.getCurrent().getDist()`；`FMLEnvironment.dist`
字段同样改为 `FMLEnvironment.getDist()`。dev/26.1 侧实际形态（`ConfigManager.java`）：

```java
if(FMLLoader.getCurrent().

getDist().

isClient())manager.

registerScreen(container);
```

**涉及文件**：

- `module.config/src/main/java/dev/anvilcraft/lib/v2/config/ConfigManager.java`
- `module.integration/src/main/java/dev/anvilcraft/lib/v2/integration/IntegrationManager.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/DistExecutor.java`
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/Util.java`

**迁移注意**：除加载器静态访问外，dev 侧还删除了部分 `@OnlyIn(Dist.CLIENT)` 标注（`ConfigManager`），以 `getDist()`
运行时判断替代；回迁时不要引入新的 `@OnlyIn` 依赖客户端侧代码。`ModList.get().getModContainerById(...)` 在 dev 侧改为
`Optional.of(ModList.get()).flatMap(...)` 链式规避空值（`module.config/.../ConfigManager.java`）。

### 9.2 `GatherDataEvent` → `GatherDataEvent.Client` / `GatherDataEvent.Server`

**变化**：NeoForge 26.1 把 `GatherDataEvent` 按物理端拆分为 `GatherDataEvent.Client` 与 `GatherDataEvent.Server`，
`@SubscribeEvent` 方法签名对应拆分。dev/26.1 侧实际形态（`module.recipe` 的 `AnvilLibDatagen.java`）：

```java

@SubscribeEvent
public static void gatherData(GatherDataEvent.Client event) { ...}

@SubscribeEvent
public static void gatherData(GatherDataEvent.Server event) { ...}
```

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/data/gen/AnvilLibDatagen.java`
- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/data/AnvilLibDatagen.java`（仅
  `GatherDataEvent.Client`，lang provider 属于客户端）

**迁移注意**：只有服务端数据的 provider（如配方、loot table）挂 `Server`，语言/模型等挂 `Client`；注意这是「事件签名拆分」，与
13.1 的 `REGISTRUM.addDataGenerator` 是两条可并存的注册通道。

### 9.3 `BlockEvent.BreakEvent` → `BreakBlockEvent`

**变化**：NeoForge 26.1 方块事件重构：`net.neoforged.neoforge.event.level.BlockEvent.BreakEvent` →
`net.neoforged.neoforge.event.level.block.BreakBlockEvent`（独立顶级类）。dev/26.1 侧实际形态（`BlockEventListener.java`）：

```java
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
```

**涉及文件**：

- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/event/BlockEventListener.java`

**迁移注意**：import 与 `@SubscribeEvent` 参数类型同步替换；事件字段（`getPlayer()`/`getPos()` 等）在 26.1 未变。

### 9.4 输入事件 `KeyEvent` / `MouseButtonEvent`

**变化**：26.1 客户端输入事件从「原语参数」改为事件对象：`InputEvent.Key.matches(int, int)` → `matches(KeyEvent)`（
`event.getKeyEvent()` 取原始键事件）；`Screen.keyPressed(int, int, int)` → `keyPressed(KeyEvent)`；
`mouseClicked(int, int, int)` → `mouseClicked(MouseButtonEvent, boolean isDoubleClick)`。dev/26.1 侧实际形态（
`WheelTestClientHandler.java` + `WheelScreen.java`）：

```java
WheelTestKeys.HOLD_KEY.matches(event.getKeyEvent());
// Screen：keyPressed(KeyEvent event) / mouseClicked(MouseButtonEvent event, boolean isDoubleClick)
```

`KeyMapping` 的 category 参数也从 `String` 改为 `new KeyMapping.Category(Identifier)`。

**涉及文件**：

- `module.test/src/main/java/dev/anvilcraft/lib/v2/test/wheel/WheelTestClientHandler.java`
- `module.test/src/main/java/dev/anvilcraft/lib/v2/test/wheel/WheelTestKeys.java`
- `module.wheel/src/main/java/dev/anvilcraft/lib/v2/wheel/client/gui/screen/WheelScreen.java`

**迁移注意**：`KeyEvent`/`MouseButtonEvent` 均为 `net.minecraft.client.input` 包新类型；`InputEvent.Key` 的旧
`matches(int,int)` 重载已移除，按键绑定判断统一传事件对象。

---

## 10. NBT 与持久化

### 10.1 `CompoundTag` + `HolderLookup` → `ValueInput` / `ValueOutput`

**变化**：26.1 实体/方块实体的 NBT 读写入口从「`CompoundTag` + `HolderLookup.Provider`」改为流式读写器 `ValueInput`/
`ValueOutput`（`net.minecraft.world.level.storage`）：

| 旧（1.21.1）                                                                         | 新（26.1）                                           |
|--------------------------------------------------------------------------------------|------------------------------------------------------|
| `loadAdditional(CompoundTag, HolderLookup.Provider)`                                 | `loadAdditional(ValueInput)`                         |
| `saveAdditional(CompoundTag, HolderLookup.Provider)`                                 | `saveAdditional(ValueOutput)`                        |
| `Codec.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)` + `Pair` | `valueInput.read("id", codec)`（返回 `Optional`）    |
| `input.contains(name)` / `getCompound(name)`                                         | `input.child(name)`（返回 `Optional<ValueInput>`）   |
| `tag.getInt(name)`                                                                   | `valueInput.getIntOr(name, def)`                     |
| `blockEntity.loadWithComponents(tag, registries)`                                    | `blockEntity.loadWithComponents(valueInput)`（单参） |

dev/26.1 侧实际形态（`PistonMovingBlockEntityMixin.java`）：

```java
private void loadAdditional(ValueInput input, CallbackInfo ci) {
    Optional<ValueInput> child = input.child(anvillib$MOVEABLE_BLOCK_ENTITY);
    if (child.isEmpty()) return;
    Optional<BlockEntityType<?>> entityType = child.get().read("id", anvillib$TYPE_CODEC);
    if (entityType.isEmpty()) return;
    // 坐标经 getIntOr("x"/"y"/"z", 0) 读取，随后 create 内部方块实体并 loadWithComponents
    this.anvillib$blockEntity.loadWithComponents(child.get());
}

private void saveAdditional(ValueOutput output, CallbackInfo ci) {
    ValueOutput child = output.child(anvillib$MOVEABLE_BLOCK_ENTITY);
    this.anvillib$blockEntity.saveWithFullMetadata(child);
}
```

**涉及文件**：

- `module.moveable-entity-block/src/main/java/dev/anvilcraft/lib/v2/piston/mixin/PistonMovingBlockEntityMixin.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/BlockCache.java`（
  `TagValueInput.create(ProblemReporter.ScopedCollector, registryAccess, nbt)` + `entity.problemPath()`）
- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/predicate/NbtPredicate.java`（
  `entity.saveWithoutId(TagValueOutput)`）

**迁移注意**：`ValueInput`/`ValueOutput` 的 `child()` 返回 `Optional`，读写路径必须显式判空；
`ProblemReporter.ScopedCollector` 用于 `ValueInput` 构造时的错误收集（`entity.problemPath()` 取路径），回迁 NBT 代码时不要遗漏。

### 10.2 `SavedData.Factory` → `SavedDataType<T>` + Codec

**变化**：26.1 的 `SavedData` 注册从「`SavedData.Factory` + `computeIfAbsent(name, Factory)` + 手写
`load/save(CompoundTag, HolderLookup)`」改为「`SavedDataType<T>` + Codec 序列化」。dev/26.1 侧实际形态（
`DynamicMultiblockManager.java`）：

```java
public static final Codec<DynamicMultiblockManager> CODEC = RecordCodecBuilder.create(inst -> inst.group(
    MultiblockState.CODEC.codec() // ...
).apply(inst, DynamicMultiblockManager::new));
public static final SavedDataType<DynamicMultiblockManager> TYPE = new SavedDataType<>(/* key / typeToken / CODEC / 默认工厂 */);
```

**涉及文件**：

- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/DynamicMultiblockManager.java`
- `module.multiblock/src/main/java/dev/anvilcraft/lib/v2/multiblock/dynamic/MultiblockState.java`（`CODEC`、
  `DEFINITION_KEY_CODEC`）

**迁移注意**：`MultiblockState` 的 `CODEC`/`STREAM_CODEC` 同时服务持久化与网络（`ResourceKey.streamCodec(...)` 只传 key，收端
`getDefinition(level.registryAccess())` 延迟解析），回迁时注意「持久化 Codec 与网络 StreamCodec 分离」的 26.1 写法。

---

## 11. 能力（capability）

### 11.1 物品 `IItemHandler` → `ResourceHandler<ItemResource>`

**变化**：NeoForge 26.1 用统一资源处理器体系取代物品能力接口：`net.neoforged.neoforge.items.IItemHandler` →
`net.neoforged.neoforge.transfer.ResourceHandler<T>`（物品场景 T = `net.neoforged.neoforge.transfer.item.ItemResource`
）。方法对照：

| 旧 `IItemHandler`                     | 新 `ResourceHandler<T>`                                             |
|---------------------------------------|---------------------------------------------------------------------|
| `getSlots()`                          | `size()`                                                            |
| `getStackInSlot(int)`                 | `getResource(int)`                                                  |
| `insertItem(int, ItemStack, boolean)` | `insert(int, T, int amount, TransactionContext)`（返回插入数量）    |
| `extractItem(int, int, boolean)`      | `extract(int, T, int amount, TransactionContext)`（返回提取数量）   |
| `setStackInSlot` / `setResource`      | **不存在**，替换语义用 Transaction 内 `extract` + `insert` 组合实现 |

dev/26.1 侧实际形态（`ItemResourceHandlerCache.java`，26.1.2.76 sources jar 中 `ResourceHandler` 源码确认无
`setResource`）：

```java
ResourceHandler<ItemResource> getInput();

ResourceHandler<ItemResource> getOutput();
// 写路径：Transaction.openRoot() 内 extract + insert
```

`IItemHandlerCache`/`ItemHandlerCacheElement` 同步重命名为 `ItemResourceHandlerCache`/`ItemResourceHandlerCacheElement`
；实体/方块实体的物品访问改为 `instanceof Container` + `VanillaContainerWrapper.of(...)` 包装。

**涉及文件**：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/ItemResourceHandlerCache.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/item/ItemResourceHandlerCacheElement.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/ItemCache.java`

**迁移注意**：`insert`/`extract` 返回「实际数量」而非栈对象，且必须在 `Transaction` 内执行（`Transaction.openRoot()`
或外层事务上下文）；「替换槽位内容」一律改写为事务内先 `extract` 后 `insert`（`ResourceHandler` 无 `setResource`，见
`docs/branch-comparison.md` #3 的适配说明）。

### 11.2 流体能力：`IFluidHandler` 弃用 → `ResourceHandler<FluidResource>`

**变化**（基于 NeoForge 26.1.2.76 sources jar 实测）：

1. 旧 API 仍存在但已弃用：`net.neoforged.neoforge.fluids.capability.IFluidHandler` 与
   `net.neoforged.neoforge.fluids.FluidStack` 可编译，但 `IFluidHandler` 标注
   `@Deprecated(since = "1.21.9", forRemoval = true)`（`getTanks`/`getFluidInTank`/`fill`/`drain` 均带弃用标注），并提供迁移包装器
   `IFluidHandler.of(ResourceHandler<FluidResource>)`。
2. 新 API：`net.neoforged.neoforge.transfer.ResourceHandler<FluidResource>`，方法为 `getResource`/`extract`/`insert`/
   `isValid`/`getCapacityAsInt`/`size`， **无 `setResource`**；`FluidResource`（
   `net.neoforged.neoforge.transfer.fluid.FluidResource`）为不可变类，实现 `DataComponentHolderResource<Fluid>`，静态字段
   `CODEC`/`OPTIONAL_CODEC`/`STREAM_CODEC`/`EMPTY`。
3. 配套类：`FluidUtil`、`BucketResourceHandler`、`CauldronWrapper`、`FluidStacksResourceHandler` 等（`transfer/fluid` 包）。

```java
// 新 API 形态（26.1.2.76 sources jar）
public final class FluidResource implements DataComponentHolderResource<Fluid> {
    public static final FluidResource EMPTY = ...;
    public static final Codec<FluidResource> CODEC = ...;
    public static final Codec<FluidResource> OPTIONAL_CODEC = ...;
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidResource> STREAM_CODEC = ...;
}
```

| 迁移选项     | 做法                                                                 | 代价                                        | 建议     |
|--------------|----------------------------------------------------------------------|---------------------------------------------|----------|
| A 最小移植   | 继续使用已弃用的 `IFluidHandler`/`FluidStack`                        | 可编译但有弃用告警；`forRemoval` 后必须再迁 | 仅兜底   |
| B 适配新 API | 改用 `ResourceHandler<FluidResource>`，Transaction 内 insert/extract | 与 11.1 物品能力迁移方向一致，一步到位      | **推荐** |

**涉及文件**：`module.yukkuri`（port 侧待迁模块，dev/26.1 无此模块）；参考 dev 侧物品能力形态：

- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/ItemResourceHandlerCache.java`
- `module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/cache/item/ItemResourceHandlerCacheElement.java`

**迁移注意**：流体槽位语义与物品一致（`size()`/`getResource(int)`）；桶灌装/倾倒等操作走 `FluidUtil`/`BucketResourceHandler`
而不是手写 `fill`/`drain` 兼容层。

### 11.3 `BlockCapability.createSided` 改用 `Identifier`

**变化**：`BlockCapability.createSided(ResourceLocation, Class<T>)` → `createSided(Identifier, Class<T>)`（26.1.2.76
sources jar 实测签名
`public static <T> BlockCapability<T, @Nullable Direction> createSided(Identifier name, Class<T> typeClass)`），仅标识符类型随
2.1 改名，方法本体与 `BlockCapabilityCache` 等配套不变。

**涉及文件**：capability 定义侧为 port `module.yukkuri`（待迁）；dev/26.1 侧无 `createSided` 调用点（物品能力已走
`ResourceHandler`）。

**迁移注意**：capability 对象定义处替换 `Identifier` 即可；NeoForge 内置 capability 常量在 26.1 仍可用，但类型已整体迁移——
`Capabilities.ItemHandler.BLOCK` 现在声明为 `BlockCapability<ResourceHandler<ItemResource>, @Nullable Direction>`（
`Capabilities.Fluid.BLOCK` 同理为 `ResourceHandler<FluidResource>`，26.1.2.76 sources jar 实测），查询方类型必须跟随。

### 11.4 `Level.getCapability` 签名核对

**变化**：26.1 下 `Level#getCapability(BlockCapability, BlockPos, Object)` 签名保持不变（26.1.2.76 sources jar
`BlockCapability.java` javadoc 明文 `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)`），因此 1.21.1 的
`level.getCapability(cap, pos, direction)` 调用形态无需改动，需要核对的是 capability 对象本身的类型参数与创建方式（见
11.3）。

**涉及文件**：`module.yukkuri`（待迁，port 侧流体能力查询点）。

**迁移注意**：回迁时逐个核对 capability 查询点的泛型参数是否已换为 `ResourceHandler<FluidResource>`（若选选项 B）；签名本身无变化。
**待验证**：port 侧 `module.yukkuri` 中 `level.getCapability` 的全部调用点清单，验证方法——
`git grep -n "getCapability" port/1.21.1 -- module.yukkuri` 收集调用点，逐点对照 26.1.2.76 sources jar 的
`BlockCapability.java`/`EntityCapability.java` 泛型签名。

---

## 12. 注解体系

### 12.1 javax/neoforge 注解 → jspecify `@NullMarked` / `@Nullable`

**变化**：dev/26.1 全仓库空注解体系统一迁移到 jspecify（`f94126f`）：

| 旧                                                                                               | 新                                                                |
|--------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| `net.minecraft.MethodsReturnNonnullByDefault` + `javax.annotation.ParametersAreNonnullByDefault` | `org.jspecify.annotations.NullMarked`（包级 `package-info.java`） |
| `javax.annotation.Nullable`                                                                      | `org.jspecify.annotations.Nullable`                               |
| `javax.annotation.Nonnull`（jetbrains `@NotNull` 等）                                            | `@NullMarked` 覆盖，删除显式标注                                  |

dev/26.1 侧实际形态（各模块 `package-info.java`）：

```java
@NullMarked
package dev.anvilcraft.lib.v2.util;
```

**涉及文件**（代表性，实际覆盖全部 19 个模块）：

- `module.util/src/main/java/dev/anvilcraft/lib/v2/util/package-info.java`
-
`module.moveable-entity-block/src/main/java/dev/anvilcraft/lib/v2/piston/injection/IPistonMovingBlockEntityExtension.java`
- `module.network/src/main/java/dev/anvilcraft/lib/v2/network/packet/package-info.java`
- `module.codec/src/main/java/dev/anvilcraft/lib/v2/codec/package-info.java`

**迁移注意**：回迁代码若保留 `javax.annotation.Nullable` import 会编译失败（26.1 不再依赖 javax.annotation）；`@NullMarked`
语义下非标注成员默认非空，可空字段/参数必须显式 `@Nullable`（dev 侧 `BlockStatePredicate.statesCache` 即加注 `@Nullable`）。

---

## 13. 数据生成与构建

### 13.1 `REGISTRUM.addDataGenerator` 模式替代 `GatherDataEvent` + `genInit.add`

**变化**：dev/26.1 侧 Registrum 提供声明式数据生成注册通道，取代「`AnvilLibTestDatagen` 类 + `GatherDataEvent` +
`REGISTRUM.getDataGenInitializer().add(KEY, ...)`」的旧模式。dev/26.1 侧实际形态（`AnvilLibTest.java` +
`TestLangGenerator.java`）：

```java
public void setupDataGeneration() {
    REGISTRUM.addDataGenerator(ProviderType.LANG, TestLangGenerator::accept);
}
// TestLangGenerator.accept(RegistrumLangProvider provider)：provider.add(...) + ConfigData.readConfigClass(...)
```

**涉及文件**：

- `module.test/src/main/java/dev/anvilcraft/lib/v2/test/AnvilLibTest.java`
- `module.test/src/main/java/dev/anvilcraft/lib/v2/test/data/TestLangGenerator.java`
- `module.test/src/main/java/dev/anvilcraft/lib/v2/test/all/TestBlocks.java`（`setupRegistration()` 模式，
  `defaultCreativeTab(RegistryEntry)`）

**迁移注意**：port 侧 `AnvilLibTestDatagen`（`GatherDataEvent` 版）不迁移；multiblock definition 的 datagen 注册点（port 侧
`genInit.add(DEFINITIONS_KEY, ...)`）在 dev/26.1 Registrum 的保留情况 **待验证**——验证方法：grep dev/26.1 侧
`module.registrum` 的 `addDataGenerator` 与 `getDataGenInitializer` 公开 API，确认 `DEFINITIONS_KEY` 类型的 provider
是否仍可注册。

### 13.2 根级 `module.gradle` 聚合构建

**变化**：dev/26.1 将各模块独立 `build.gradle`（约 270 行，含 plugins/neoForge runs/publishing/jreleaser/machete/lombok
全量配置）统一为根级 `module.gradle` 聚合（`ea4301f`）+ `createModule` 任务（`16b7bb5`），各模块 `build.gradle` 缩减为依赖块 +
可选 publishMods 块（如 `module.main/build.gradle` 49 行）。

**涉及文件**：

- `module.gradle`（仓库根，聚合构建核心）
- `module.recipe/build.gradle`、`module.util/build.gradle`、`module.network/build.gradle`（各 2–11 行依赖块）
- `module.main/build.gradle`（依赖块 + `publishMods` 块保留在本文件）

**迁移注意**：port 侧独立构建脚本一律不回迁；新增模块按 dev 侧 `module.gradle` 结构注册（参考 `createModule`
任务约定），不要在模块内重建旧式构建配置。

### 13.3 依赖坐标 `-neoforge-26.1`

**变化**：模块间依赖坐标后缀从 `-neoforge-1.21.1` 改为 `-neoforge-26.1`，并区分发布/本地两种形态：

```gradle
if (System.getenv("NOT_DEV") == 'true') {
    jarJar(api("dev.anvilcraft.lib:anvillib-util-neoforge-26.1:latest.release"))
} else {
    jarJar(implementation project(":anvillib-util-neoforge-26.1"))
}
```

**涉及文件**：

- `module.registrum/build.gradle`、`module.recipe/build.gradle`、`module.main/build.gradle` 等全部模块构建文件

**迁移注意**：本地用 `jarJar(implementation ...)`、发布用 `jarJar(api ...)` 是 dev 侧既定形态， **不要**把 port 侧
`jarJar(api ...)` 的本地分支写法搬回（见 `docs/branch-comparison.md` #8 注意事项）。

### 13.4 `anvillib.needRunConfig` 属性

**变化**：dev 侧 `gradle.properties` 新增模块化 run config 开关 `anvillib.needRunConfig=true` 与细分项
`anvillib.needRunConfig.data=true`（module.main/module.multiblock 等），由根级 `module.gradle` 消费，控制是否为模块生成运行配置。

**涉及文件**：

- `module.main/gradle.properties`
- `module.multiblock/gradle.properties`
- `module.test/gradle.properties`

**迁移注意**：该属性属 dev 侧构建机制，port 侧没有；回迁模块若需要 datagen/客户端运行配置，按 dev 侧现有模块的
`gradle.properties` 写法声明，不要自建 run config 块。

---

## 附录

### A. 核实命令

```bash
# 两侧 diff（本文全部差异面的原始来源）
git diff port/1.21.1 dev/26.1 -- module.recipe
# 查看 dev/26.1 侧文件的实际代码形态
git show dev/26.1:module.recipe/src/main/java/dev/anvilcraft/lib/v2/recipe/InWorldRecipe.java
# 收集 port 侧能力查询调用点（11.4 待验证项）
git grep -n "getCapability" port/1.21.1 -- module.yukkuri
```

NeoForge 26.1.2.76 sources jar 本地路径：

```
C:/Users/Administrator/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/
  1d1dabe31afb953e46fb278e1cd8f04ab8e0d9a0/neoforge-26.1.2.76-sources.jar
```

### B. 参考资料

| 资料                                                                                                                                                      | 位置                                                                                                                                                                                                                             |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 分支对比报告（回迁项 #1–#8、模块差异分类）                                                                                                                | `docs/branch-comparison.md`                                                                                                                                                                                                      |
| 8 个并行子代理模块级调研报告（codec/util/network/recipe/registrum/multiblock/moveable-entity-block/main/config/integration/wheel/test 的 API 变化点明细） | `C:/Users/Administrator/.kimi-code/sessions/wd_anvillib_031cb785b7e3/session_6033f19b-f4ef-481a-b18c-d626cc22c314/agents/main/tool-results/AgentSwarm-call_00_nh47UIFAaqo9d96levr46332-594fb004-9f1f-4c9e-b230-43bc1440211d.txt` |
| NeoForge 26.1.2.76 sources jar（`ResourceHandler`/`FluidResource`/`IFluidHandler`/`BlockCapability` 源码）                                                | 见附录 A                                                                                                                                                                                                                         |

### C. 待验证事项

| #  | 事项                                                                                                                                                                            | 验证方法                                                                                                                                                          |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| V1 | `Level.getCapability` 在 port 侧 `module.yukkuri` 的全部调用点清单与泛型适配（11.4）                                                                                            | `git grep -n "getCapability" port/1.21.1 -- module.yukkuri` 收集调用点，逐点对照 26.1.2.76 sources jar 的 `BlockCapability.java`/`EntityCapability.java` 泛型签名 |
| V2 | multiblock definition 的 datagen 注册（`genInit.add(DEFINITIONS_KEY, ...)`）在 dev/26.1 Registrum 是否保留（13.1）                                                              | grep dev/26.1 侧 `module.registrum` 的 `addDataGenerator`/`getDataGenInitializer` 公开 API，确认 `DEFINITIONS_KEY` 类型 provider 的注册通道                       |
| V3 | `FriendlyByteBuf.writeVec3/readVec3` 在 26.1 是否移除（`module.codec` VEC3 字段已手写 3×float 替代，double → float 精度降级）                                                   | 在 26.1 环境编译 port 侧该字段写法；若需 double 精度自实现                                                                                                        |
| V4 | `MapCodec.encodeStart(...)` 在 26.1 是否原生提供（port 侧 `CodecUtil.encodeStart` 被 dev 删除的原因）                                                                           | 26.1 环境检查 `MapCodec` 接口方法集                                                                                                                               |
| V5 | `Ingredient.CODEC_NONEMPTY` 与 `NonNullList.of(E, E...)` 在 26.1 是否移除（`CodecUtil.createIngredientListCodec` 已改 `Ingredient.CODEC` + `NonNullList.copyOf(List.of(...))`） | 26.1 环境编译旧写法，或对照 MC 26.1.2 源码                                                                                                                        |

---

*本文档仅整理「Minecraft/NeoForge API 版本差异」这一差异类别；两侧分支的业务差异、回迁项判定与执行建议见
`docs/branch-comparison.md`。*