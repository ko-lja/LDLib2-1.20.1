package com.lowdragmc.lowdraglib2.compat;

import com.lowdragmc.lowdraglib2.compat.network.ByteBufCodecs;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamCodecTest {
    record Triple(int count, String name, boolean flag) {}

    record Quad(int a, String b, boolean c, long d) {}

    interface Shape {}

    record Circle(float radius) implements Shape {}

    record Square(int side) implements Shape {}

    static <B extends ByteBuf, V> V roundTrip(StreamCodec<B, V> codec, B buf, V value) {
        codec.encode(buf, value);
        V decoded = codec.decode(buf);
        assertEquals(0, buf.readableBytes());
        return decoded;
    }

    static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    @Test
    void compositeThreeFieldRecord() {
        StreamCodec<ByteBuf, Triple> codec = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Triple::count,
                ByteBufCodecs.STRING_UTF8, Triple::name,
                ByteBufCodecs.BOOL, Triple::flag,
                Triple::new);
        var value = new Triple(300, "ldlib", true);
        assertEquals(value, roundTrip(codec, Unpooled.buffer(), value));
    }

    @Test
    void compositeOneAndFourFields() {
        StreamCodec<ByteBuf, Integer> single = StreamCodec.composite(ByteBufCodecs.VAR_INT, Function.identity(), Function.identity());
        assertEquals(7, roundTrip(single, Unpooled.buffer(), 7));
        StreamCodec<ByteBuf, Quad> quad = StreamCodec.composite(
                ByteBufCodecs.INT, Quad::a,
                ByteBufCodecs.STRING_UTF8, Quad::b,
                ByteBufCodecs.BOOL, Quad::c,
                ByteBufCodecs.VAR_LONG, Quad::d,
                Quad::new);
        var value = new Quad(-5, "x", false, 1L << 40);
        assertEquals(value, roundTrip(quad, Unpooled.buffer(), value));
    }

    @Test
    void ofAndOfMemberAgree() {
        StreamCodec<FriendlyByteBuf, String> viaOf = StreamCodec.of((buf, value) -> buf.writeUtf(value), buf -> buf.readUtf());
        StreamCodec<FriendlyByteBuf, String> viaMember = StreamCodec.ofMember((value, buf) -> buf.writeUtf(value), FriendlyByteBuf::readUtf);
        var first = buffer();
        viaOf.encode(first, "hello");
        assertEquals("hello", viaMember.decode(first));
        var second = buffer();
        viaMember.encode(second, "world");
        assertEquals("world", viaOf.decode(second));
    }

    @Test
    void mapAndList() {
        StreamCodec<ByteBuf, BlockPos> pos = ByteBufCodecs.VAR_LONG.map(BlockPos::of, BlockPos::asLong);
        var at = new BlockPos(-12, 64, 900);
        assertEquals(at, roundTrip(pos, Unpooled.buffer(), at));
        StreamCodec<ByteBuf, List<String>> strings = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
        var list = List.of("a", "", "ccc");
        assertEquals(list, roundTrip(strings, Unpooled.buffer(), list));
        assertEquals(List.<String>of(), roundTrip(strings, Unpooled.buffer(), List.<String>of()));
    }

    @Test
    void unit() {
        StreamCodec<ByteBuf, String> unit = StreamCodec.unit("only");
        var buf = Unpooled.buffer();
        unit.encode(buf, "only");
        assertEquals(0, buf.readableBytes());
        assertEquals("only", unit.decode(buf));
        assertThrows(IllegalStateException.class, () -> unit.encode(Unpooled.buffer(), "other"));
    }

    @Test
    void dispatch() {
        StreamCodec<ByteBuf, Shape> circle = ByteBufCodecs.FLOAT.map(f -> (Shape) new Circle(f), s -> ((Circle) s).radius());
        StreamCodec<ByteBuf, Shape> square = ByteBufCodecs.VAR_INT.map(i -> (Shape) new Square(i), s -> ((Square) s).side());
        StreamCodec<ByteBuf, Shape> shape = ByteBufCodecs.VAR_INT.<Shape>dispatch(
                s -> s instanceof Circle ? 0 : 1,
                id -> id == 0 ? circle : square);
        assertEquals(new Circle(2.5f), roundTrip(shape, Unpooled.buffer(), new Circle(2.5f)));
        assertEquals(new Square(9), roundTrip(shape, Unpooled.buffer(), new Square(9)));
    }

    @Test
    void primitives() {
        assertEquals(true, roundTrip(ByteBufCodecs.BOOL, Unpooled.buffer(), true));
        assertEquals((byte) -3, roundTrip(ByteBufCodecs.BYTE, Unpooled.buffer(), (byte) -3));
        assertEquals((short) 1234, roundTrip(ByteBufCodecs.SHORT, Unpooled.buffer(), (short) 1234));
        assertEquals(-77, roundTrip(ByteBufCodecs.INT, Unpooled.buffer(), -77));
        assertEquals(123456, roundTrip(ByteBufCodecs.VAR_INT, Unpooled.buffer(), 123456));
        assertEquals(-9L, roundTrip(ByteBufCodecs.VAR_LONG, Unpooled.buffer(), -9L));
        assertEquals(1.5f, roundTrip(ByteBufCodecs.FLOAT, Unpooled.buffer(), 1.5f));
        assertEquals(2.25, roundTrip(ByteBufCodecs.DOUBLE, Unpooled.buffer(), 2.25));
        assertEquals("héllo", roundTrip(ByteBufCodecs.STRING_UTF8, Unpooled.buffer(), "héllo"));
    }

    @Test
    void vectorsAndTags() {
        var vector = new Vector3f(1, 2, 3);
        assertEquals(vector, roundTrip(ByteBufCodecs.VECTOR3F, Unpooled.buffer(), vector));
        var quaternion = new Quaternionf(0.1f, 0.2f, 0.3f, 0.9f);
        assertEquals(quaternion, roundTrip(ByteBufCodecs.QUATERNIONF, Unpooled.buffer(), quaternion));
        var compound = new CompoundTag();
        compound.putString("k", "v");
        compound.putInt("n", 4);
        assertEquals(compound, roundTrip(ByteBufCodecs.COMPOUND_TAG, Unpooled.buffer(), compound));
        Tag number = IntTag.valueOf(42);
        assertEquals(number, roundTrip(ByteBufCodecs.TRUSTED_TAG, Unpooled.buffer(), number));
    }

    @Test
    void fromCodec() {
        StreamCodec<ByteBuf, List<Integer>> codec = ByteBufCodecs.fromCodec(Codec.INT.listOf());
        var list = List.of(1, 2, 3);
        assertEquals(list, roundTrip(codec, Unpooled.buffer(), list));
    }
}
