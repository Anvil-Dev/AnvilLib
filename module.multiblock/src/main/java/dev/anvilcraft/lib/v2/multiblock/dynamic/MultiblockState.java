package dev.anvilcraft.lib.v2.multiblock.dynamic;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.util.Util;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

@Getter
@Setter
public class MultiblockState {
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockState> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.VAR_INT_BLOCK_POS,
        MultiblockState::getControllerPos,
        ByteBufCodecs.holder(LibRegistries.DEFINITIONS_KEY, MultiblockDefinition.STREAM_CODEC),
        MultiblockState::getDefinition,
        MultiblockState::new
    );
    public static final Codec<ResourceKey<MultiblockDefinition>> DEFINITION_KEY_CODEC = ResourceKey.codec(LibRegistries.DEFINITIONS_KEY);

    private final BlockPos controllerPos;
    private final Holder.Reference<MultiblockDefinition> definition;
    private boolean formed;

    public MultiblockState(BlockPos controllerPos, Holder<MultiblockDefinition> definition) {
        this(
            controllerPos,
            definition instanceof Holder.Reference<MultiblockDefinition> ref
            ? ref
            : Util.throwE(new IllegalArgumentException("Non Reference Holder '" + definition + "' found")),
            false
        );
    }

    public MultiblockState(BlockPos controllerPos, Holder.Reference<MultiblockDefinition> definition, boolean formed) {
        this.controllerPos = controllerPos;
        this.definition = definition;
        this.formed = formed;
    }

    public ResourceKey<MultiblockDefinition> getDefinitionKey() {
        return this.definition.key();
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
        Holder.Reference<MultiblockDefinition> definition = registries.lookup(LibRegistries.DEFINITIONS_KEY).orElseThrow().getOrThrow(
            MultiblockState.DEFINITION_KEY_CODEC.decode(
                registries.createSerializationContext(NbtOps.INSTANCE),
                tag.get("definition")
            ).getOrThrow().getFirst()
        );
        boolean formed = tag.getBoolean("formed");
        return new MultiblockState(controllerPos, definition, formed);
    }
}



