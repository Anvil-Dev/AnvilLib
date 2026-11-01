package dev.anvilcraft.lib.v2.test.data;

import dev.anvilcraft.lib.v2.util.NumberProviderUtil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.floats.FromInt;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public final class NumberProviderCodecChecks {
    private NumberProviderCodecChecks() {
    }

    public static void verify() {

        assertRoundTrip(CodecUtil.INT_NUMBER_PROVIDER, StreamCodecUtil.INT_NUMBER_PROVIDER, ContextIntProviders.exactly(7).value());
        assertRoundTrip(CodecUtil.INT_NUMBER_PROVIDER, StreamCodecUtil.INT_NUMBER_PROVIDER, ContextIntProviders.between(2, 8).value());
        assertRoundTrip(CodecUtil.INT_NUMBER_PROVIDER, StreamCodecUtil.INT_NUMBER_PROVIDER, ContextIntProviders.binomial(4, 0.25f).value());
        // 算术与跨类型嵌套必须保留原生类型，不能回退成固定数量。
        var division = ContextIntProviders.div(ContextIntProviders.exactly(12), ContextIntProviders.exactly(3));
        assertRoundTrip(CodecUtil.INT_NUMBER_PROVIDER, StreamCodecUtil.INT_NUMBER_PROVIDER, division.value());
        assertRoundTrip(CodecUtil.FLOAT_NUMBER_PROVIDER, StreamCodecUtil.FLOAT_NUMBER_PROVIDER, ContextFloatProviders.exactly(0.375f).value());
        var uniform = ContextFloatProviders.between(0.25f, 0.75f);
        assertRoundTrip(CodecUtil.FLOAT_NUMBER_PROVIDER, StreamCodecUtil.FLOAT_NUMBER_PROVIDER, uniform.value());
        assertRoundTrip(CodecUtil.FLOAT_NUMBER_PROVIDER, StreamCodecUtil.FLOAT_NUMBER_PROVIDER, new FromInt(division));

        assertEquals(1.0, NumberProviderUtil.expected(ContextIntProviders.binomial(4, 0.25f).value()), "binomial mean");
        assertEquals(0.5, NumberProviderUtil.expected(uniform.value()), "float uniform mean");
        assertEquals(0.25, NumberProviderUtil.min(uniform.value()), "float uniform minimum");
        assertEquals(0.75, NumberProviderUtil.max(uniform.value()), "float uniform maximum");
        System.out.println("Number provider JSON/network round trips passed (7 cases).");
    }

    private static <T> void assertRoundTrip(Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, T original) {
        var json = codec.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        T decodedJson = codec.parse(JsonOps.INSTANCE, json).getOrThrow();
        if (!json.equals(codec.encodeStart(JsonOps.INSTANCE, decodedJson).getOrThrow())) {
            throw new AssertionError("JSON round trip changed provider: " + json);
        }
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            streamCodec.encode(buffer, original);
            T decodedNetwork = streamCodec.decode(buffer);
            if (buffer.isReadable() || !json.equals(codec.encodeStart(JsonOps.INSTANCE, decodedNetwork).getOrThrow())) {
                throw new AssertionError("Network round trip changed provider: " + json);
            }
        } finally {
            buffer.release();
        }
    }

    private static void assertEquals(double expected, double actual, String label) {
        if (Double.compare(expected, actual) != 0) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
