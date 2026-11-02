# 世界内配方

## 方块状态继承与映射

`SetBlock` 默认仍然写入指定的完整状态。添加 `copyProperties()`（JSON：`copy_properties: true`）后，复制转换前方块中目标方块能够接受的同名属性值，例如铜楼梯的朝向、上下半和含水状态。

```java
SetBlock.builder()
    .block(Blocks.CUT_COPPER_STAIRS.waxed().unaffected())
    .copyProperties()
    .build();

SetBlock.builder()
    .block(Blocks.OAK_LOG)
    .mapProperty("facing", "axis", Map.of("north", "z", "south", "z", "east", "x", "west", "x"))
    .build();
```

`property_mappings` 以目标属性名为键，值为 `{ "source": "来源属性", "values": { "源值": "目标值" } }`。省略 `values` 表示直接复制序列化值。Builder 也接受 `Property<T>` 和类型化的值映射。

处理顺序为指定的目标状态、同名属性继承、显式映射。映射可以单独启用；所有映射读取同一份输入快照。缺少来源属性或没有命中的映射时保留目标值；错误的目标属性或映射值会在构建/加载配方时报错。没有对应取值的属性不会被复制。

快照在每次配方组装时更新，所以 `HasBlockIngredient` 先移除原料也不会丢失源状态，连续批次能够继承上一批次的结果。双格方块保持上下半结构，分别转换各半的其他属性。本功能只继承方块状态，不隐式复制方块实体 NBT。

AnvilCraft 的涂抹封装接入时，应在它创建结果 `SetBlock` 的位置启用 `copyProperties()`；原有 `SetBlock.fromState(...)` 和四参数构造器保持旧行为。库升级本身不会自动改变现有涂抹配方。

测试模块的 `TestRecipeGenerator` 提供可生成的继承和映射示例。新增字段支持 JSON 与网络编解码；客户端和服务端须使用匹配版本。

## 实体条件与结果

```java
InWorldRecipeBuilder.incompatible(trigger)
    .hasEntity(check -> check.of(EntityTypes.COW).range(4))
    .hasEntityIngredient(input -> input.of(EntityTypes.PIG).range(4).count(2)
        .experienceMultiplier(1.5f).dropItems(true))
    .spawnEntity(output -> output.entity(EntityTypes.CHICKEN).offset(0, 1, 0).count(2));
```

- `has_entity`：只读检测。JSON 字段为 `predicate`（原版 EntityPredicate）、`offset`（默认零）、`range`（默认三个轴均为 1）和 `count`（默认至少 1，支持原版数量区间）。Builder 的 `count(int)` 表示至少指定数量，精确或上界条件用 `count(MinMaxBounds.Ints)`。
- `has_entity_ingredient`：使用相同范围和谓词，`count` 为要消耗的正整数（默认 1）。`experience_multiplier` 默认 0，必须为有限非负数；`drop_items` 默认 false。
- `spawn_entity`：`entity` 为实体类型，`nbt` 默认空，`offset` 默认零，`count` 与 `chance` 为 NumberProvider，默认 1。生成数量沿用 SpawnItem 的 0–99 范围。

范围表示以「上下文位置 + 偏移」为中心的包围盒尺寸。使用 `predicate(builder -> ...)` 配置原版类型、标签、NBT 等组合条件，或直接传入构建好的 EntityPredicate；该方法替换先前条件。实体按距离和 UUID 稳定选择。玩家可被只读检测，但不能作为原料或生成结果。

匹配只预留实体；失败会归还占用，组装不修改真实世界。物品堆或实体物品栏一旦被占用，该实体不能再作为整只原料；反向也一样。只读存在条件不会占用资源。生成实体和奖励在资源提交成功后才出现，不参与同一上下文的后续匹配。

消耗直接移除实体，不触发伤害、死亡或击杀成就。开启物品掉落时，生物使用原生掉落生成流程（包含装备及 LivingDropsEvent），非生物保留其原有移除掉落，例如容器内容物；不会伪造玩家击杀条件。经验为生物经验奖励在无攻击者条件下乘倍率并向下取整，非生物为零，物品和经验可分别开启。奖励遵守 mob/entity drops 游戏规则，默认也会抑制容器移除时的自动掉落。

实体生成支持乘客 NBT，类型和根实体落点由配方决定，UUID 总是重新生成。未指定 NBT 的生物经过原生生成初始化。提交前失效的实体会使提交失败；沿用现有提交失败语义，不尝试回滚已经完成的其他世界修改。
