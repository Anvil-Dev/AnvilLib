package dev.anvilcraft.lib.v2.multiblock.dynamic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Getter
@Setter
public class MultiblockState {
    public static final Codec<ResourceKey<MultiblockDefinition>> DEFINITION_KEY_CODEC = ResourceKey.codec(LibRegistries.DEFINITIONS_KEY);
    public static final StreamCodec<ByteBuf, ResourceKey<MultiblockDefinition>> DEFINITION_KEY_STREAM_CODEC = ResourceKey.streamCodec(
        LibRegistries.DEFINITIONS_KEY
    );
    public static final MapCodec<MultiblockState> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        BlockPos.CODEC
            .fieldOf("controller_pos")
            .forGetter(MultiblockState::getControllerPos),
        DEFINITION_KEY_CODEC
            .fieldOf("definition")
            .forGetter(MultiblockState::getDefinitionKey)
    ).apply(inst, MultiblockState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockState> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.VAR_INT_BLOCK_POS,
        MultiblockState::getControllerPos,
        DEFINITION_KEY_STREAM_CODEC,
        MultiblockState::getDefinitionKey,
        MultiblockState::new
    );

    private final BlockPos controllerPos;
    private final ResourceKey<MultiblockDefinition> definitionKey;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Holder.@Nullable Reference<MultiblockDefinition> definition;
    private boolean formed;
    private MultiblockCheckSnapshot snapshot;

    public MultiblockState(BlockPos controllerPos, ResourceKey<MultiblockDefinition> definitionKey) {
        this(controllerPos, definitionKey, false);
    }

    public MultiblockState(BlockPos controllerPos, ResourceKey<MultiblockDefinition> definitionKey, boolean formed) {
        this.controllerPos = controllerPos;
        this.definitionKey = definitionKey;
        this.formed = formed;
        this.snapshot = new MultiblockCheckSnapshot(this.controllerPos, Map.of());
    }

    public Holder.Reference<MultiblockDefinition> getDefinition(HolderLookup.Provider registries) {
        if (this.definition != null) return this.definition;
        return this.definition = registries.lookup(LibRegistries.DEFINITIONS_KEY).orElseThrow().getOrThrow(this.definitionKey);
    }
}



