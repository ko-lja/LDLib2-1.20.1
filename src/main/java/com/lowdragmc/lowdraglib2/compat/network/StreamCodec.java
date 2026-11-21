package com.lowdragmc.lowdraglib2.compat.network;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import io.netty.buffer.ByteBuf;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface StreamCodec<B extends ByteBuf, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {
    static <B extends ByteBuf, V> StreamCodec<B, V> of(final StreamEncoder<B, V> encoder, final StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(buf, value);
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> encoder, final StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.decode(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.encode(value, buf);
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec<B, V> unit(final V instance) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return instance;
            }

            @Override
            public void encode(B buf, V value) {
                if (!value.equals(instance)) {
                    throw new IllegalStateException("Can't encode '" + value + "', expected '" + instance + "'");
                }
            }
        };
    }

    static <B extends ByteBuf, C, T1> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final Function<T1, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                return constructor.apply(value1);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final BiFunction<T1, T2, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                return constructor.apply(value1, value2);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2, T3> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final Function3<T1, T2, T3, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                T3 value3 = codec3.decode(buf);
                return constructor.apply(value1, value2, value3);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2, T3, T4> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4, final Function4<T1, T2, T3, T4, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                T3 value3 = codec3.decode(buf);
                T4 value4 = codec4.decode(buf);
                return constructor.apply(value1, value2, value3, value4);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4, final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5, final Function5<T1, T2, T3, T4, T5, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                T3 value3 = codec3.decode(buf);
                T4 value4 = codec4.decode(buf);
                T5 value5 = codec5.decode(buf);
                return constructor.apply(value1, value2, value3, value4, value5);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
                codec5.encode(buf, getter5.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4, final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5, final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6, final Function6<T1, T2, T3, T4, T5, T6, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                T3 value3 = codec3.decode(buf);
                T4 value4 = codec4.decode(buf);
                T5 value5 = codec5.decode(buf);
                T6 value6 = codec6.decode(buf);
                return constructor.apply(value1, value2, value3, value4, value5, value6);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
                codec5.encode(buf, getter5.apply(value));
                codec6.encode(buf, getter6.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4, final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5, final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6, final StreamCodec<? super B, T7> codec7, final Function<C, T7> getter7, final Function7<T1, T2, T3, T4, T5, T6, T7, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                T3 value3 = codec3.decode(buf);
                T4 value4 = codec4.decode(buf);
                T5 value5 = codec5.decode(buf);
                T6 value6 = codec6.decode(buf);
                T7 value7 = codec7.decode(buf);
                return constructor.apply(value1, value2, value3, value4, value5, value6, value7);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
                codec5.encode(buf, getter5.apply(value));
                codec6.encode(buf, getter6.apply(value));
                codec7.encode(buf, getter7.apply(value));
            }
        };
    }

    static <B extends ByteBuf, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1, final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2, final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3, final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4, final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5, final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6, final StreamCodec<? super B, T7> codec7, final Function<C, T7> getter7, final StreamCodec<? super B, T8> codec8, final Function<C, T8> getter8, final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> constructor) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buf) {
                T1 value1 = codec1.decode(buf);
                T2 value2 = codec2.decode(buf);
                T3 value3 = codec3.decode(buf);
                T4 value4 = codec4.decode(buf);
                T5 value5 = codec5.decode(buf);
                T6 value6 = codec6.decode(buf);
                T7 value7 = codec7.decode(buf);
                T8 value8 = codec8.decode(buf);
                return constructor.apply(value1, value2, value3, value4, value5, value6, value7, value8);
            }

            @Override
            public void encode(B buf, C value) {
                codec1.encode(buf, getter1.apply(value));
                codec2.encode(buf, getter2.apply(value));
                codec3.encode(buf, getter3.apply(value));
                codec4.encode(buf, getter4.apply(value));
                codec5.encode(buf, getter5.apply(value));
                codec6.encode(buf, getter6.apply(value));
                codec7.encode(buf, getter7.apply(value));
                codec8.encode(buf, getter8.apply(value));
            }
        };
    }

    default <S> StreamCodec<B, S> apply(CodecOperation<B, V, S> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> to, final Function<? super O, ? extends V> from) {
        return new StreamCodec<>() {
            @Override
            public O decode(B buf) {
                return to.apply(StreamCodec.this.decode(buf));
            }

            @Override
            public void encode(B buf, O value) {
                StreamCodec.this.encode(buf, from.apply(value));
            }
        };
    }

    @SuppressWarnings("unchecked")
    default <U> StreamCodec<B, U> dispatch(final Function<? super U, ? extends V> keyGetter, final Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codecGetter) {
        return new StreamCodec<>() {
            @Override
            public U decode(B buf) {
                V key = StreamCodec.this.decode(buf);
                StreamCodec<? super B, ? extends U> codec = codecGetter.apply(key);
                return codec.decode(buf);
            }

            @Override
            public void encode(B buf, U value) {
                V key = keyGetter.apply(value);
                StreamCodec<B, U> codec = (StreamCodec<B, U>) codecGetter.apply(key);
                StreamCodec.this.encode(buf, key);
                codec.encode(buf, value);
            }
        };
    }

    @FunctionalInterface
    interface CodecOperation<B extends ByteBuf, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}
