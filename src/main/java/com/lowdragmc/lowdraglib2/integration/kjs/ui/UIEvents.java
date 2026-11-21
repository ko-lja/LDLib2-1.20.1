package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;

public interface UIEvents {
    EventGroup INSTANCE = EventGroup.of("LDLib2UI");
    EventHandler PLAYER = INSTANCE.add("player", ScriptTypePredicate.ALL, () -> KJSPlayerUIMenuType.PlayerUIEventJS.class).extra(Extra.REQUIRES_STRING);
    EventHandler BLOCK = INSTANCE.add("block", ScriptTypePredicate.ALL, () -> KJSBlockUIMenuType.BlockUIEventJS.class).extra(Extra.REQUIRES_STRING);
    EventHandler ITEM = INSTANCE.add("item", ScriptTypePredicate.ALL, () -> KJSHeldItemUIMenuType.ItemUIEventJS.class).extra(Extra.REQUIRES_STRING);
}
