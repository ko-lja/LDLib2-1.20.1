package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.compat.network.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class PayloadContextImpl implements IPayloadContext {
    private final NetworkEvent.Context context;

    PayloadContextImpl(NetworkEvent.Context context) {
        this.context = context;
    }

    @Override
    public Player player() {
        var sender = context.getSender();
        if (sender != null) {
            return sender;
        }
        if (context.getDirection().getReceptionSide().isClient()) {
            return ClientSide.player();
        }
        throw new IllegalStateException("No player is attached to this payload");
    }

    @Override
    public PacketFlow flow() {
        return context.getDirection().getReceptionSide().isServer() ? PacketFlow.SERVERBOUND : PacketFlow.CLIENTBOUND;
    }

    @Override
    public Connection connection() {
        return context.getNetworkManager();
    }

    @Override
    public CompletableFuture<Void> enqueueWork(Runnable task) {
        return context.enqueueWork(task);
    }

    @Override
    public <T> CompletableFuture<T> enqueueWork(Supplier<T> task) {
        var executor = LogicalSidedProvider.WORKQUEUE.get(context.getDirection().getReceptionSide());
        if (executor.isSameThread()) {
            return CompletableFuture.completedFuture(task.get());
        }
        return executor.submit(task);
    }

    private static final class ClientSide {
        private static Player player() {
            return Minecraft.getInstance().player;
        }
    }
}
