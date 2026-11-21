package com.lowdragmc.lowdraglib2.compat.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

public final class VarInt {
    private VarInt() {
    }

    public static int getByteSize(int value) {
        return FriendlyByteBuf.getVarIntSize(value);
    }

    public static int read(ByteBuf buf) {
        return new FriendlyByteBuf(buf).readVarInt();
    }

    public static ByteBuf write(ByteBuf buf, int value) {
        new FriendlyByteBuf(buf).writeVarInt(value);
        return buf;
    }
}
