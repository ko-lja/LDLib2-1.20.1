package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.TaffyDirection;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Deprecated
public class DataComponentConfigurator extends ConfiguratorGroup {
    public final Button resetButton = new Button();
    @Getter
    private CompoundTag prototype = new CompoundTag();
    private final Supplier<CompoundTag> supplier;
    private final Consumer<CompoundTag> consumer;
    private final boolean forceUpdate;

    public DataComponentConfigurator(@Nullable CompoundTag prototype, Supplier<CompoundTag> supplier, Consumer<CompoundTag> consumer, boolean forceUpdate) {
        super("configurator.data_component");
        this.supplier = supplier;
        this.consumer = consumer;
        this.forceUpdate = forceUpdate;
        setPrototype(prototype);
        this.resetButton.setText("configurator.reset_prototype").setOnClick(e -> {
            resetPrototype();
            e.stopPropagation();
        });
        this.resetButton.text.layout(layout -> layout.widthPercent(100));
        this.resetButton.text.textStyle(textStyle -> textStyle.adaptiveWidth(false).textWrap(TextWrap.HOVER_ROLL)).setOverflowVisible(false);
        this.inlineContainer.layout(layout -> layout.direction(TaffyDirection.RTL));
        this.inlineContainer.addChild(resetButton);
    }

    public void resetPrototype() {
        consumer.accept(prototype.copy());
        notifyChanges();
    }

    public DataComponentConfigurator setPrototype(@Nullable CompoundTag prototype) {
        var newPrototype = prototype == null ? new CompoundTag() : prototype;
        if (this.prototype.equals(newPrototype) && !getConfigurators().isEmpty()) return this;
        this.prototype = newPrototype;
        removeAllConfigurators();
        var tagConfigurator = new TagConfigurator("NBT", () -> {
            var current = supplier.get();
            return current == null || current.isEmpty() ? EndTag.INSTANCE : current;
        }, tag -> {
            if (tag instanceof CompoundTag compound) consumer.accept(compound.copy());
            else consumer.accept(new CompoundTag());
        }, EndTag.INSTANCE, forceUpdate);
        tagConfigurator.tagField.setTagValidator(DataComponentConfigurator::isCompoundOrEmpty);
        addConfigurator(tagConfigurator);
        return this;
    }

    private static boolean isCompoundOrEmpty(Tag tag) {
        return tag instanceof CompoundTag || tag == EndTag.INSTANCE;
    }
}
