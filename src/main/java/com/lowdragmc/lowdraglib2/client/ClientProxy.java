package com.lowdragmc.lowdraglib2.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.font.LDFontManager;
import com.lowdragmc.lowdraglib2.client.font.LDFontStatsOverlay;
import com.lowdragmc.lowdraglib2.client.model.custommodel.CustomBakedModel;
import com.lowdragmc.lowdraglib2.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib2.client.model.forge.LDLRendererModel;
import com.lowdragmc.lowdraglib2.client.renderer.ATESRRendererProvider;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.core.mixins.ParticleEngineAccessor;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ModelBakeryAccessor;
import com.lowdragmc.lowdraglib2.editor.resource.EditorResourceEvent;
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib2.editor.resource.PackResourceManager;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.factory.LDMenuTypes;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.OreSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUIClientElementComponent;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUITooltipComponent;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.LDKJSMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.client.gui.screens.MenuScreens;

import com.lowdragmc.lowdraglib2.test.TestBlock;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.*;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class ClientProxy {

    public static final Multimap<ResourceLocation, Material> SCRAPED_TEXTURES = HashMultimap.create();
    public static final Object2BooleanMap<ResourceLocation> WRAPPED_MODELS = new Object2BooleanLinkedOpenHashMap<>();

    public ClientProxy(FMLJavaModLoadingContext ctx, IEventBus eventBus) {
        eventBus.register(this);
        ctx.registerConfig(ModConfig.Type.CLIENT, LDLibClientConfig.SPEC);
        ctx.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new LDLibConfigScreen(parent)));
    }

    @SubscribeEvent
    public void onRegisterClientTooltipComponentFactoriesEvent(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ModularUITooltipComponent.class, ModularUIClientElementComponent::new);
    }

    @SubscribeEvent
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (Platform.isDevEnv()) {
            event.registerBlockEntityRenderer(CommonProxy.TEST_BE_TYPE.get(), ATESRRendererProvider::new);
        }
        event.registerBlockEntityRenderer(CommonProxy.RENDERER_BE_TYPE.get(), ATESRRendererProvider::new);
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            LDLibShaders.init();
            MenuScreens.register(LDMenuTypes.PLAYER_UI.get(), ModularUIContainerScreen::new);
            MenuScreens.register(LDMenuTypes.HELD_ITEM_UI.get(), ModularUIContainerScreen::new);
            MenuScreens.register(LDMenuTypes.BLOCK_UI.get(), ModularUIContainerScreen::new);
            if (LDLib2.isKubejsLoaded()) {
                LDKJSMenuTypes.onRegisterMenuScreensEvent();
            }

            //noinspection removal
            ItemBlockRenderTypes.setRenderLayer(RendererBlock.BLOCK, RenderType.translucent());
            if (Platform.isDevEnv()) {
                //noinspection removal
                ItemBlockRenderTypes.setRenderLayer(TestBlock.BLOCK, RenderType.cutoutMipped());
            }
        });
    }

    @SubscribeEvent
    public void modelRegistry(final ModelEvent.RegisterGeometryLoaders e) {
        e.register(LDLib2.id("renderer").getPath(), LDLRendererModel.Loader.INSTANCE);
    }

    @SubscribeEvent
    public void modelBake(final ModelEvent.ModifyBakingResult event) {
        ModelBakery modelBakery = event.getModelBakery();
        for (Map.Entry<ResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            ResourceLocation rl = entry.getKey();
            UnbakedModel rootModel = ((ModelBakeryAccessor)modelBakery).getTopLevelModels().get(rl);
            if (rootModel != null) {
                BakedModel baked = entry.getValue();
                if (baked instanceof LDLRendererModel) {
                    continue;
                }
                if (baked.isCustomRenderer()) { // Nothing we can add to builtin models
                    continue;
                }
                Deque<ResourceLocation> dependencies = new ArrayDeque<>();
                Set<ResourceLocation> seenModels = new HashSet<>();
                dependencies.push(rl);
                seenModels.add(rl);
                boolean shouldWrap = ClientProxy.WRAPPED_MODELS.getOrDefault(rl, false);
                // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
                while (!shouldWrap && !dependencies.isEmpty()) {
                    ResourceLocation dep = dependencies.pop();
                    UnbakedModel model;
                    try {
                        model = dep == rl ? rootModel : modelBakery.getModel(dep);
                    } catch (Exception e) {
                        continue;
                    }

                    try {
                        Set<Material> textures = new HashSet<>(ClientProxy.SCRAPED_TEXTURES.get(dep));
                        for (Material tex : textures) {
                            // Cache all dependent texture metadata
                            // At least one texture has CTM metadata, so we should wrap this model
                            if (!LDLMetadataSection.getMetadata(LDLMetadataSection.spriteToAbsolute(tex.texture())).isMissing()) { // TODO lazy
                                shouldWrap = true;
                                break;
                            }
                        }
                        if (!shouldWrap) {
                            for (ResourceLocation newDep : model.getDependencies()) {
                                if (seenModels.add(newDep)) {
                                    dependencies.push(newDep);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LDLib2.LOGGER.error("Error loading model dependency {} for model {}. Skipping...", dep, rl, e);
                    }
                }
                ClientProxy.WRAPPED_MODELS.put(rl, shouldWrap);
                if (shouldWrap) {
                    entry.setValue(new CustomBakedModel(baked));
                }
            }
        }
        SCRAPED_TEXTURES.clear();
        WRAPPED_MODELS.clear();

    }

    @SubscribeEvent
    public void shaderRegistry(RegisterShadersEvent event) {
        LDLibShaders.registerShaders(event);
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onLoadBuiltinEditorResource(EditorResourceEvent.LoadBuiltin event) {
        if (event.resourceInstance.resource == TexturesResource.INSTANCE) {
            var instance = (ResourceInstance<IGuiTexture>) event.resourceInstance;
            Sprites.init(instance);
            MCSprites.init(instance);
            OreSprites.init(instance);
        }
    }

    /**
     * The client config decides how glyphs are baked, so a change to it invalidates every atlas.
     * <p>
     * Only {@code Reloading} matters: at {@code Loading} time nothing has been baked yet. The rebuild is handed
     * to the client thread because this event is documented to fire on any thread and freeing a texture is not
     * thread safe.
     */
    @SubscribeEvent
    public void onConfigReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == LDLibClientConfig.SPEC) {
            Minecraft.getInstance().execute(LDFontManager.INSTANCE::invalidate);
        }
    }

    /**
     * TEMPORARY: development readout, see {@link LDFontStatsOverlay}. Registered above everything so it is not
     * hidden by the rest of the HUD.
     */
    @SubscribeEvent
    public void registerFontStatsOverlay(RegisterGuiOverlaysEvent event) {
        if (Platform.isDevEnv()) {
            event.registerAboveAll(LDLib2.id("font_stats").getPath(), LDFontStatsOverlay.INSTANCE);
        }
    }

    @SubscribeEvent
    public void onRegisterClientReloadListenersEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(PackResourceManager.INSTANCE);
        event.registerReloadListener(StylesheetManager.INSTANCE);
        event.registerReloadListener(LDFontManager.INSTANCE);
    }

    @SubscribeEvent
    public void registerModels(ModelEvent.RegisterAdditional event) {
        // load all models under the ldlib folder
        for (var entry : Minecraft.getInstance().getResourceManager().listResources("models",
                id -> id.getNamespace().equals(LDLib2.MOD_ID) && id.getPath().endsWith(".json")).entrySet()) {
            if (entry.getValue().sourcePackId().equals(LDLib2.MOD_ID)) {
                var modelLocation = ResourceLocation.fromNamespaceAndPath(
                        entry.getKey().getNamespace(),
                        entry.getKey().getPath()
                                .replace("models/", "")
                                .replace(".json", ""));
                event.register(modelLocation);
            }
        }
        IRendererResource.INSTANCE.onAdditionalModel(event::register);
        for (IRenderer renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onAdditionalModel(event::register);
        }
    }

    public static ParticleProvider getProvider(ParticleType<?> type) {
        if (Minecraft.getInstance().particleEngine instanceof ParticleEngineAccessor accessor) {
            return accessor.getProviders().get(ForgeRegistries.PARTICLE_TYPES.getKey(type));
        }
        return null;
    }

    public static void textureScraped(ResourceLocation modelLocation, Material material) {
        SCRAPED_TEXTURES.put(modelLocation, material);
    }

}
