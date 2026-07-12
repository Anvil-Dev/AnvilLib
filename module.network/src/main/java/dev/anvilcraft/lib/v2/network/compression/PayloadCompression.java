package dev.anvilcraft.lib.v2.network.compression;

import dev.anvilcraft.lib.v2.network.AnvilLibNetwork;
import io.netty.buffer.ByteBuf;
import net.jpountz.lz4.LZ4Factory;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Shared compression envelope for opaque network payloads.
 *
 * <p>The wire format is an algorithm byte followed by the raw bytes for
 * {@link CompressionAlgorithm#NONE}, or a VarInt uncompressed length and the
 * compressed bytes for all other algorithms.</p>
 */
public final class PayloadCompression {
    public static final StreamCodec<ByteBuf, byte[]> STREAM_CODEC = ByteBufCodecs.BYTE_ARRAY.map(
        PayloadCompression::decode,
        PayloadCompression::encode
    );

    private PayloadCompression() {
    }

    public static CompressionAlgorithm algorithm() {
        return AnvilLibNetwork.CONFIG.defaultCompressionAlgorithm;
    }

    public static int threshold() {
        return AnvilLibNetwork.CONFIG.threshold;
    }

    public static int maxDecompressedSize() {
        return AnvilLibNetwork.CONFIG.maxDecompressedSize;
    }

    public static byte[] encode(byte[] data) {
        Objects.requireNonNull(data, "data");
        int threshold = threshold();
        int maxSize = maxDecompressedSize();
        CompressionAlgorithm algo = algorithm();
        if (data.length > maxSize) {
            throw new IllegalArgumentException("Payload exceeds maximum uncompressed size: " + data.length);
        }

        CompressionAlgorithm selected = data.length >= threshold ? algo : CompressionAlgorithm.NONE;
        if (selected == CompressionAlgorithm.NONE) return uncompressed(data);

        byte[] compressed = compress(selected, data);
        int headerLength = 1 + varIntSize(data.length);
        if (compressed.length + headerLength >= data.length + 1) return uncompressed(data);

        ByteArrayOutputStream output = new ByteArrayOutputStream(headerLength + compressed.length);
        output.write(selected.id());
        writeVarInt(output, data.length);
        output.writeBytes(compressed);
        return output.toByteArray();
    }

    public static byte[] decode(byte[] envelope) {
        Objects.requireNonNull(envelope, "envelope");
        int maxSize = maxDecompressedSize();
        if (envelope.length == 0) throw new IllegalArgumentException("Compressed payload envelope is empty");

        CompressionAlgorithm selected = CompressionAlgorithm.byId(Byte.toUnsignedInt(envelope[0]));
        if (selected == CompressionAlgorithm.NONE) {
            int size = envelope.length - 1;
            if (size > maxSize) {
                throw new IllegalArgumentException("Payload exceeds maximum uncompressed size: " + size);
            }
            return Arrays.copyOfRange(envelope, 1, envelope.length);
        }

        VarInt length = readVarInt(envelope, 1);
        if (length.value() < 0 || length.value() > maxSize) {
            throw new IllegalArgumentException("Invalid decompressed payload size: " + length.value());
        }
        if (length.nextOffset() >= envelope.length) {
            throw new IllegalArgumentException("Compressed payload has no body");
        }

        byte[] compressed = Arrays.copyOfRange(envelope, length.nextOffset(), envelope.length);
        byte[] data = decompress(selected, compressed, length.value());
        if (data.length != length.value()) {
            throw new IllegalArgumentException("Decompressed payload size mismatch: expected " + length.value() + ", got " + data.length);
        }
        return data;
    }

    private static byte[] uncompressed(byte[] data) {
        byte[] envelope = new byte[data.length + 1];
        envelope[0] = (byte) CompressionAlgorithm.NONE.id();
        System.arraycopy(data, 0, envelope, 1, data.length);
        return envelope;
    }

    private static byte[] compress(CompressionAlgorithm selected, byte[] data) {
        return switch (selected) {
            case LZ4 -> LZ4Factory.fastestInstance().fastCompressor().compress(data);
            case GZIP -> gzipCompress(data);
            case NONE -> throw new IllegalArgumentException("NONE does not compress data");
        };
    }

    private static byte[] decompress(CompressionAlgorithm selected, byte[] data, int originalLength) {
        return switch (selected) {
            case LZ4 -> LZ4Factory.fastestInstance().safeDecompressor().decompress(data, originalLength);
            case GZIP -> gzipDecompress(data, originalLength);
            case NONE -> throw new IllegalArgumentException("NONE does not decompress data");
        };
    }

    private static byte[] gzipCompress(byte[] data) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(data);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to GZIP-compress payload", e);
        }
    }

    private static byte[] gzipDecompress(byte[] data, int originalLength) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            ByteArrayOutputStream output = new ByteArrayOutputStream(originalLength);
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) >= 0) {
                total += read;
                if (total > originalLength) {
                    throw new IllegalArgumentException("GZIP payload exceeds declared decompressed size");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to GZIP-decompress payload", e);
        }
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        while ((value & -128) != 0) {
            output.write(value & 127 | 128);
            value >>>= 7;
        }
        output.write(value);
    }

    private static VarInt readVarInt(byte[] data, int offset) {
        int value = 0;
        int position = 0;
        while (position < 32) {
            if (offset >= data.length) throw new IllegalArgumentException("Truncated payload length VarInt");
            byte current = data[offset++];
            value |= (current & 127) << position;
            if ((current & 128) == 0) return new VarInt(value, offset);
            position += 7;
        }
        throw new IllegalArgumentException("Payload length VarInt is too large");
    }

    private static int varIntSize(int value) {
        int size = 1;
        while ((value & -128) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    private record VarInt(int value, int nextOffset) {
    }
}
