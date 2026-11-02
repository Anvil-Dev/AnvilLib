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
