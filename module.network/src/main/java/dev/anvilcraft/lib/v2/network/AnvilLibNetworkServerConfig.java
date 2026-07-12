package dev.anvilcraft.lib.v2.network;

import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import dev.anvilcraft.lib.v2.network.compression.CompressionAlgorithm;
import net.neoforged.fml.config.ModConfig;

@Config(name = "anvillib_network", group = "anvillib", type = ModConfig.Type.SERVER)
public class AnvilLibNetworkServerConfig {
    @Comment(
        """
            The default compression algorithm to use for network packets.
            Options: NONE, ZSTD, LZ4, GZIP
            Default: ZSTD
            - NONE No compression
            - ZSTD Excellent balance between compression ratio, compression speed, and decompression speed
            - LZ4 Extremely fast compression and decompression speed
            - GZIP Maximum compatibility
            """
    )
    public volatile CompressionAlgorithm defaultCompressionAlgorithm = CompressionAlgorithm.ZSTD;
    @Comment(
        """
            The threshold for compression in bytes.
            Default: 128
            """
    )
    public volatile int threshold = 128;
    @Comment(
        """
            The maximum decompressed size in bytes.
            Default: 16777216
            """
    )
    public volatile int maxDecompressedSize = 16777216;
}
