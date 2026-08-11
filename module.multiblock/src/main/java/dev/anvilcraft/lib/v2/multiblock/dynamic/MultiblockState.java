package dev.anvilcraft.lib.v2.multiblock.dynamic;

import com.mojang.serialization.Codec;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import javax.annotation.Nullable;

import java.util.Map;

@Getter
@Setter
public class MultiblockState {
    public static final Codec<ResourceKey<MultiblockDefinition>> DEFINITION_KEY_CODEC = ResourceKey.codec(LibRegistries.DEFINITIONS_KEY);
    public static final StreamCodec<ByteBuf, ResourceKey<MultiblockDefinition>> DEFINITION_KEY_STREAM_CODEC = ResourceKey.streamCodec(
        LibRegistries.DEFINITIONS_KEY
    );
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
    private @Nullable Holder.Reference<MultiblockDefinition> definition;
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

    public Tag toTag(HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        data.put(
            "controllerPos",
            BlockPos.CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE),
                this.controllerPos
            ).getOrThrow()
        );
        data.put(
            "definition",
            MultiblockState.DEFINITION_KEY_CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE),
                this.getDefinitionKey()
            ).getOrThrow()
        );
        data.putBoolean("formed", this.formed);
        return data;
    }

    public static MultiblockState fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        BlockPos controllerPos = BlockPos.CODEC.decode(
            registries.createSerializationContext(NbtOps.INSTANCE),
            tag.get("controllerPos")
        ).getOrThrow().getFirst();
        ResourceKey<MultiblockDefinition> definitionKey = MultiblockState.DEFINITION_KEY_CODEC.decode(
            registries.createSerializationContext(NbtOps.INSTANCE),
            tag.get("definition")
        ).getOrThrow().getFirst();
        boolean formed = tag.getBoolean("formed");
        return new MultiblockState(controllerPos, definitionKey, formed);
    }
}
