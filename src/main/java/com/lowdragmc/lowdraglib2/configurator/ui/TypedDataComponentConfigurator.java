package com.lowdragmc.lowdraglib2.configurator.ui;

import net.minecraft.nbt.Tag;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Deprecated
public class TypedDataComponentConfigurator extends TagConfigurator {

    public TypedDataComponentConfigurator(String name, Supplier<Tag> supplier, Consumer<Tag> onUpdate, @Nonnull Tag defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
    }
}
