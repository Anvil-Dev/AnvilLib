package dev.anvilcraft.lib.v2.rpc;

public @interface CallableParam {
    Class<?> clazz();

    String field();
}
