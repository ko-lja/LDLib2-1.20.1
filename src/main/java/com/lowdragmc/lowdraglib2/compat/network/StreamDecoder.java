package com.lowdragmc.lowdraglib2.compat.network;

@FunctionalInterface
public interface StreamDecoder<I, T> {
    T decode(I buf);
}
