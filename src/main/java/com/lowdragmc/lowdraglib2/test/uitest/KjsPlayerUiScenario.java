package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.LDKJSMenuTypes;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

@LDLRegisterClient(name = "kjs_player_ui", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class KjsPlayerUiScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("kjs").requiresWorld(true).guiScale(3);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.server("open the script-defined player UI", sc ->
                        LDKJSMenuTypes.openPlayerUI(sc.player(), "ldlib2_kjs_test"))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .awaitElement("#kjs_label")
                .checkExists("#kjs_button")
                .checkExists("#kjs_swatch")
                .checkText("#kjs_label", "KJS label ready")
                .screenshot("01_opened")
                .click("#kjs_button")
                .checkText("#kjs_label", "KJS clicked 1")
                .click("#kjs_button")
                .checkText("#kjs_label", "KJS clicked 2")
                .screenshot("02_clicked")
                .teardown("close", ctx -> ctx.requirePlayer().closeContainer());
    }
}
