package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.utils.CustomResourcePack;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mixin(WorldLoader.PackConfig.class)
public abstract class PackConfigMixin {

    @ModifyExpressionValue(method = "createResourceManager",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/repository/PackRepository;openAllSelected()Ljava/util/List;"
    ))
    private List<PackResources> injectCreateReload(List<PackResources> resourcePacks) {
        var mutableList = new ArrayList<>(resourcePacks);
        mutableList.add(new CustomResourcePack(new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID), LDLib2.MOD_ID, PackType.SERVER_DATA));
        return mutableList;
    }

}
