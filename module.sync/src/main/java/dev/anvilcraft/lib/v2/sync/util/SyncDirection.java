package dev.anvilcraft.lib.v2.sync.util;

import lombok.Getter;

@Getter
public enum SyncDirection {
    BOTH(true, true),
    C2S(true, false),
    S2C(false, true);
    private final boolean createByClient;
    private final boolean createByServer;

    SyncDirection(boolean createByClient, boolean createByServer) {
        this.createByClient = createByClient;
        this.createByServer = createByServer;
    }
}