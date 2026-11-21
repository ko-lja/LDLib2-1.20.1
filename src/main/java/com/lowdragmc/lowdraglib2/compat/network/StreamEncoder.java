package com.lowdragmc.lowdraglib2.compat.network;

@FunctionalInterface
public interface StreamEncoder<O, T> {
    void encode(O buf, T value);
}
