package com.lowdragmc.lowdraglib2.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class LDLibConfigScreen extends Screen {

    private static final String LANG = LDLib2.MOD_ID + ".configuration.";
    private static final int ROW_WIDTH = 360;
    private static final int LABEL_WIDTH = 150;
    private static final int FIELD_X = 154;
    private static final int FIELD_WIDTH = 140;
    private static final int RESET_X = 298;
    private static final int RESET_WIDTH = 60;
    private static final int INVALID_COLOR = 0xFF5555;

    private final Screen parent;
    private final ForgeConfigSpec spec;
    private final List<Row> rows = new ArrayList<>();
    private ConfigList list;

    public LDLibConfigScreen(Screen parent) {
        this(parent, LDLibClientConfig.SPEC);
    }

    public LDLibConfigScreen(Screen parent, ForgeConfigSpec spec) {
        super(Component.translatable(LANG + "title", "LDLib2"));
        this.parent = parent;
        this.spec = spec;
    }

    @Override
    protected void init() {
        if (rows.isEmpty() && spec.isLoaded()) {
            collect(spec.getValues(), "");
        }
        list = new ConfigList(minecraft, width, height, 32, height - 32, 28);
        rows.forEach(list::addRow);
        addWidget(list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done())
                .bounds(width / 2 - 155, height - 27, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(width / 2 + 5, height - 27, 150, 20).build());
    }

    private void collect(UnmodifiableConfig config, String prefix) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig section) {
                String key = prefix + entry.getKey();
                Component title = I18n.exists(LANG + key) ? Component.translatable(LANG + key) : Component.literal(key);
                rows.add(new HeaderRow(title.copy().withStyle(ChatFormatting.YELLOW)));
                collect(section, key + ".");
            } else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                Row row = createRow(configValue);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Row createRow(ForgeConfigSpec.ConfigValue<?> configValue) {
        List<String> path = configValue.getPath();
        String joined = String.join(".", path);
        ForgeConfigSpec.ValueSpec valueSpec = spec.getSpec().get(path);
        Component label = I18n.exists(LANG + joined) ? Component.translatable(LANG + joined) : Component.literal(joined);
        Component tooltip = null;
        if (I18n.exists(LANG + joined + ".tooltip")) {
            tooltip = Component.translatable(LANG + joined + ".tooltip");
        } else if (valueSpec != null && valueSpec.getComment() != null) {
            tooltip = Component.literal(valueSpec.getComment());
        }
        Object current = configValue.get();
        Object defaultValue = valueSpec == null ? current : valueSpec.getDefault();
        if (current instanceof Boolean b) {
            CycleButton<Boolean> button = CycleButton.onOffBuilder(b)
                    .displayOnlyValue()
                    .create(0, 0, FIELD_WIDTH, 20, label, (btn, value) -> {});
            return new CycleRow(label, tooltip, button, configValue, defaultValue);
        }
        if (current instanceof Enum<?> e) {
            List<Object> values = new ArrayList<>();
            for (Object constant : e.getDeclaringClass().getEnumConstants()) {
                if (valueSpec == null || valueSpec.test(constant)) {
                    values.add(constant);
                }
            }
            CycleButton<Object> button = CycleButton.<Object>builder(LDLibConfigScreen::enumName)
                    .withValues(values)
                    .withInitialValue(current)
                    .displayOnlyValue()
                    .create(0, 0, FIELD_WIDTH, 20, label, (btn, value) -> {});
            return new CycleRow(label, tooltip, button, configValue, defaultValue);
        }
        Function<String, Object> parser = null;
        if (current instanceof Integer) {
            parser = Integer::valueOf;
        } else if (current instanceof Long) {
            parser = Long::valueOf;
        } else if (current instanceof Double) {
            parser = Double::valueOf;
        } else if (current instanceof String) {
            parser = s -> s;
        }
        if (parser == null) {
            return null;
        }
        return new TextRow(label, tooltip, configValue, valueSpec, parser, String.valueOf(current), String.valueOf(defaultValue));
    }

    private static Component enumName(Object value) {
        try {
            Method method = value.getClass().getMethod("getTranslatedName");
            if (method.invoke(value) instanceof Component component) {
                return component;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Component.literal(((Enum<?>) value).name());
    }

    private void done() {
        rows.forEach(Row::apply);
        if (spec.isLoaded()) {
            spec.save();
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        void apply() {
        }
    }

    private abstract class ValueRow extends Row {
        final Component label;
        final List<AbstractWidget> widgets = new ArrayList<>();

        ValueRow(Component label) {
            this.label = label;
        }

        void place(AbstractWidget widget, int x, int top, int width) {
            widget.setX(x);
            widget.setY(top + 2);
            widget.setWidth(width);
        }

        abstract void layoutWidgets(int left, int top);

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawString(font, font.plainSubstrByWidth(label.getString(), LABEL_WIDTH), left, top + 8, 0xFFFFFF);
            layoutWidgets(left, top);
            for (AbstractWidget widget : widgets) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final class CycleRow extends ValueRow {
        private final CycleButton button;
        private final ForgeConfigSpec.ConfigValue value;
        private final Button reset;

        CycleRow(Component label, Component tooltip, CycleButton button, ForgeConfigSpec.ConfigValue value, Object defaultValue) {
            super(label);
            this.button = button;
            this.value = value;
            if (tooltip != null) {
                button.setTooltip(Tooltip.create(tooltip));
            }
            this.reset = Button.builder(Component.translatable("controls.reset"), b -> button.setValue(defaultValue))
                    .bounds(0, 0, RESET_WIDTH, 20).build();
            widgets.add(button);
            widgets.add(reset);
        }

        @Override
        void layoutWidgets(int left, int top) {
            place(button, left + FIELD_X, top, FIELD_WIDTH);
            place(reset, left + RESET_X, top, RESET_WIDTH);
        }

        @Override
        void apply() {
            value.set(button.getValue());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final class TextRow extends ValueRow {
        private final EditBox box;
        private final ForgeConfigSpec.ConfigValue value;
        private final ForgeConfigSpec.ValueSpec valueSpec;
        private final Function<String, Object> parser;
        private final Button reset;

        TextRow(Component label, Component tooltip, ForgeConfigSpec.ConfigValue value, ForgeConfigSpec.ValueSpec valueSpec,
                Function<String, Object> parser, String initial, String defaultText) {
            super(label);
            this.value = value;
            this.valueSpec = valueSpec;
            this.parser = parser;
            this.box = new EditBox(font, 0, 0, FIELD_WIDTH, 20, label);
            box.setMaxLength(256);
            box.setValue(initial);
            box.setResponder(text -> box.setTextColor(parsed() == null ? INVALID_COLOR : 0xE0E0E0));
            if (tooltip != null) {
                box.setTooltip(Tooltip.create(tooltip));
            }
            this.reset = Button.builder(Component.translatable("controls.reset"), b -> box.setValue(defaultText))
                    .bounds(0, 0, RESET_WIDTH, 20).build();
            widgets.add(box);
            widgets.add(reset);
        }

        private Object parsed() {
            try {
                Object parsed = parser.apply(box.getValue());
                return valueSpec == null || valueSpec.test(parsed) ? parsed : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        void layoutWidgets(int left, int top) {
            place(box, left + FIELD_X, top, FIELD_WIDTH);
            place(reset, left + RESET_X, top, RESET_WIDTH);
        }

        @Override
        void apply() {
            Object parsed = parsed();
            if (parsed != null) {
                value.set(parsed);
            }
        }
    }

    private final class HeaderRow extends Row {
        private final Component title;

        HeaderRow(Component title) {
            this.title = title;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawCenteredString(font, title, left + width / 2, top + 8, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private static final class ConfigList extends ContainerObjectSelectionList<Row> {
        ConfigList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
        }

        void addRow(Row row) {
            addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + ROW_WIDTH / 2 + 8;
        }
    }
}
