package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.utils.data.OptionalFloat;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OptionalFloatConfigurator extends ValueConfigurator<OptionalFloat> {
    public final TextField textField;
    public final Selector<Boolean> definedSelector;
    @Getter
    protected Float min, max, wheel;

    public OptionalFloatConfigurator(String name, Supplier<OptionalFloat> supplier, Consumer<OptionalFloat> onUpdate, @Nonnull OptionalFloat defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
        });
        inlineContainer.addChildren(textField = new TextField(), definedSelector = new Selector<>());

        definedSelector.buttonIcon.setDisplay(false);
        definedSelector.layout(layout -> {
            layout.flex(1);
        });
        definedSelector.setCandidates(List.of(false, true));
        updateSelector();

        definedSelector.setOnValueChanged(defined -> {
            if (defined) {
                updateValueActively(OptionalFloat.of(min == null ? 0 : min));
            } else {
                updateValueActively(OptionalFloat.empty());
            }
            updateTextFieldValue();
        });
        definedSelector.setCandidateUIProvider(UIElementProvider.text(defined -> defined ?
                Component.literal("-") : Component.translatable("initial")));

        textField.layout(layout -> {
            layout.flex(2);
        }).setDisplay(value.isPresent());
        textField.setTextResponder(this::onNumberUpdate);
        updateTextField();
    }

    @Override
    protected void onPaste(OptionalFloat pasted) {
        if ((max != null && pasted.unwrap() <= max) && (min != null && pasted.unwrap() >= min)) {
            super.onPaste(pasted);
        }
    }

    @Override
    protected void onDropObject(@Nonnull Object object) {
        if (object instanceof OptionalFloat floatOptional && (max != null && floatOptional.unwrap() <= max) && (min != null && floatOptional.unwrap() >= min)) {
            updateValueActively(floatOptional);
            updateTextFieldValue();
        }
    }

    public OptionalFloatConfigurator setRange(Float min, Float max) {
        this.min = min;
        this.max = max;
        updateTextField();
        return this;
    }

    public OptionalFloatConfigurator setWheel(Float wheel) {
        if (wheel.doubleValue() == 0) return this;
        this.wheel = wheel;
        updateTextField();
        return this;
    }

    protected void updateTextField() {
        textField.setNumbersOnlyFloat(min == null ? -Float.MAX_VALUE : min, max == null ? Float.MAX_VALUE : max);
        var wheelValue = 0.1f;
        if (wheel != null) wheelValue = wheel;
        textField.setWheelDur(wheelValue);
        updateTextFieldValue();
    }

    protected void updateTextFieldValue() {
        assert value != null;
        textField.setDisplay(value.isPresent());
        textField.setText(String.valueOf(value.unwrap()), false);
    }

    protected void updateSelector() {
        assert value != null;
        definedSelector.setValue(value.isPresent(), false);
    }

    @Override
    protected void onValueUpdatePassively(OptionalFloat newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        updateValueActively(Optional.ofNullable(definedSelector.getValue()).orElse(false) ? OptionalFloat.of(number) : OptionalFloat.empty());
    }
}
