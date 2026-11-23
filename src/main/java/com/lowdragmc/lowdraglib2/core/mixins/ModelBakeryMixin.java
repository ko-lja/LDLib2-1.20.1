package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IItemRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;

/**
 * @author KilaBash
 * @date 2022/05/28
 */
@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {

    @WrapOperation(method = "getModel",
              at = @At(value = "INVOKE",
                       target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    protected void injectStateToModelLocation(Logger instance, String s, Object[] objects, Operation<Void> original) {
        ResourceLocation id = objects[0] instanceof ResourceLocation rl ? new ResourceLocation(rl.getNamespace(), rl.getPath()) : null;
        if (id != null) {
            if (id.getPath().startsWith("block/")) {
                id = id.withPath(id.getPath().substring("block/".length()));
            } else if (id.getPath().startsWith("item/")) {
                id = id.withPath(id.getPath().substring("item/".length()));
            }
            if (BuiltInRegistries.ITEM.get(id) instanceof IItemRendererProvider) {
                return;
            }
        }
        original.call(instance, s, objects);
    }

    @Shadow protected abstract BlockModel loadBlockModel(ResourceLocation location) throws IOException;

    @Unique
    private UnbakedModel ldlib2$rendererModel;

    @ModifyVariable(method = "cacheAndQueueDependencies", at = @At("HEAD"), argsOnly = true)
    protected UnbakedModel ldlib2$changeLoadedModel(UnbakedModel model, ResourceLocation location) {
        if (location instanceof ModelResourceLocation modelResourceLocation && !"inventory".equals(modelResourceLocation.getVariant())) {
            var block = BuiltInRegistries.BLOCK.get(new ResourceLocation(modelResourceLocation.getNamespace(), modelResourceLocation.getPath()));
            if (block instanceof IBlockRendererProvider) {
                if (ldlib2$rendererModel == null) {
                    try {
                        ldlib2$rendererModel = loadBlockModel(LDLib2.id("block/renderer_model"));
                    } catch (IOException e) {
                        return model;
                    }
                }
                return ldlib2$rendererModel;
            }
        }
        return model;
    }
}
