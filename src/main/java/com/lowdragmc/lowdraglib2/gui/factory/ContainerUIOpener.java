package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.event.ContainerMenuEvent;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ContainerUIOpener {
    private ContainerUIOpener() {
    }

    public static boolean open(ServerPlayer player, MenuProvider provider) {
        return open(player, provider, (menu, buffer) -> {
            if (provider instanceof IContainerUIHolder holder) {
                holder.writeClientSideData(menu, buffer);
            }
        });
    }

    public static boolean open(ServerPlayer player, MenuProvider provider, BiConsumer<AbstractContainerMenu, FriendlyByteBuf> clientDataWriter) {
        var state = new PreparedMenu();
        var wrapper = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player owner) {
                if (state.created) {
                    return state.menu;
                }
                return provider.createMenu(containerId, playerInventory, owner);
            }
        };
        NetworkHooks.openScreen(player, wrapper, buffer -> {
            state.menu = provider.createMenu(player.containerCounter, player.getInventory(), player);
            state.created = true;
            if (state.menu == null) return;
            MinecraftForge.EVENT_BUS.post(new ContainerMenuEvent.Create(player, state.menu));
            clientDataWriter.accept(state.menu, buffer);
            if (state.menu instanceof IModularUIHolder holder) {
                holder.writeInitialData(buffer);
            }
        });
        return state.menu != null && player.containerMenu == state.menu;
    }

    private static final class PreparedMenu {
        @Nullable
        private AbstractContainerMenu menu;
        private boolean created;
    }
}
