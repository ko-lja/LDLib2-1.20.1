package com.lowdragmc.lowdraglib2.compat.nbt;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

public interface INBTSerializable<T extends Tag> extends net.minecraftforge.common.util.INBTSerializable<T> {
    T serializeNBT(HolderLookup.Provider provider);

    void deserializeNBT(HolderLookup.Provider provider, T nbt);

    @Override
    default T serializeNBT() {
        return serializeNBT(Platform.getFrozenRegistry());
    }

    @Override
    default void deserializeNBT(T nbt) {
        deserializeNBT(Platform.getFrozenRegistry(), nbt);
    }
}
