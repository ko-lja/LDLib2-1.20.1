package com.lowdragmc.lowdraglib2.networking.c2s;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.compat.network.CustomPacketPayload;
import com.lowdragmc.lowdraglib2.compat.network.IPayloadContext;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.BitSet;

public record CPacketSlotVisibility(int containerId, BitSet hiddenSlots) implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("slot_visibility");
    public static final Type<CPacketSlotVisibility> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, CPacketSlotVisibility> CODEC = StreamCodec.ofMember(CPacketSlotVisibility::write, CPacketSlotVisibility::decode);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeBitSet(hiddenSlots);
    }

    public static CPacketSlotVisibility decode(FriendlyByteBuf buf) {
        var containerId = buf.readVarInt();
        var hiddenSlots = buf.readBitSet();
        return new CPacketSlotVisibility(containerId, hiddenSlots);
    }

    public static void execute(CPacketSlotVisibility packet, IPayloadContext context) {
        if (context.player().containerMenu instanceof ModularUIContainerMenu menu && menu.containerId == packet.containerId) {
            menu.setClientHiddenSlots(packet.hiddenSlots);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
