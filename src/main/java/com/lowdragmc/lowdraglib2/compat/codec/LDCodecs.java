package com.lowdragmc.lowdraglib2.compat.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

public final class LDCodecs {
    private LDCodecs() {
    }

    public static final Codec<Component> COMPONENT = ExtraCodecs.COMPONENT;

    public static <T> T getOrThrow(DataResult<T> result) {
        return result.getOrThrow(false, message -> {
            throw new IllegalStateException(message);
        });
    }

    public static <T> Codec<T> withAlternative(Codec<T> primary, Codec<? extends T> alternative) {
        return Codec.either(primary, alternative).xmap(either -> either.map(Function.identity(), Function.identity()), Either::left);
    }

    public static <T, U> Codec<T> withAlternative(Codec<T> primary, Codec<U> alternative, Function<U, T> converter) {
        return Codec.either(primary, alternative).xmap(either -> either.map(Function.identity(), converter), Either::left);
    }

    public static <T> Codec<T> validate(Codec<T> codec, Function<T, DataResult<T>> validator) {
        return ExtraCodecs.validate(codec, validator);
    }
}
