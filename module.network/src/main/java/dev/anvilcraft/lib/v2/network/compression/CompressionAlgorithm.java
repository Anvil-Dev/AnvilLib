package dev.anvilcraft.lib.v2.network.compression;

/**
 * Compression algorithms supported by the AnvilLib byte payload wire format.
 */
public enum CompressionAlgorithm {
    NONE(0),
    ZSTD(1),
    LZ4(2),
    GZIP(3);

    private final int id;

    CompressionAlgorithm(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    static CompressionAlgorithm byId(int id) {
        for (CompressionAlgorithm algorithm : values()) {
            if (algorithm.id == id) return algorithm;
        }
        throw new IllegalArgumentException("Unknown payload compression algorithm id: " + id);
    }
}
