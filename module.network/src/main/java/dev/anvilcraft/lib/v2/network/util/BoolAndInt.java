package dev.anvilcraft.lib.v2.network.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/// A record that packs a {@code boolean} and an {@code int} into a compact
/// VarInt-compatible format for network serialization.
///
/// <p>The integer is first zigzag-encoded to reduce the size of small negative
/// numbers, then the boolean is folded into the lowest bit. The combined value
/// is written as a variable-length integer.</p>
///
/// @param bool    a boolean value
/// @param integer an integer value
public record BoolAndInt(boolean bool, int integer) {
    public static final StreamCodec<ByteBuf, BoolAndInt> STREAM_CODEC = StreamCodec.of(
        BoolAndInt::encode,
        BoolAndInt::decode
    );

    private static void encode(ByteBuf buf, BoolAndInt data) {
        // zigzag encoding
        long value = ((long) data.integer << 1) ^ (data.integer >> 31);

        // store bool
        value = (value << 1) | (data.bool ? 1 : 0);

        // VarInt encoding
        while((value & -128) != 0) {
            buf.writeByte((int) (value & 127 | 128));
            value >>>= 7;
        }

        buf.writeByte((int) (value & 0x7F));
    }

    private static BoolAndInt decode(ByteBuf buf) {
        long out = 0;
        int bytes = 0;

        // VarInt decoding
        byte in;
        do {
            in = buf.readByte();
            out |= (in & 127L) << bytes++ * 7;
            if (bytes > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((in & 128) == 128);

        // extract bool
        boolean bool = (out & 1) != 0;
        out >>>= 1;

        out = (out >>> 1) ^ -(out & 1);

        return new BoolAndInt(bool, (int) out);
    }
}
