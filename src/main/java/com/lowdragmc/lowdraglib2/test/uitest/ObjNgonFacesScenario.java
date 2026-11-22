package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.geometry.StandaloneGeometryBakingContext;
import net.minecraftforge.client.model.obj.ObjLoader;
import net.minecraftforge.client.model.obj.ObjModel;
import net.minecraftforge.client.model.renderable.CompositeRenderable;

import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@LDLRegisterClient(name = "obj_ngon_faces", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ObjNgonFacesScenario implements UIScenario {

    private static final ResourceLocation OBJ = LDLib2.id("models/test/ngon.obj");
    private static final Map<String, Integer> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("quad", 1);
        EXPECTED.put("pentagon", 2);
        EXPECTED.put("hexagon", 2);
        EXPECTED.put("heptagon", 3);
        EXPECTED.put("octagon", 3);
    }

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("render", "mixin");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("bake every shape through addQuads and ModelMesh.bake", this::run);
        for (var shape : EXPECTED.keySet()) {
            s.checkEquals(shape + " addQuads quad count", EXPECTED.get(shape), ctx -> ctx.<Integer>get("a.count." + shape))
                    .checkEquals(shape + " ModelMesh.bake quad count", EXPECTED.get(shape), ctx -> ctx.<Integer>get("b.count." + shape))
                    .check(shape + " addQuads covers every OBJ vertex", ctx -> ctx.<Boolean>get("a.cover." + shape))
                    .check(shape + " ModelMesh.bake covers every OBJ vertex", ctx -> ctx.<Boolean>get("b.cover." + shape));
        }
    }

    private void run(TestContext ctx) {
        try {
            var polygons = readPolygons();
            var model = ObjLoader.INSTANCE.loadModel(new ObjModel.ModelSettings(OBJ, false, true, false, false, null));
            var atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            for (var shape : EXPECTED.keySet()) {
                var visible = new Object2BooleanOpenHashMap<String>();
                for (var name : EXPECTED.keySet()) visible.put(name, name.equals(shape));
                var context = StandaloneGeometryBakingContext.builder().withVisibleComponents(visible)
                        .build(StandaloneGeometryBakingContext.LOCATION);

                BakedModel baked = model.bake(context, null, material -> atlas.getSprite(material.texture()),
                        BlockModelRotation.X0_Y0, ItemOverrides.EMPTY, OBJ);
                var aQuads = new ArrayList<>(baked.getQuads(null, null, RandomSource.create()));
                for (var dir : Direction.values()) aQuads.addAll(baked.getQuads(null, dir, RandomSource.create()));

                var renderable = model.bakeRenderable(context);
                var bQuads = new ArrayList<BakedQuad>();
                collect(renderable, shape, bQuads);

                var expectedPositions = polygons.get(shape);
                var aPositions = positions(aQuads);
                var bPositions = positions(bQuads);
                int faceVertices = expectedPositions.size();

                ctx.put("a.count." + shape, aQuads.size());
                ctx.put("b.count." + shape, bQuads.size());
                ctx.put("a.cover." + shape, aPositions.equals(expectedPositions));
                ctx.put("b.cover." + shape, bPositions.equals(expectedPositions));
                ctx.log("OBJ_NGON " + shape + " vertices=" + faceVertices
                        + " | addQuads: with mixin=" + aQuads.size() + " covered=" + aPositions.size() + "/" + faceVertices
                        + " | bake: with mixin=" + bQuads.size() + " covered=" + bPositions.size() + "/" + faceVertices
                        + " | without mixin: quads=1 covered=" + Math.min(4, faceVertices) + "/" + faceVertices);
            }
        } catch (Exception e) {
            throw new IllegalStateException("obj n-gon bake failed", e);
        }
    }

    private static Map<String, Set<String>> readPolygons() throws Exception {
        var resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(OBJ);
        var vertices = new ArrayList<float[]>();
        var polygons = new LinkedHashMap<String, Set<String>>();
        Set<String> current = null;
        try (BufferedReader reader = resource.openAsReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                var parts = line.trim().split("\\s+");
                switch (parts[0]) {
                    case "v" -> vertices.add(new float[]{Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                    case "o" -> {
                        current = new LinkedHashSet<>();
                        polygons.put(parts[1], current);
                    }
                    case "f" -> {
                        for (int i = 1; i < parts.length; i++) {
                            var v = vertices.get(Integer.parseInt(parts[i].split("/")[0]) - 1);
                            current.add(key(v[0], v[1], v[2]));
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return polygons;
    }

    private static Set<String> positions(List<BakedQuad> quads) {
        var set = new LinkedHashSet<String>();
        for (var quad : quads) {
            var data = quad.getVertices();
            for (int i = 0; i < 4; i++) {
                set.add(key(Float.intBitsToFloat(data[i * 8]), Float.intBitsToFloat(data[i * 8 + 1]), Float.intBitsToFloat(data[i * 8 + 2])));
            }
        }
        return set;
    }

    private static String key(float x, float y, float z) {
        return Math.round(x * 1000) + "," + Math.round(y * 1000) + "," + Math.round(z * 1000);
    }

    @SuppressWarnings("unchecked")
    private static void collect(CompositeRenderable renderable, String shape, List<BakedQuad> out) throws Exception {
        var components = (List<Object>) field(CompositeRenderable.class, "components").get(renderable);
        for (var component : components) {
            if (shape.equals(field(component.getClass(), "name").get(component))) collectComponent(component, out);
        }
    }

    @SuppressWarnings("unchecked")
    private static void collectComponent(Object component, List<BakedQuad> out) throws Exception {
        for (var mesh : (List<Object>) field(component.getClass(), "meshes").get(component)) {
            out.addAll((List<BakedQuad>) field(mesh.getClass(), "quads").get(mesh));
        }
        for (var child : (List<Object>) field(component.getClass(), "children").get(component)) {
            collectComponent(child, out);
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
