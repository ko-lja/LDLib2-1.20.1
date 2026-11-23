package com.lowdragmc.lowdraglib2.client.model.custommodel;

import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import com.lowdragmc.lowdraglib2.client.model.bakedpipeline.Quad;
import com.lowdragmc.lowdraglib2.client.model.bakedpipeline.Submap;
import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.lowdragmc.lowdraglib2.client.model.forge.LDLRendererModel.RendererBakedModel.*;

/**
 * Used to bake the model with emissive effect. or multi-layer making the top layer emissive.
 */
@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomBakedModel implements BakedModel {
    @Getter
    protected final BakedModel parent;
    protected final ConcurrentMap<Direction, ConcurrentMap<Connections, List<BakedQuad>>> sideCache;
    protected final ConcurrentMap<Connections, List<BakedQuad>> noSideCache;

    public CustomBakedModel(BakedModel parent) {
        this.parent = parent;
        this.sideCache = new ConcurrentHashMap<>();
        this.noSideCache = new ConcurrentHashMap<>();
    }

    @Override
    @Nonnull
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return parent.getQuads(state, side, rand);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                             ModelData modelData, @Nullable RenderType renderType) {
        BlockAndTintGetter level = modelData.get(WORLD);
        BlockPos pos = modelData.get(POS);
        ModelData parentModelData = modelData.get(MODEL_DATA);
        if (parentModelData == null) parentModelData = ModelData.EMPTY;

        if (level != null && pos != null && state != null) {
            return getCustomQuads(level, pos, state, side, rand, parentModelData, renderType);
        } else {
            // return the parent's quads (instead of nothing), like CustomBakedModel does in the default getQuads method
            return parent.getQuads(state, side, rand, parentModelData, renderType);
        }
    }

    public @NotNull List<BakedQuad> getCustomQuads(BlockAndTintGetter level, BlockPos pos, @NotNull BlockState state,
                                                   @Nullable Direction side, RandomSource rand,
                                                   ModelData parentModelData, @Nullable RenderType renderType) {
        var connections = Connections.checkConnections(level, pos, state, side);
        // Don't cache the quads if we're rendering for a specific render type or if the parent model has set any model data
        // as that might change the model and caching anything will likely result in broken models.
        if (renderType != null || !parentModelData.getProperties().isEmpty()) {
            return buildCustomQuads(connections, parent.getQuads(state, side, rand, parentModelData, renderType), 0.0f);
        }

        return getCustomQuads(level, pos, state, side, rand);
    }


    @Nonnull
    public List<BakedQuad> getCustomQuads(BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state, @Nullable Direction side, RandomSource rand) {
        var connections = Connections.checkConnections(level, pos, state, side);
        if (side == null) {
            return noSideCache.computeIfAbsent(connections, key -> buildCustomQuads(key, parent.getQuads(state, null, rand), 0.0f));
        }
        return sideCache
            .computeIfAbsent(side, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(connections, key -> buildCustomQuads(key, parent.getQuads(state, side, rand), 0.0f));
    }

    public static List<BakedQuad> reBakeCustomQuads(List<BakedQuad> quads, BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state, @Nullable Direction side, float offset) {
        return buildCustomQuads(Connections.checkConnections(level, pos, state, side), quads, offset);
    }

    public static List<BakedQuad> buildCustomQuads(Connections connections, List<BakedQuad> base, float offset) {
        List<BakedQuad> result = new LinkedList<>();
        for (BakedQuad bakedQuad : base) {
            var section = LDLMetadataSection.getMetadata(bakedQuad.getSprite());
            TextureAtlasSprite connection = section.connection == null ? null : ModelFactory.getBlockSprite(section.connection);
            if (connection == null) {
                result.add(makeQuad(bakedQuad, section, offset).rebake());
                continue;
            }

            Quad quad = makeQuad(bakedQuad, section, offset).derotate();
            Quad[] quads = quad.subdivide(4);

            int[] ctm = connections.getSubmapIndices();

            for (int j = 0; j < quads.length; j++) {
                Quad q = quads[j];
                if (q == null) continue;
                int ctmid = q.getUvs().normalize().getQuadrant();
                quads[j] = q.grow().transformUVs(ctm[ctmid] > 15 ? bakedQuad.getSprite() : connection, Submap.uvs[ctm[ctmid]]);
            }

            for (Quad quad1 : quads) {
                if (quad1 == null) continue;
                BakedQuad rebake = quad1.rebake();
                result.add(rebake);
            }
        }
        return result;
    }

    protected static Quad makeQuad(BakedQuad bq, LDLMetadataSection section, float offset) {
        Quad q = Quad.from(bq, offset);
        if (section.emissive) {
            q = q.setLight(15, 15);
        }
        return q;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return parent.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return parent.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return parent.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return parent.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return parent.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return parent.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return parent.getOverrides();
    }

    @Override
    public @NotNull ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return modelData.derive()
            .with(WORLD, level)
            .with(POS, pos)
            .with(MODEL_DATA, parent.getModelData(level, pos, state, modelData))
            .build();
    }
}
