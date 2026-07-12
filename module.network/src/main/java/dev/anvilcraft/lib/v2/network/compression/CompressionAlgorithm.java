package dev.anvilcraft.lib.v2.network.compression;

/**
 * Compression algorithms supported by the AnvilLib byte payload wire format.
 */
public enum CompressionAlgorithm {
    NONE(0),
    LZ4(1),
    GZIP(2);

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
