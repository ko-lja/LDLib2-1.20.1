package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {
    @Shadow private VertexFormat.Mode mode;

    @Inject(method = "_drawWithShader",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V"))
    private void ldlib2$lineScreenSizeFromViewport(Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                                                   ShaderInstance shader, CallbackInfo ci) {
        if (shader.SCREEN_SIZE != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP)) {
            shader.SCREEN_SIZE.set((float) GlStateManager.Viewport.width(), (float) GlStateManager.Viewport.height());
        }
    }
}
