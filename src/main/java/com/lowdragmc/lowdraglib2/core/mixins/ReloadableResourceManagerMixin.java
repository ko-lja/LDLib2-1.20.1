package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.model.custommodel.CustomBakedModel;
import com.lowdragmc.lowdraglib2.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib2.gui.texture.VanillaSpriteTexture;
import com.lowdragmc.lowdraglib2.utils.CustomResourcePack;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @ModifyVariable(method = "createReload", at = @At("HEAD"), index = 4, argsOnly = true)
    private List<PackResources> injectCreateReload(List<PackResources> resourcePacks) {
        var mutableList = new ArrayList<>(resourcePacks);
        mutableList.add(new CustomResourcePack(new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID), LDLib2.MOD_ID, PackType.CLIENT_RESOURCES));

        return mutableList;
    }

    @Inject(method = "createReload", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;resources:Lnet/minecraft/server/packs/resources/CloseableResourceManager;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void ldlib2$afterPackSwap(Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> waitingFor,
                                      List<PackResources> resourcePacks, CallbackInfoReturnable<ReloadInstance> cir) {
        if (LDLib2.isClient()) {
            ldlib2$clearClientResourceCaches();
        }
    }

    @Unique
    private static void ldlib2$clearClientResourceCaches() {
        LDLMetadataSection.clearCache();
        CustomBakedModel.clearCache();
        VanillaSpriteTexture.clearCache();
    }
}
