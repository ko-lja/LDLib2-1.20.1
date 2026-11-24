package com.lowdragmc.lowdraglib2.utils.data;

import dev.vfyjxf.taffy.geometry.FloatSupplier;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import lombok.Getter;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class OptionalFloat {

    private static final OptionalFloat EMPTY = new OptionalFloat();

    public static OptionalFloat empty() {
        return EMPTY;
    }

    public static OptionalFloat of(float value) {
        if (Float.isNaN(value)) {
            return EMPTY;
        }
        return new OptionalFloat(value);
    }

    @Getter
    private final boolean isPresent;
    private final float value;

    private OptionalFloat() {
        isPresent = false;
        value = Float.NaN;
    }

    private OptionalFloat(float floatValue) {
        isPresent = true;
        value = floatValue;
    }

    public float unwrap() {
        return value;
    }

    public float getAsFloat() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(FloatConsumer action) {
        if (isPresent) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(FloatConsumer action, Runnable emptyAction) {
        if (isPresent) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    public float orElse(float other) {
        return isPresent ? value : other;
    }

    public float orElseGet(FloatSupplier supplier) {
        return isPresent ? value : supplier.get();
    }

    public float orElseThrow() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public<X extends Throwable> float orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof OptionalFloat other
            && (isPresent && other.isPresent
            ? Float.compare(value, other.value) == 0
            : isPresent == other.isPresent);
    }

    @Override
    public int hashCode() {
        return isPresent ? Float.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        return isPresent
            ? ("OptionalFloat[" + value + "]")
            : "OptionalFloat.empty";
    }
}
