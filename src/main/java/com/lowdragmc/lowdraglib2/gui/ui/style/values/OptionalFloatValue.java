package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.utils.data.OptionalFloat;
import org.jetbrains.annotations.Nullable;

public class OptionalFloatValue extends StyleValue<OptionalFloat> {

    public OptionalFloatValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable OptionalFloat doCompute(String rawValue) {
        rawValue = rawValue.trim();
        if (rawValue.equalsIgnoreCase("undefined")) {
            return OptionalFloat.empty();
        } else {
            return OptionalFloat.of(Float.parseFloat(rawValue));
        }
    }
}
