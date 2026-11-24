package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.OptionalFloatConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.layout.TaffyCodecs;
import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.OptionalFloatValue;
import com.lowdragmc.lowdraglib2.utils.data.OptionalFloat;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class OptionalFloatProperty extends Property<OptionalFloat> {
    public OptionalFloatProperty(String name, OptionalFloat initialValue) {
        super(name, OptionalFloat.class, TaffyCodecs.OPTIONAL_FLOAT_CODEC, initialValue, OptionalFloatValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<OptionalFloat> getter, Consumer<OptionalFloat> setter) {
        return new OptionalFloatConfigurator(name, getter, setter, initialValue, true);
    }

    private OptionalFloat interpolate(OptionalFloat from, OptionalFloat to, float interpolation) {
        if (from.isPresent() && to.isPresent()) {
            return OptionalFloat.of(from.getAsFloat() + (to.getAsFloat() - from.getAsFloat()) * interpolation);
        }
        return IValueInterpolator.<OptionalFloat>binary().interpolate(from, to, interpolation);
    }

}
