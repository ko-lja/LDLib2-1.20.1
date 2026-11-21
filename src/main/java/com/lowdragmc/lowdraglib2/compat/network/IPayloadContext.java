package com.lowdragmc.lowdraglib2.compat.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IPayloadContext {
    Player player();

    PacketFlow flow();

    Connection connection();

    CompletableFuture<Void> enqueueWork(Runnable task);

    <T> CompletableFuture<T> enqueueWork(Supplier<T> task);
}
