package com.lowdragmc.lowdraglib2.compat.network;

import com.lowdragmc.lowdraglib2.networking.LDLNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

public final class PacketDistributor {
    private PacketDistributor() {
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var target = net.minecraftforge.network.PacketDistributor.SERVER.noArg();
        LDLNetworking.CHANNEL.send(target, payload);
        for (var other : payloads) {
            LDLNetworking.CHANNEL.send(target, other);
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var target = net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player);
        LDLNetworking.CHANNEL.send(target, payload);
        for (var other : payloads) {
            LDLNetworking.CHANNEL.send(target, other);
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var target = net.minecraftforge.network.PacketDistributor.ALL.noArg();
        LDLNetworking.CHANNEL.send(target, payload);
        for (var other : payloads) {
            LDLNetworking.CHANNEL.send(target, other);
        }
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var target = net.minecraftforge.network.PacketDistributor.DIMENSION.with(level::dimension);
        LDLNetworking.CHANNEL.send(target, payload);
        for (var other : payloads) {
            LDLNetworking.CHANNEL.send(target, other);
        }
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var target = net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> entity);
        LDLNetworking.CHANNEL.send(target, payload);
        for (var other : payloads) {
            LDLNetworking.CHANNEL.send(target, other);
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        var target = net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity);
        LDLNetworking.CHANNEL.send(target, payload);
        for (var other : payloads) {
            LDLNetworking.CHANNEL.send(target, other);
        }
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        for (var player : level.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            sendToPlayer(player, payload, payloads);
        }
    }
}
