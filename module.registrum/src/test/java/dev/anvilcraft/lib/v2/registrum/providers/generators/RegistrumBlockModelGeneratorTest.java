package dev.anvilcraft.lib.v2.registrum.providers.generators;

import net.minecraft.SharedConstants;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrumBlockModelGeneratorTest {
    private final Set<Block> states = new HashSet<>();
    private final Set<Item> items = new HashSet<>();
    private final Map<Identifier, ModelInstance> models = new HashMap<>();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void vanillaFamilyBaseDoesNotGenerateAnyResourcesEvenWhenReused() {
        var generator = generator();
        generator.family(Blocks.IRON_BLOCK);
        generator.family(Blocks.IRON_BLOCK);

        assertTrue(this.states.isEmpty());
        assertTrue(this.models.isEmpty());
        assertTrue(this.items.isEmpty());
        assertTrue(generator.seenBlockstates.isEmpty());
    }

    @Test
    void familyStillGeneratesRequestedSlabAndReusesVanillaTextures() {
        // 用已注册的台阶作为输出夹具，基底仅供引用，派生方块仍应正常生成。
        generator().family(Blocks.IRON_BLOCK).slab(Blocks.STONE_SLAB);

        assertEquals(Set.of(Blocks.STONE_SLAB), this.states);
        assertEquals(Set.of(Blocks.STONE_SLAB.asItem()), this.items);
        assertEquals(2, this.models.size());
        assertFalse(this.models.containsKey(ModelLocationUtils.getModelLocation(Blocks.IRON_BLOCK)));
        var slab = this.models.get(ModelLocationUtils.getModelLocation(Blocks.STONE_SLAB)).get().getAsJsonObject();
        assertEquals("minecraft:block/iron_block", slab.getAsJsonObject("textures").get("bottom").getAsString());
    }

    @Test
    void explicitlyCreatingVanillaResourcesRemainsPossible() {
        generator().create(Blocks.IRON_BLOCK, TexturedModel.CUBE);

        assertEquals(Set.of(Blocks.IRON_BLOCK), this.states);
        assertTrue(this.models.containsKey(ModelLocationUtils.getModelLocation(Blocks.IRON_BLOCK)));
    }

    @Test
    void vanillaSpecialTextureMappingIsPreserved() {
        generator().family(Blocks.SANDSTONE).stairs(Blocks.STONE_STAIRS);

        var stairs = this.models.get(ModelLocationUtils.getModelLocation(Blocks.STONE_STAIRS)).get().getAsJsonObject();
        var textures = stairs.getAsJsonObject("textures");
        assertEquals("minecraft:block/sandstone_top", textures.get("top").getAsString());
        assertEquals("minecraft:block/sandstone_bottom", textures.get("bottom").getAsString());
        assertFalse(this.models.containsKey(ModelLocationUtils.getModelLocation(Blocks.SANDSTONE)));
    }

    private RegistrumBlockModelGenerator generator() {
        var itemOutput = (ItemModelOutput) Proxy.newProxyInstance(ItemModelOutput.class.getClassLoader(),
            new Class<?>[] {ItemModelOutput.class}, (proxy, method, arguments) -> {
                this.items.add((Item) arguments[method.getName().equals("copy") ? 1 : 0]);
                return null;
            });
        return new RegistrumBlockModelGenerator(null, state -> this.states.add(state.block()), itemOutput, this.models::put);
    }
}
