package com.lowdragmc.lowdraglib2.networking.both;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.compat.network.CustomPacketPayload;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PacketSplitPart(boolean first, boolean last, byte[] data) implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("split_part");
    public static final Type<PacketSplitPart> TYPE = new Type<>(ID);
    public static final int MAX_PART_SIZE = 32000;
    public static final StreamCodec<FriendlyByteBuf, PacketSplitPart> CODEC = StreamCodec.ofMember(PacketSplitPart::write, PacketSplitPart::decode);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(first);
        buf.writeBoolean(last);
        buf.writeByteArray(data);
    }

    public static PacketSplitPart decode(FriendlyByteBuf buf) {
        var first = buf.readBoolean();
        var last = buf.readBoolean();
        var data = buf.readByteArray(MAX_PART_SIZE);
        return new PacketSplitPart(first, last, data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
