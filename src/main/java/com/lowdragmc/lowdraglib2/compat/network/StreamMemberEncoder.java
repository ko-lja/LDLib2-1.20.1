package com.lowdragmc.lowdraglib2.compat.network;

@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T value, O buf);
}
