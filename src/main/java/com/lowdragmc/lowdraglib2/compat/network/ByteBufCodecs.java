package com.lowdragmc.lowdraglib2.compat.network;

import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ByteBufCodecs {
    private ByteBufCodecs() {
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<>() {
        @Override
        public Boolean decode(ByteBuf buf) {
            return buf.readBoolean();
        }

        @Override
        public void encode(ByteBuf buf, Boolean value) {
            buf.writeBoolean(value);
        }
    };

    public static final StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<>() {
        @Override
        public Byte decode(ByteBuf buf) {
            return buf.readByte();
        }

        @Override
        public void encode(ByteBuf buf, Byte value) {
            buf.writeByte(value);
        }
    };

    public static final StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<>() {
        @Override
        public Short decode(ByteBuf buf) {
            return buf.readShort();
        }

        @Override
        public void encode(ByteBuf buf, Short value) {
            buf.writeShort(value);
        }
    };

    public static final StreamCodec<ByteBuf, Integer> INT = new StreamCodec<>() {
        @Override
        public Integer decode(ByteBuf buf) {
            return buf.readInt();
        }

        @Override
        public void encode(ByteBuf buf, Integer value) {
            buf.writeInt(value);
        }
    };

    public static final StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<>() {
        @Override
        public Integer decode(ByteBuf buf) {
            return VarInt.read(buf);
        }

        @Override
        public void encode(ByteBuf buf, Integer value) {
            VarInt.write(buf, value);
        }
    };

    public static final StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<>() {
        @Override
        public Long decode(ByteBuf buf) {
            return new FriendlyByteBuf(buf).readVarLong();
        }

        @Override
        public void encode(ByteBuf buf, Long value) {
            new FriendlyByteBuf(buf).writeVarLong(value);
        }
    };

    public static final StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<>() {
        @Override
        public Float decode(ByteBuf buf) {
            return buf.readFloat();
        }

        @Override
        public void encode(ByteBuf buf, Float value) {
            buf.writeFloat(value);
        }
    };

    public static final StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<>() {
        @Override
        public Double decode(ByteBuf buf) {
            return buf.readDouble();
        }

        @Override
        public void encode(ByteBuf buf, Double value) {
            buf.writeDouble(value);
        }
    };

    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = new StreamCodec<>() {
        @Override
        public String decode(ByteBuf buf) {
            return new FriendlyByteBuf(buf).readUtf();
        }

        @Override
        public void encode(ByteBuf buf, String value) {
            new FriendlyByteBuf(buf).writeUtf(value);
        }
    };

    public static final StreamCodec<ByteBuf, Tag> TRUSTED_TAG = new StreamCodec<>() {
        @Override
        public Tag decode(ByteBuf buf) {
            return readTag(buf, NbtAccounter.UNLIMITED);
        }

        @Override
        public void encode(ByteBuf buf, Tag value) {
            writeTag(buf, value);
        }
    };

    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = new StreamCodec<>() {
        @Override
        public CompoundTag decode(ByteBuf buf) {
            var tag = readTag(buf);
            if (tag instanceof CompoundTag compound) {
                return compound;
            }
            throw new DecoderException("Not a compound tag: " + tag);
        }

        @Override
        public void encode(ByteBuf buf, CompoundTag value) {
            writeTag(buf, value);
        }
    };

    public static final StreamCodec<ByteBuf, Vector3f> VECTOR3F = new StreamCodec<>() {
        @Override
        public Vector3f decode(ByteBuf buf) {
            return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void encode(ByteBuf buf, Vector3f value) {
            buf.writeFloat(value.x());
            buf.writeFloat(value.y());
            buf.writeFloat(value.z());
        }
    };

    public static final StreamCodec<ByteBuf, Quaternionf> QUATERNIONF = new StreamCodec<>() {
        @Override
        public Quaternionf decode(ByteBuf buf) {
            return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void encode(ByteBuf buf, Quaternionf value) {
            buf.writeFloat(value.x());
            buf.writeFloat(value.y());
            buf.writeFloat(value.z());
            buf.writeFloat(value.w());
        }
    };

    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = new StreamCodec<>() {
        @Override
        public ResourceLocation decode(ByteBuf buf) {
            return new FriendlyByteBuf(buf).readResourceLocation();
        }

        @Override
        public void encode(ByteBuf buf, ResourceLocation value) {
            new FriendlyByteBuf(buf).writeResourceLocation(value);
        }
    };

    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = new StreamCodec<>() {
        @Override
        public BlockPos decode(ByteBuf buf) {
            return new FriendlyByteBuf(buf).readBlockPos();
        }

        @Override
        public void encode(ByteBuf buf, BlockPos value) {
            new FriendlyByteBuf(buf).writeBlockPos(value);
        }
    };

    public static final StreamCodec<ByteBuf, UUID> UUID = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            return new FriendlyByteBuf(buf).readUUID();
        }

        @Override
        public void encode(ByteBuf buf, UUID value) {
            new FriendlyByteBuf(buf).writeUUID(value);
        }
    };

    public static final StreamCodec<FriendlyByteBuf, Component> COMPONENT = new StreamCodec<>() {
        @Override
        public Component decode(FriendlyByteBuf buf) {
            return buf.readComponent();
        }

        @Override
        public void encode(FriendlyByteBuf buf, Component value) {
            buf.writeComponent(value);
        }
    };

    public static final StreamCodec<FriendlyByteBuf, ItemStack> OPTIONAL_ITEM_STACK = new StreamCodec<>() {
        @Override
        public ItemStack decode(FriendlyByteBuf buf) {
            var stack = buf.readItem();
            if (!stack.isEmpty()) stack.setCount(buf.readVarInt());
            return stack;
        }

        @Override
        public void encode(FriendlyByteBuf buf, ItemStack value) {
            buf.writeItem(value);
            if (!value.isEmpty()) buf.writeVarInt(value.getCount());
        }
    };

    public static final StreamCodec<FriendlyByteBuf, FluidStack> OPTIONAL_FLUID_STACK = new StreamCodec<>() {
        @Override
        public FluidStack decode(FriendlyByteBuf buf) {
            return buf.readFluidStack();
        }

        @Override
        public void encode(FriendlyByteBuf buf, FluidStack value) {
            buf.writeFluidStack(value);
        }
    };

    public static final StreamCodec<FriendlyByteBuf, Ingredient> INGREDIENT_CONTENTS = new StreamCodec<>() {
        @Override
        public Ingredient decode(FriendlyByteBuf buf) {
            return Ingredient.fromNetwork(buf);
        }

        @Override
        public void encode(FriendlyByteBuf buf, Ingredient value) {
            value.toNetwork(buf);
        }
    };

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> new StreamCodec<>() {
            @Override
            public List<V> decode(B buf) {
                int size = VarInt.read(buf);
                var list = new ArrayList<V>(Math.min(size, 65536));
                for (int i = 0; i < size; i++) {
                    list.add(codec.decode(buf));
                }
                return list;
            }

            @Override
            public void encode(B buf, List<V> value) {
                VarInt.write(buf, value.size());
                for (var element : value) {
                    codec.encode(buf, element);
                }
            }
        };
    }

    public static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(ByteBuf buf) {
                var tag = readTag(buf);
                return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, message -> {
                    throw new DecoderException("Failed to decode: " + message + " " + tag);
                });
            }

            @Override
            public void encode(ByteBuf buf, T value) {
                writeTag(buf, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, message -> {
                    throw new EncoderException("Failed to encode: " + message + " " + value);
                }));
            }
        };
    }

    public static <T> StreamCodec<FriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(FriendlyByteBuf buf) {
                var tag = readTag(buf);
                var ops = RegistryOps.create(NbtOps.INSTANCE, Platform.getFrozenRegistry());
                return codec.parse(ops, tag).getOrThrow(false, message -> {
                    throw new DecoderException("Failed to decode: " + message + " " + tag);
                });
            }

            @Override
            public void encode(FriendlyByteBuf buf, T value) {
                var ops = RegistryOps.create(NbtOps.INSTANCE, Platform.getFrozenRegistry());
                writeTag(buf, codec.encodeStart(ops, value).getOrThrow(false, message -> {
                    throw new EncoderException("Failed to encode: " + message + " " + value);
                }));
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> StreamCodec<FriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> key) {
        return new StreamCodec<>() {
            private Registry<T> resolve() {
                var builtIn = BuiltInRegistries.REGISTRY.get(key.location());
                if (builtIn != null) {
                    return (Registry<T>) builtIn;
                }
                return Platform.getFrozenRegistry().registryOrThrow(key);
            }

            @Override
            public T decode(FriendlyByteBuf buf) {
                int id = buf.readVarInt();
                var value = resolve().byId(id);
                if (value == null) {
                    throw new DecoderException("Received unknown id " + id + " for registry " + key.location());
                }
                return value;
            }

            @Override
            public void encode(FriendlyByteBuf buf, T value) {
                int id = resolve().getId(value);
                if (id == -1) {
                    throw new EncoderException("Can't find id for '" + value + "' in registry " + key.location());
                }
                buf.writeVarInt(id);
            }
        };
    }

    private static Tag readTag(ByteBuf buf) {
        return readTag(buf, new NbtAccounter(2097152L));
    }

    private static Tag readTag(ByteBuf buf, NbtAccounter accounter) {
        try {
            int id = buf.readByte();
            return TagTypes.getType(id).load(new ByteBufInputStream(buf), 0, accounter);
        } catch (IOException e) {
            throw new DecoderException(e);
        }
    }

    private static void writeTag(ByteBuf buf, Tag tag) {
        try {
            buf.writeByte(tag.getId());
            tag.write(new ByteBufOutputStream(buf));
        } catch (IOException e) {
            throw new EncoderException(e);
        }
    }
}
