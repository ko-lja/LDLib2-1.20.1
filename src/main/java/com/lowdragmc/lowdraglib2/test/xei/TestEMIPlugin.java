package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;
import com.lowdragmc.lowdraglib2.test.TestItem;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.emi.emi.api.EmiRegistry;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class TestEMIPlugin {
    public static final AtomicInteger CLICKS = new AtomicInteger();
    public static final AtomicInteger ROOT_PRESSES = new AtomicInteger();

    public static ModularUI createUI() {
        var mui = new TestRecipe().createModularUI();
        mui.ui.rootElement.addEventListener(UIEvents.MOUSE_DOWN, event -> ROOT_PRESSES.incrementAndGet(), true);
        mui.ui.rootElement.addChild(new Button()
                .setOnClick(event -> CLICKS.incrementAndGet())
                .setText("click")
                .layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(100).top(4).width(40)));
        return mui;
    }

    public static void register(EmiRegistry registry) {
        var category = new TestEmiRecipeCategory();
        registry.addCategory(category);
        registry.addRecipe(new TestEmiRecipe(category));
        registry.addWorkstation(category, EmiStack.of(TestItem.ITEM));
    }

    protected static class TestEmiRecipeCategory extends EmiRecipeCategory {
        public TestEmiRecipeCategory() {
            super(LDLib2.id("test_category"), EmiStack.of(TestItem.ITEM));
        }
    }

    protected static class TestEmiRecipe extends ModularUIEMIRecipe {
        @Getter
        protected TestEmiRecipeCategory category;

        public TestEmiRecipe(TestEmiRecipeCategory category) {
            super(recipe -> createUI());
            this.category = category;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return LDLib2.id("/test_recipe");
        }

        @Override
        public int getDisplayWidth() {
            return TestRecipe.WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return TestRecipe.HEIGHT;
        }
    }
}
