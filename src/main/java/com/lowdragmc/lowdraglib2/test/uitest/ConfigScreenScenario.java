package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.client.LDLibConfigScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;

@LDLRegisterClient(name = "config_screen", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ConfigScreenScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("config").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openScreen("config screen", ctx -> new LDLibConfigScreen(null))
                .awaitScreen(LDLibConfigScreen.class)
                .frames(5)
                .step("remember the cache setting", ctx -> ctx.put("before", LDLibClientConfig.isTextLayoutCache()))
                .step("shoot", ctx -> ctx.screenshot("config_screen"))
                .step("toggle the boolean", ctx -> {
                    CycleButton<?> button = booleanButton(ctx);
                    ctx.put("text", button.getMessage().getString());
                    button.onPress();
                })
                .frames(3)
                .check("the button text changed",
                        ctx -> !booleanButton(ctx).getMessage().getString().equals(ctx.<String>get("text")))
                .step("shoot toggled", ctx -> ctx.screenshot("config_screen_toggled"))
                .step("press cancel", ctx -> {
                    for (GuiEventListener child : ctx.screen().children()) {
                        if (child instanceof Button button && button.getMessage().equals(CommonComponents.GUI_CANCEL)) {
                            button.onPress();
                        }
                    }
                })
                .frames(2)
                .check("the config value is unchanged",
                        ctx -> ctx.<Boolean>get("before") == LDLibClientConfig.isTextLayoutCache())
                .check("the screen closed", ctx -> !(ctx.screen() instanceof LDLibConfigScreen));
    }

    private static CycleButton<?> booleanButton(TestContext ctx) {
        for (GuiEventListener child : ctx.screen().children()) {
            if (child instanceof ContainerObjectSelectionList<?> list) {
                for (var entry : list.children()) {
                    for (GuiEventListener widget : entry.children()) {
                        if (widget instanceof CycleButton<?> button && button.getValue() instanceof Boolean) {
                            return button;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("no boolean button");
    }
}
