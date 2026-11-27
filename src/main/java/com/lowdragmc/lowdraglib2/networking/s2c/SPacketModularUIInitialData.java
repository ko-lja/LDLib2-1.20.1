package com.lowdragmc.lowdraglib2.networking.s2c;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.compat.network.CustomPacketPayload;
import com.lowdragmc.lowdraglib2.compat.network.IPayloadContext;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.lowdragmc.lowdraglib2.gui.sync.IUISyncManagerHolder;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SPacketModularUIInitialData(int containerId, byte[] data) implements CustomPacketPayload {
    public static final ResourceLocation ID = LDLib2.id("modular_ui_initial_data");
    public static final Type<SPacketModularUIInitialData> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, SPacketModularUIInitialData> CODEC = StreamCodec.ofMember(SPacketModularUIInitialData::write, SPacketModularUIInitialData::decode);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeByteArray(data);
    }

    public static SPacketModularUIInitialData decode(FriendlyByteBuf buf) {
        var containerId = buf.readVarInt();
        var data = buf.readByteArray();
        return new SPacketModularUIInitialData(containerId, data);
    }

    public static void execute(SPacketModularUIInitialData packet, IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (menu.containerId != packet.containerId || !(menu instanceof IUISyncManagerHolder holder)) {
            return;
        }
        var syncManager = holder.getSyncManager();
        if (syncManager == null) return;
        ByteBufUtil.readCustomData(packet.data, syncManager::readInitialData, player.level().registryAccess());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
