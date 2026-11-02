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

## Entity conditions and outcomes

```java
InWorldRecipeBuilder.incompatible(trigger)
    .hasEntity(check -> check.of(EntityTypes.COW).range(4))
    .hasEntityIngredient(input -> input.of(EntityTypes.PIG).range(4).count(2)
        .experienceMultiplier(1.5f).dropItems(true))
    .spawnEntity(output -> output.entity(EntityTypes.CHICKEN).offset(0, 1, 0).count(2));
```

- `has_entity` is read-only. Fields: vanilla `predicate`, `offset` (zero), `range` (1 on each axis), and `count` (at least 1). The count accepts vanilla integer bounds; builder `count(int)` means at least, while `count(MinMaxBounds.Ints)` also supports exact and maximum counts.
- `has_entity_ingredient` reserves and consumes a positive integer `count` (default 1). `experience_multiplier` defaults to 0 and must be finite and non-negative. `drop_items` defaults to false.
- `spawn_entity` accepts `entity`, `nbt` (empty), `offset` (zero), and NumberProvider `count`/`chance` (both 1). Counts use SpawnItem's 0–99 range.

Range specifies box dimensions centered on the context position plus offset. Use `predicate(builder -> ...)` for combined vanilla type, tag, NBT and other conditions, or pass an EntityPredicate directly; this replaces previously configured conditions. Candidates are ordered by distance and UUID. Players may be detected but never consumed or spawned.

Matching only reserves entities and restores reservations on failure. Whole-entity consumption conflicts with item reservations or outputs belonging to that entity in either order. Presence checks reserve nothing. Entity outputs and rewards only appear after successful resource commits and cannot feed subsequent matches in the same context.

Consumption removes entities without damage, death or kill achievements. Optional living-entity loot uses the native generation path, including equipment and LivingDropsEvent; nonliving entities retain their native removal drops such as container contents. No player kill is synthesized. Experience is the living entity's reward with no attacker, multiplied and floored; nonliving entities award zero. Item and experience options are independent and respect mob/entity drop game rules. Defaults suppress even automatic container removal drops.

Spawn NBT may contain passengers. The recipe controls the root type and position, and all UUIDs are regenerated. Mobs without explicit NBT receive native spawn initialization. Missing reserved entities fail validation before resources commit. Existing partial-commit failure semantics remain; already committed world changes are not globally rolled back.
