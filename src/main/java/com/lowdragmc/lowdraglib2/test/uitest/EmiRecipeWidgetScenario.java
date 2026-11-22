package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIWidget;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.xei.TestEMIPlugin;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

@LDLRegisterClient(name = "emi_recipe_widget", group = "ldlib2", registry = UIScenario.REGISTRY, modID = "emi",
        environment = RegistrationEnvironment.DEV_ONLY)
public class EmiRecipeWidgetScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("emi").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.waitUntil("EMI has registered the test recipe", ctx -> findRecipe() != null)
                .step("open the test recipe in EMI", ctx -> EmiApi.displayRecipe(findRecipe()))
                .awaitScreen(RecipeScreen.class)
                .waitUntil("the ModularUIEMIWidget is on the page", ctx -> widget(ctx.screen()) != null)
                .frames(5)
                .step("reset counters", ctx -> {
                    TestEMIPlugin.CLICKS.set(0);
                    TestEMIPlugin.ROOT_PRESSES.set(0);
                })
                .screenshot("recipe_screen")
                .clickAt("a point outside the widget", ctx -> outsidePoint(ctx.screen()), 0)
                .frames(3)
                .check("an outside click is not forwarded to LDLib", ctx -> TestEMIPlugin.ROOT_PRESSES.get() == 0)
                .check("an outside click does not press the button", ctx -> TestEMIPlugin.CLICKS.get() == 0)
                .clickAt("the LDLib button inside the widget", ctx -> buttonPoint(ctx.screen()), 0)
                .frames(3)
                .check("the inside click reached LDLib", ctx -> TestEMIPlugin.ROOT_PRESSES.get() == 1)
                .check("the button handler ran once", ctx -> TestEMIPlugin.CLICKS.get() == 1)
                .screenshot("after_click")
                .step("press, drag within the button, release", ctx -> {
                    var p = buttonPoint(ctx.screen());
                    ctx.input().moveTo(p[0], p[1]);
                    ctx.put("p", p);
                })
                .frames(2)
                .step("press", ctx -> {
                    var p = ctx.<float[]>get("p");
                    ctx.input().mouseDown(p[0], p[1], 0);
                })
                .frames(2)
                .step("drag", ctx -> {
                    var p = ctx.<float[]>get("p");
                    ctx.input().dragTo(p[0] + 1, p[1], 0);
                })
                .frames(2)
                .step("release", ctx -> {
                    var p = ctx.<float[]>get("p");
                    ctx.input().mouseUp(p[0] + 1, p[1], 0);
                })
                .frames(3)
                .check("press-drag-release inside the button clicked it again", ctx -> TestEMIPlugin.CLICKS.get() == 2)
                .clickAt("outside again", ctx -> outsidePoint(ctx.screen()), 0)
                .frames(3)
                .check("a second outside click is still not forwarded", ctx -> TestEMIPlugin.ROOT_PRESSES.get() == 2)
                .screenshot("final")
                .teardown("close", ctx -> ctx.mc().setScreen(null));
    }

    private static EmiRecipe findRecipe() {
        for (var recipe : EmiApi.getRecipeManager().getRecipes()) {
            if (recipe.getId() != null && recipe.getId().getPath().equals("/test_recipe")) return recipe;
        }
        return null;
    }

    private static ModularUIEMIWidget widget(Screen screen) {
        return found(screen) == null ? null : found(screen)[1] instanceof ModularUIEMIWidget w ? w : null;
    }

    private static Object[] found(Screen screen) {
        if (!(screen instanceof RecipeScreen)) return null;
        try {
            var field = RecipeScreen.class.getDeclaredField("currentPage");
            field.setAccessible(true);
            for (var group : (List<WidgetGroup>) field.get(screen)) {
                for (var w : group.widgets) {
                    if (w instanceof ModularUIEMIWidget) return new Object[]{group, w};
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    private static float[] outsidePoint(Screen screen) {
        var group = (WidgetGroup) found(screen)[0];
        return new float[]{group.x() - 3, group.y() + 60};
    }

    private static float[] buttonPoint(Screen screen) {
        var group = (WidgetGroup) found(screen)[0];
        var widget = (ModularUIEMIWidget) found(screen)[1];
        UIElement button = widget.modularUI.ui.rootElement.getChildren().get(widget.modularUI.ui.rootElement.getChildren().size() - 1);
        var local = ElementBounds.of(button);
        return new float[]{local.centerX(), local.centerY()};
    }
}
