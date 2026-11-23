package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib2.utils.CustomResourcePack;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @ModifyVariable(method = "createReload", at = @At("HEAD"), index = 4, argsOnly = true)
    private List<PackResources> injectCreateReload(List<PackResources> resourcePacks) {
        if (LDLib2.isClient()) {
            ldlib2$injectClientResourcePack();
        }

        var mutableList = new ArrayList<>(resourcePacks);
        mutableList.add(new CustomResourcePack(new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID), LDLib2.MOD_ID, PackType.CLIENT_RESOURCES));

        return mutableList;
    }

    @Unique
    private static void ldlib2$injectClientResourcePack() {
        LDLMetadataSection.clearCache();
    }
}
