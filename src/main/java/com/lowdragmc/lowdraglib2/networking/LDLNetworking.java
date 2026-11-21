package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.compat.network.CustomPacketPayload;
import com.lowdragmc.lowdraglib2.compat.network.IPayloadContext;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEventReturn;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntities;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class LDLNetworking {
    public static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel CHANNEL;

    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();

    private static final List<Registration> PENDING = new ArrayList<>();

    @FunctionalInterface
    private interface Binder {
        void bind(SimpleChannel channel, int index);
    }

    private record Registration(ResourceLocation id, Binder binder) {}

    public static synchronized void init() {
        if (CHANNEL != null) return;
        var channel = NetworkRegistry.newSimpleChannel(LDLib2.id("main"), () -> PROTOCOL_VERSION,
                NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION), NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION));
        var registrations = new ArrayList<>(PENDING);

        add(registrations, SPacketAutoSyncBlockEntity.class, SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC, SPacketAutoSyncBlockEntity::execute, NetworkDirection.PLAY_TO_CLIENT);
        add(registrations, SPacketAutoSyncBlockEntities.class, SPacketAutoSyncBlockEntities.TYPE, SPacketAutoSyncBlockEntities.CODEC, SPacketAutoSyncBlockEntities::execute, NetworkDirection.PLAY_TO_CLIENT);

        add(registrations, PacketUIRPCEvent.class, PacketUIRPCEvent.TYPE, PacketUIRPCEvent.CODEC, PacketUIRPCEvent::execute, null);
        add(registrations, PacketUIRPCEventReturn.class, PacketUIRPCEventReturn.TYPE, PacketUIRPCEventReturn.CODEC, PacketUIRPCEventReturn::execute, null);

        add(registrations, PacketRPCBlockEntity.class, PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC, PacketRPCBlockEntity::execute, null);
        add(registrations, PacketModularUISync.class, PacketModularUISync.TYPE, PacketModularUISync.CODEC, PacketModularUISync::execute, null);

        add(registrations, PacketRPCPacket.class, PacketRPCPacket.TYPE, PacketRPCPacket.CODEC, PacketRPCPacket::execute, null);

        registrations.sort(Comparator.comparing(Registration::id));
        var ids = new HashSet<ResourceLocation>();
        for (int index = 0; index < registrations.size(); index++) {
            var registration = registrations.get(index);
            if (!ids.add(registration.id())) {
                throw new IllegalStateException("Payload " + registration.id() + " is registered twice");
            }
            registration.binder().bind(channel, index);
        }
        REGISTERED.addAll(ids);
        PENDING.clear();
        CHANNEL = channel;
    }

    public static synchronized <T extends CustomPacketPayload> void register(Class<T> messageType, CustomPacketPayload.Type<T> type,
                                                                              StreamCodec<FriendlyByteBuf, T> codec,
                                                                              BiConsumer<T, IPayloadContext> handler, @Nullable NetworkDirection direction) {
        if (CHANNEL != null) {
            throw new IllegalStateException("Cannot register payload " + type.id() + ": the ldlib2:main channel is already built, register payloads before LDLNetworking.init()");
        }
        add(PENDING, messageType, type, codec, handler, direction);
    }

    public static boolean hasChannel(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return CHANNEL != null && REGISTERED.contains(type.id()) && CHANNEL.isRemotePresent(player.connection.connection);
    }

    private static <T extends CustomPacketPayload> void add(List<Registration> registrations, Class<T> messageType,
                                                            CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                                            BiConsumer<T, IPayloadContext> handler, @Nullable NetworkDirection direction) {
        registrations.add(new Registration(type.id(), (channel, index) -> channel.registerMessage(index, messageType,
                (message, buf) -> codec.encode(buf, message),
                codec::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        try {
                            handler.accept(message, new PayloadContextImpl(context));
                        } catch (Throwable e) {
                            LDLib2.LOGGER.error("Failed to handle payload {}", type.id(), e);
                        }
                    });
                    context.setPacketHandled(true);
                },
                Optional.ofNullable(direction))));
    }

}
