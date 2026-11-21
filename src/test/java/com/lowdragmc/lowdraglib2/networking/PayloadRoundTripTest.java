package com.lowdragmc.lowdraglib2.networking;

import com.lowdragmc.lowdraglib2.compat.network.CustomPacketPayload;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.lowdragmc.lowdraglib2.networking.both.PacketModularUISync;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCBlockEntity;
import com.lowdragmc.lowdraglib2.networking.both.PacketRPCPacket;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEvent;
import com.lowdragmc.lowdraglib2.networking.both.PacketUIRPCEventReturn;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntities;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PayloadRoundTripTest {
    private static boolean bootstrapped;
    private static Throwable bootstrapFailure;

    @BeforeAll
    static void bootstrap() {
        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            bootstrapped = true;
        } catch (Throwable e) {
            bootstrapped = false;
            bootstrapFailure = e;
            e.printStackTrace();
        }
    }

    private static <T extends CustomPacketPayload> T roundTrip(StreamCodec<FriendlyByteBuf, T> codec, T value) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buf, value);
            var decoded = codec.decode(buf);
            assertEquals(0, buf.readableBytes());
            assertSame(value.type(), decoded.type());
            return decoded;
        } finally {
            buf.release();
        }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        Field field = null;
        while (field == null) {
            try {
                field = type.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
                if (type == null) throw e;
            }
        }
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void modularUiSync() throws ReflectiveOperationException {
        var packet = new PacketModularUISync(new byte[]{1, 2, 3, 4});
        var decoded = roundTrip(PacketModularUISync.CODEC, packet);
        assertArrayEquals((byte[]) field(packet, "data"), (byte[]) field(decoded, "data"));
    }

    @Test
    void uiRpcEvent() {
        var decoded = roundTrip(PacketUIRPCEvent.CODEC, new PacketUIRPCEvent(new byte[]{9, 8, 7}));
        assertArrayEquals(new byte[]{9, 8, 7}, decoded.eventData);
    }

    @Test
    void uiRpcEventReturn() {
        var decoded = roundTrip(PacketUIRPCEventReturn.CODEC, new PacketUIRPCEventReturn(new byte[]{0, 5}));
        assertArrayEquals(new byte[]{0, 5}, decoded.returnData);
    }

    @Test
    void rpcPacket() throws ReflectiveOperationException {
        var decoded = roundTrip(PacketRPCPacket.CODEC, PacketRPCPacket.of("rpcPacketTest", new byte[]{6, 6, 6}));
        assertEquals("rpcPacketTest", field(decoded, "packetID"));
        assertArrayEquals(new byte[]{6, 6, 6}, (byte[]) field(decoded, "data"));
    }

    @Test
    void rpcBlockEntity() throws ReflectiveOperationException {
        assumeTrue(bootstrapped, () -> "Bootstrap failed: " + bootstrapFailure);
        var pos = new BlockPos(3, -64, 12);
        var decoded = roundTrip(PacketRPCBlockEntity.CODEC, new PacketRPCBlockEntity(BlockEntityType.CHEST, pos, new byte[]{1}));
        assertEquals(pos, field(decoded, "pos"));
        assertSame(BlockEntityType.CHEST, field(decoded, "blockEntityType"));
        assertArrayEquals(new byte[]{1}, (byte[]) field(decoded, "data"));
    }

    @Test
    void autoSyncBlockEntity() throws ReflectiveOperationException {
        assumeTrue(bootstrapped, () -> "Bootstrap failed: " + bootstrapFailure);
        var constructor = SPacketAutoSyncBlockEntity.class.getDeclaredConstructor(BlockEntityType.class, BlockPos.class, BitSet.class, byte[].class, CompoundTag.class);
        constructor.setAccessible(true);
        var changed = new BitSet();
        changed.set(1);
        changed.set(9);
        var extra = new CompoundTag();
        extra.putString("note", "x");
        var packet = constructor.newInstance(BlockEntityType.FURNACE, new BlockPos(1, 2, 3), changed, new byte[]{4, 5}, extra);
        var decoded = roundTrip(SPacketAutoSyncBlockEntity.CODEC, packet);
        assertEquals(packet.getPos(), decoded.getPos());
        assertSame(packet.getBlockEntityType(), decoded.getBlockEntityType());
        assertEquals(packet.getChanged(), decoded.getChanged());
        assertArrayEquals(packet.getData(), decoded.getData());
        assertEquals(packet.getExtra(), decoded.getExtra());
    }

    @Test
    void autoSyncBlockEntities() {
        assumeTrue(bootstrapped, () -> "Bootstrap failed: " + bootstrapFailure);
        var changed = new BitSet();
        changed.set(0);
        var extra = new CompoundTag();
        extra.putInt("n", 1);
        var first = new SPacketAutoSyncBlockEntities.Entry(new BlockPos(0, 0, 0), BlockEntityType.CHEST, changed, new byte[]{1, 2}, extra);
        var second = new SPacketAutoSyncBlockEntities.Entry(new BlockPos(5, 6, 7), BlockEntityType.FURNACE, new BitSet(), new byte[0], new CompoundTag());
        var expectedEntries = List.of(first, second);
        var decoded = roundTrip(SPacketAutoSyncBlockEntities.CODEC, new SPacketAutoSyncBlockEntities(expectedEntries));
        assertEquals(2, decoded.entries().size());
        for (int i = 0; i < 2; i++) {
            var expected = expectedEntries.get(i);
            var actual = decoded.entries().get(i);
            assertEquals(expected.pos(), actual.pos());
            assertSame(expected.type(), actual.type());
            assertEquals(expected.changed(), actual.changed());
            assertArrayEquals(expected.data(), actual.data());
            assertEquals(expected.extra(), actual.extra());
        }
    }
}
