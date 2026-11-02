# In-world recipes

## Copying and mapping block properties

`SetBlock` still writes the configured state by default. Enable `copyProperties()` (JSON: `copy_properties: true`) to copy compatible, equally named properties from the input block, including facing, half and waterlogging.

```java
SetBlock.builder().block(Blocks.CUT_COPPER_STAIRS.waxed().unaffected()).copyProperties().build();
SetBlock.builder().block(Blocks.OAK_LOG)
    .mapProperty("facing", "axis", Map.of("north", "z", "south", "z", "east", "x", "west", "x"))
    .build();
```

`property_mappings` maps target property names to `{ "source": "source_property", "values": { "source_value": "target_value" } }`. Omit `values` to copy the serialized value directly. Typed `Property<T>` builder overloads are also available.

The order is the configured target state, copied properties, then explicit mappings. Mapping works independently of copying, and every mapping reads the same input snapshot. Missing source properties and unmapped values leave the target unchanged. Invalid target properties or mapped values fail at construction/load time. Incompatible copied values are skipped.

Each assembly batch snapshots its inputs before ingredients remove blocks. Later batches inherit earlier simulated outputs. Two-block structures preserve their halves and transform each half separately. Block entity NBT is not implicitly inherited.

AnvilCraft's coating wrapper must opt in where it constructs its result `SetBlock`. Existing `fromState(...)` calls and the four-argument constructor retain their behavior; upgrading the library alone does not enable copying in existing coating recipes.

`TestRecipeGenerator` supplies generated examples. JSON and network codecs include the new fields; client and server versions must match.
