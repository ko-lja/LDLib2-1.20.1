package com.lowdragmc.lowdraglib2.compat.util;

public enum TriState {
    FALSE,
    DEFAULT,
    TRUE;

    public static TriState of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean orElse(boolean fallback) {
        return this == DEFAULT ? fallback : this == TRUE;
    }
}
