package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBakery.class)
public abstract class BlockStateModelLoaderMixin {

    @WrapOperation(method = "/^(lambda\\$.*\\$\\d+|m_\\d+_)$/",
                   at = @At(value = "INVOKE",
                            target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                            remap = false),
                   remap = false)
    private void ldlib2$injectStateToModelLocation(Logger instance, String s, Object arg1, Object arg2, Operation<Void> original) {
        if (arg2 instanceof ModelResourceLocation modelResourceLocation
                && BuiltInRegistries.BLOCK.get(new ResourceLocation(modelResourceLocation.getNamespace(), modelResourceLocation.getPath())) instanceof IBlockRendererProvider) {
            return;
        }
        original.call(instance, s, arg1, arg2);
    }
}
