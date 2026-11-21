package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("nextElementByte")
    int getNextElementByte();

    @Accessor("buffer")
    ByteBuffer getBuffer();
}
