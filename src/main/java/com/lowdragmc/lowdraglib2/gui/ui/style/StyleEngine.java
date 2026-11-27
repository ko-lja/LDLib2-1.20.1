package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class StyleEngine {
    public static final int SHEET_RULE_ORDER = 1 << 30;

    public final ModularUI modularUI;
    public final List<Stylesheet> globalSheets = new CopyOnWriteArrayList<>();

    // runtime
    private final Set<UIElement> dirtyElements = ConcurrentHashMap.newKeySet();
    private final Set<StyleBag> queue = ConcurrentHashMap.newKeySet();
    private int styleEpoch = 0;
    @Getter
    private final Map<UIElement, Map<Stylesheet, List<StyleRule>>> elementStyleRules = new ConcurrentHashMap<>();

    public StyleEngine(ModularUI modularUI) {
        this.modularUI = modularUI;
        StylesheetManager.INSTANCE.registerEngine(this);
    }

    /** Called when the owning ModularUI is closed/disposed. */
    public void dispose() {
        StylesheetManager.INSTANCE.unregisterEngine(this);
    }

    /**
     * Schedule all registered elements for style re-matching.
     * Called after a resource pack reload so elements pick up updated stylesheet rules.
     */
    public void scheduleFullReload() {
        dirtyElements.addAll(modularUI.getAllElements());
    }

    public void addStylesheets(Stylesheet... stylesheets) {
        addStylesheets(Arrays.asList(stylesheets));
    }

    public void addStylesheets(List<Stylesheet> stylesheets) {
        if (stylesheets.isEmpty()) return;
        globalSheets.addAll(stylesheets);
        modularUI.getAllElements().forEach(this::applyStyleRules);
    }

    public void addStylesheet(Stylesheet stylesheet) {
        globalSheets.add(stylesheet);
        modularUI.getAllElements().forEach(this::applyStyleRules);
    }

    public void removeStylesheet(Stylesheet sheet) {
        globalSheets.remove(sheet);
        for (var element : List.copyOf(elementStyleRules.keySet())) {
            var rulesMap = elementStyleRules.get(element);
            if (rulesMap != null && rulesMap.containsKey(sheet)) {
                applyStyleRules(element);
            }
        }
    }

    public void clearAllStylesheets() {
        globalSheets.clear();
        List.copyOf(elementStyleRules.keySet()).forEach(this::applyStyleRules);
    }

    public void enqueue(StyleBag bag) {
        queue.add(bag);
    }

    public boolean inQueue(StyleBag bag) {
        return queue.contains(bag);
    }

    public boolean requireCalculate() {
        return !queue.isEmpty() || !dirtyElements.isEmpty();
    }

    public void remove(StyleBag bag) {
        queue.remove(bag);
    }

    public void calculateStyle() {
        dirtyElements.forEach(this::updateElementStyle);
        dirtyElements.clear();

        if (queue.isEmpty()) return;
        styleEpoch++;
        var bags = new ArrayList<>(queue);
        queue.clear();
        for (StyleBag bag : bags) {
            bag.compute(styleEpoch);
        }
    }

    public void onElementRegister(UIElement element) {
        enqueue(element.getStyleBag());
        applyStyleRules(element);
    }

    /**
     * Add a local stylesheet scoped to the given owner element's subtree.
     * The stylesheet will only be applied to the owner and its descendants.
     */
    public void addLocalStylesheet(UIElement owner, Stylesheet stylesheet) {
        forEachRegisteredDescendantAndSelf(owner, this::applyStyleRules);
    }

    /**
     * Remove a local stylesheet that was scoped to the given owner element's subtree.
     */
    public void removeLocalStylesheet(UIElement owner, Stylesheet stylesheet) {
        forEachRegisteredDescendantAndSelf(owner, this::applyStyleRules);
    }

    private void forEachRegisteredDescendantAndSelf(UIElement root, Consumer<UIElement> action) {
        if (root.getModularUI() == modularUI) {
            action.accept(root);
            for (var child : root.getSafeChildren()) {
                forEachRegisteredDescendantAndSelf(child, action);
            }
        }
    }

    public void onElementUnregister(UIElement element) {
        if (elementStyleRules.containsKey(element)) {
            remove(element.getStyleBag());
            element.setStylesheetRules(List.of());
            elementStyleRules.remove(element);
        }
    }

    public void scheduleReloadElementStyles(UIElement element) {
        if (dirtyElements.contains(element)) return;
        dirtyElements.add(element);

        // 4. update children
        for (var child : element.getSafeChildren()) {
            scheduleReloadElementStyles(child);
        }
    }

    private void updateElementStyle(UIElement element) {
        if (element.getModularUI() != modularUI) return;
        applyStyleRules(element);
    }

    private void applyStyleRules(UIElement element) {
        var newRulesMap = new LinkedHashMap<Stylesheet, List<StyleRule>>();
        for (var stylesheet : globalSheets) {
            matchStylesheet(element, stylesheet, newRulesMap);
        }
        var ancestors = new ArrayList<UIElement>();
        for (var ancestor = element; ancestor != null; ancestor = ancestor.getParent()) {
            ancestors.add(ancestor);
        }
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            for (var sheet : ancestors.get(i).getLocalStylesheets()) {
                matchStylesheet(element, sheet, newRulesMap);
            }
        }

        var oldRulesMap = elementStyleRules.get(element);
        var oldRules = oldRulesMap == null ? List.<StyleRule>of() : flatten(oldRulesMap);
        var newRules = flatten(newRulesMap);
        if (oldRules.equals(newRules)) return;

        if (newRulesMap.isEmpty()) {
            elementStyleRules.remove(element);
        } else {
            elementStyleRules.put(element, Collections.unmodifiableMap(newRulesMap));
        }
        element.setStylesheetRules(newRules);
        enqueue(element.getStyleBag());
    }

    private static void matchStylesheet(UIElement element, Stylesheet sheet,
                                        Map<Stylesheet, List<StyleRule>> rulesMap) {
        rulesMap.remove(sheet);
        var rules = sheet.calculateValues(element);
        if (!rules.isEmpty()) {
            rulesMap.put(sheet, rules);
        }
    }

    private static List<StyleRule> flatten(Map<Stylesheet, List<StyleRule>> rulesMap) {
        var rules = new ArrayList<StyleRule>();
        rulesMap.values().forEach(rules::addAll);
        return rules;
    }
}
