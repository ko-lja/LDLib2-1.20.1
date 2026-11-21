package com.lowdragmc.lowdraglib2.compat.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector4f;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class LDCodecs {
    private LDCodecs() {
    }

    public static final Codec<Component> COMPONENT = ExtraCodecs.COMPONENT;

    public static final Codec<Vector4f> VECTOR4F = Codec.FLOAT.listOf().comapFlatMap(
            list -> Util.fixedSize(list, 4).map(values -> new Vector4f(values.get(0), values.get(1), values.get(2), values.get(3))),
            vector -> List.of(vector.x(), vector.y(), vector.z(), vector.w()));

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

    public static <A> MapCodec<Optional<A>> optionalFieldOf(Codec<A> codec, String name) {
        return new StrictOptionalFieldCodec<>(name, codec);
    }

    public static <A> MapCodec<A> optionalFieldOf(Codec<A> codec, String name, A defaultValue) {
        return optionalFieldOf(codec, name).xmap(
                value -> value.orElse(defaultValue),
                value -> Objects.equals(value, defaultValue) ? Optional.empty() : Optional.of(value));
    }

    private static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final String name;
        private final Codec<A> elementCodec;

        private StrictOptionalFieldCodec(String name, Codec<A> elementCodec) {
            this.name = name;
            this.elementCodec = elementCodec;
        }

        @Override
        public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
            var value = input.get(name);
            if (value == null) {
                return DataResult.success(Optional.empty());
            }
            return elementCodec.parse(ops, value).map(Optional::of);
        }

        @Override
        public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (input.isPresent()) {
                return prefix.add(name, elementCodec.encodeStart(ops, input.get()));
            }
            return prefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString(name));
        }
    }
}
