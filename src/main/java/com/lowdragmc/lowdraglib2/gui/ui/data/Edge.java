package com.lowdragmc.lowdraglib2.gui.ui.data;

public enum Edge {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    START,
    END,
    HORIZONTAL,
    VERTICAL,
    ALL;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case LEFT -> "left";
            case TOP -> "top";
            case RIGHT -> "right";
            case BOTTOM -> "bottom";
            case START -> "start";
            case END -> "end";
            case HORIZONTAL -> "horizontal";
            case VERTICAL -> "vertical";
            case ALL -> "all";
        };
    }
}
