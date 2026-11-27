package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.compat.network.CustomPacketPayload;
import com.lowdragmc.lowdraglib2.compat.network.IPayloadContext;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.both.PacketSplitPart;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEventReturn;
import com.lowdragmc.lowdraglib2.networking.c2s.CPacketSlotVisibility;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntities;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketModularUIInitialData;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class LDLNetworking {
    public static final ResourceLocation CHANNEL_ID = LDLib2.id("main");
    private static final int SERVERBOUND_LIMIT = 32767;
    private static final int CLIENTBOUND_LIMIT = 1048576;
    private static final int MAX_SPLIT_SIZE = 8 * 1024 * 1024;

    private static SimpleChannel CHANNEL;

    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();

    private static final List<Registration<?>> PENDING = new ArrayList<>();

    private static final List<Registration<?>> BY_INDEX = new ArrayList<>();

    private static final Object2IntMap<ResourceLocation> INDICES = new Object2IntOpenHashMap<>();

    private static final Map<Connection, ByteArrayOutputStream> SPLIT_ASSEMBLIES = Collections.synchronizedMap(new WeakHashMap<>());

    static {
        INDICES.defaultReturnValue(-1);
    }

    private record Registration<T extends CustomPacketPayload>(Class<T> messageType, CustomPacketPayload.Type<T> type,
                                                               StreamCodec<FriendlyByteBuf, T> codec,
                                                               BiConsumer<T, IPayloadContext> handler,
                                                               @Nullable NetworkDirection direction) {
        ResourceLocation id() {
            return type.id();
        }

        void bind(SimpleChannel channel, int index) {
            channel.registerMessage(index, messageType,
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
                    Optional.ofNullable(direction));
        }

        void encode(FriendlyByteBuf buf, CustomPacketPayload payload) {
            codec.encode(buf, messageType.cast(payload));
        }

        void handle(FriendlyByteBuf buf, IPayloadContext context) {
            handler.accept(codec.decode(buf), context);
        }

        boolean accepts(PacketFlow flow) {
            return direction == null || (direction == NetworkDirection.PLAY_TO_SERVER) == (flow == PacketFlow.SERVERBOUND);
        }
    }

    public static SimpleChannel getChannel() {
        return CHANNEL;
    }

    public static synchronized void init() {
        if (CHANNEL != null) return;
        var registrations = new ArrayList<>(PENDING);

        add(registrations, SPacketAutoSyncBlockEntity.class, SPacketAutoSyncBlockEntity.TYPE, SPacketAutoSyncBlockEntity.CODEC, SPacketAutoSyncBlockEntity::execute, NetworkDirection.PLAY_TO_CLIENT);
        add(registrations, SPacketAutoSyncBlockEntities.class, SPacketAutoSyncBlockEntities.TYPE, SPacketAutoSyncBlockEntities.CODEC, SPacketAutoSyncBlockEntities::execute, NetworkDirection.PLAY_TO_CLIENT);
        add(registrations, SPacketModularUIInitialData.class, SPacketModularUIInitialData.TYPE, SPacketModularUIInitialData.CODEC, SPacketModularUIInitialData::execute, NetworkDirection.PLAY_TO_CLIENT);

        add(registrations, CPacketSlotVisibility.class, CPacketSlotVisibility.TYPE, CPacketSlotVisibility.CODEC, CPacketSlotVisibility::execute, NetworkDirection.PLAY_TO_SERVER);

        add(registrations, PacketUIRPCEvent.class, PacketUIRPCEvent.TYPE, PacketUIRPCEvent.CODEC, PacketUIRPCEvent::execute, null);
        add(registrations, PacketUIRPCEventReturn.class, PacketUIRPCEventReturn.TYPE, PacketUIRPCEventReturn.CODEC, PacketUIRPCEventReturn::execute, null);

        add(registrations, PacketRPCBlockEntity.class, PacketRPCBlockEntity.TYPE, PacketRPCBlockEntity.CODEC, PacketRPCBlockEntity::execute, null);
        add(registrations, PacketModularUISync.class, PacketModularUISync.TYPE, PacketModularUISync.CODEC, PacketModularUISync::execute, null);

        add(registrations, PacketRPCPacket.class, PacketRPCPacket.TYPE, PacketRPCPacket.CODEC, PacketRPCPacket::execute, null);

        add(registrations, PacketSplitPart.class, PacketSplitPart.TYPE, PacketSplitPart.CODEC, LDLNetworking::handleSplitPart, null);

        registrations.sort(Comparator.comparing(Registration::id));
        if (registrations.size() > 256) {
            throw new IllegalStateException("The ldlib2:main channel holds at most 256 payloads, " + registrations.size() + " were registered");
        }
        var ids = new HashSet<ResourceLocation>();
        var fingerprint = new StringBuilder();
        for (var registration : registrations) {
            if (!ids.add(registration.id())) {
                throw new IllegalStateException("Payload " + registration.id() + " is registered twice");
            }
            fingerprint.append(registration.id()).append(registration.direction() == null ? "" : registration.direction().name()).append(';');
        }
        var version = "2-" + Integer.toHexString(fingerprint.toString().hashCode());
        var channel = NetworkRegistry.newSimpleChannel(CHANNEL_ID, () -> version,
                NetworkRegistry.acceptMissingOr(version), NetworkRegistry.acceptMissingOr(version));
        for (int index = 0; index < registrations.size(); index++) {
            var registration = registrations.get(index);
            registration.bind(channel, index);
            INDICES.put(registration.id(), index);
        }
        BY_INDEX.addAll(registrations);
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

    public static void send(PacketDistributor.PacketTarget target, CustomPacketPayload payload) {
        var id = payload.type().id();
        var index = INDICES.getInt(id);
        if (CHANNEL == null || index < 0) {
            throw new IllegalStateException("Payload " + id + " is not registered on the ldlib2:main channel");
        }
        var direction = target.getDirection();
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(index);
        BY_INDEX.get(index).encode(buf, payload);
        var size = buf.readableBytes();
        if (size <= (direction == NetworkDirection.PLAY_TO_SERVER ? SERVERBOUND_LIMIT : CLIENTBOUND_LIMIT)) {
            target.send(direction == NetworkDirection.PLAY_TO_SERVER
                    ? new ServerboundCustomPayloadPacket(CHANNEL_ID, buf)
                    : new ClientboundCustomPayloadPacket(CHANNEL_ID, buf));
            return;
        }
        var bytes = new byte[size];
        buf.readBytes(bytes);
        buf.release();
        if (size > MAX_SPLIT_SIZE) {
            throw new IllegalArgumentException("Payload " + id + " is " + size + " bytes, more than the " + MAX_SPLIT_SIZE + " bytes a split payload may carry");
        }
        for (int offset = 0; offset < size; offset += PacketSplitPart.MAX_PART_SIZE) {
            var end = Math.min(size, offset + PacketSplitPart.MAX_PART_SIZE);
            CHANNEL.send(target, new PacketSplitPart(offset == 0, end == size, Arrays.copyOfRange(bytes, offset, end)));
        }
    }

    private static void handleSplitPart(PacketSplitPart part, IPayloadContext context) {
        var connection = context.connection();
        ByteArrayOutputStream assembly;
        synchronized (SPLIT_ASSEMBLIES) {
            if (part.first()) {
                assembly = new ByteArrayOutputStream();
                SPLIT_ASSEMBLIES.put(connection, assembly);
            } else {
                assembly = SPLIT_ASSEMBLIES.get(connection);
            }
            if (assembly == null) {
                LDLib2.LOGGER.warn("Dropped part of a split payload that arrived without its first part");
                return;
            }
            if (assembly.size() + part.data().length > MAX_SPLIT_SIZE) {
                SPLIT_ASSEMBLIES.remove(connection);
                LDLib2.LOGGER.warn("Dropped a split payload larger than {} bytes", MAX_SPLIT_SIZE);
                return;
            }
            assembly.writeBytes(part.data());
            if (!part.last()) {
                return;
            }
            SPLIT_ASSEMBLIES.remove(connection);
        }
        var buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(assembly.toByteArray()));
        var index = buf.readUnsignedByte();
        if (index >= BY_INDEX.size()) {
            LDLib2.LOGGER.warn("Dropped a split payload with unknown discriminator {}", index);
            return;
        }
        var registration = BY_INDEX.get(index);
        if (registration.id().equals(PacketSplitPart.ID) || !registration.accepts(context.flow())) {
            LDLib2.LOGGER.warn("Dropped a split payload {} sent in the wrong direction", registration.id());
            return;
        }
        registration.handle(buf, context);
    }

    private static <T extends CustomPacketPayload> void add(List<Registration<?>> registrations, Class<T> messageType,
                                                            CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                                            BiConsumer<T, IPayloadContext> handler, @Nullable NetworkDirection direction) {
        registrations.add(new Registration<>(messageType, type, codec, handler, direction));
    }

}
