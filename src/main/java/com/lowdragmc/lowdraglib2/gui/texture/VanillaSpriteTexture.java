package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.GsonHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The sprite ResourceLocation should point to a registered vanilla sprite
 * (e.g. from a GUI atlas), not a raw texture file.
 */
@KJSBindings
@LDLRegisterClient(name = "vanilla_sprite_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class VanillaSpriteTexture extends TransformTexture {
    private static final Map<ResourceLocation, SpriteData> SPRITE_CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> MISSING_SPRITES = ConcurrentHashMap.newKeySet();

    private record SpriteData(ResourceLocation texture, String type, int width, int height,
                              int left, int top, int right, int bottom) {
    }


    @Configurable(name = "ldlib.gui.editor.name.resource")
    @ConfigSearch(searchConfiguratorMethod = "searchSprites")
    @Getter
    @Setter
    private ResourceLocation sprite = ResourceLocation.withDefaultNamespace("toast/recipe_book");

    @Configurable
    @ConfigColor
    @Getter
    @Setter
    private int color = -1;

    public VanillaSpriteTexture() {
    }

    public VanillaSpriteTexture(ResourceLocation sprite) {
        this.sprite = sprite;
    }

    @HideFromJS
    public static VanillaSpriteTexture of(ResourceLocation sprite) {
        return new VanillaSpriteTexture(sprite);
    }

    @HideFromJS
    public static VanillaSpriteTexture of(String sprite) {
        return of(ResourceLocation.parse(sprite));
    }

    @RemapForJS("of")
    public static VanillaSpriteTexture kjs$of(ResourceLocation sprite) {
        return of(sprite);
    }

    @Override
    public VanillaSpriteTexture copy() {
        var copied = new VanillaSpriteTexture(sprite);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        if (sprite == null || width <= 0 || height <= 0) {
            return;
        }
        var data = loadSpriteData(sprite);
        if (data == null) {
            return;
        }
        switch (data.type()) {
            case "tile" -> blitTiledSpriteFloat(graphics, data, x, y, width, height,
                    0, 0, data.width(), data.height(), data.width(), data.height());
            case "nine_slice" -> blitNineSlicedSpriteFloat(graphics, data, x, y, width, height);
            default -> blitSpriteFloat(graphics, data, x, y, width, height);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private static SpriteData loadSpriteData(ResourceLocation sprite) {
        var cached = SPRITE_CACHE.get(sprite);
        if (cached != null) {
            return cached;
        }
        if (MISSING_SPRITES.contains(sprite)) {
            return null;
        }
        var manager = Minecraft.getInstance().getResourceManager();
        var texture = new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
        var resource = manager.getResource(texture);
        if (resource.isEmpty()) {
            MISSING_SPRITES.add(sprite);
            return null;
        }
        String type = "stretch";
        int width = 0, height = 0, left = 0, top = 0, right = 0, bottom = 0;
        var meta = manager.getResource(new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta"));
        if (meta.isPresent()) {
            try (var reader = meta.get().openAsReader()) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                var scaling = GsonHelper.getAsJsonObject(GsonHelper.getAsJsonObject(root, "gui", new JsonObject()), "scaling", new JsonObject());
                type = GsonHelper.getAsString(scaling, "type", "stretch");
                width = GsonHelper.getAsInt(scaling, "width", 0);
                height = GsonHelper.getAsInt(scaling, "height", 0);
                JsonElement border = scaling.get("border");
                if (border != null && border.isJsonPrimitive()) {
                    left = top = right = bottom = border.getAsInt();
                } else if (border != null && border.isJsonObject()) {
                    var borderObject = border.getAsJsonObject();
                    left = GsonHelper.getAsInt(borderObject, "left", 0);
                    top = GsonHelper.getAsInt(borderObject, "top", 0);
                    right = GsonHelper.getAsInt(borderObject, "right", 0);
                    bottom = GsonHelper.getAsInt(borderObject, "bottom", 0);
                }
            } catch (Exception ignored) {
                type = "stretch";
            }
        }
        if (width <= 0 || height <= 0) {
            try (var stream = resource.get().open(); var image = NativeImage.read(stream)) {
                width = image.getWidth();
                height = image.getHeight();
            } catch (IOException e) {
                MISSING_SPRITES.add(sprite);
                return null;
            }
        }
        var data = new SpriteData(texture, type, width, height, left, top, right, bottom);
        SPRITE_CACHE.put(sprite, data);
        return data;
    }

    @OnlyIn(Dist.CLIENT)
    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchSprites() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            @NotNull
            public ResourceLocation defaultValue() {
                return ResourceLocation.withDefaultNamespace("toast/recipe_book");
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var lowerWord = word.toLowerCase();
                var prefix = "textures/gui/sprites/";
                var resources = Minecraft.getInstance().getResourceManager().listResources("textures/gui/sprites", id -> id.getPath().endsWith(".png"));
                for (var id : resources.keySet()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    var path = id.getPath();
                    var key = new ResourceLocation(id.getNamespace(), path.substring(prefix.length(), path.length() - ".png".length()));
                    if (key.toString().toLowerCase().contains(lowerWord)) {
                        searchHandler.acceptResult(key);
                    }
                }
            }

            @Override
            @NotNull
            public String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            @Override
            public UIElementProvider<ResourceLocation> candidateUIProvider() {
                return UIElementProvider.iconText(VanillaSpriteTexture::of, res -> Component.literal(res.toString()));
            }
        };
    }

    /**
     * Blit a full sprite with float coordinates (stretch mode).
     */
    @OnlyIn(Dist.CLIENT)
    private void blitSpriteFloat(GuiGraphics graphics, SpriteData atlasSprite, float x, float y, float width, float height) {
        innerBlitFloat(graphics, atlasSprite,
                x, x + width, y, y + height,
                0, 1, 0, 1);
    }

    /**
     * Blit a sub-region of a sprite with float coordinates.
     */
    @OnlyIn(Dist.CLIENT)
    private void blitSubSpriteFloat(GuiGraphics graphics, SpriteData sprite,
                                    int textureWidth, int textureHeight,
                                    int uPosition, int vPosition,
                                    float x, float y, float uWidth, float vHeight) {
        if (uWidth == 0 || vHeight == 0) return;
        innerBlitFloat(graphics, sprite,
                x, x + uWidth, y, y + vHeight,
                (float) uPosition / textureWidth,
                (uPosition + uWidth) / textureWidth,
                (float) vPosition / textureHeight,
                (vPosition + vHeight) / textureHeight);
    }

    /**
     * Core float quad rendering.
     */
    @OnlyIn(Dist.CLIENT)
    private void innerBlitFloat(GuiGraphics graphics, SpriteData sprite,
                                float x1, float x2, float y1, float y2,
                                float u0, float u1, float v0, float v1) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        var matrix = graphics.pose().last().pose();
        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.guiTexture(sprite.texture()));
        buffer.vertex(matrix, x1, y1, 0).uv(u0, v0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, 0).uv(u0, v1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, 0).uv(u1, v1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, 0).uv(u1, v0).color(r, g, b, a).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    private void blitTiledSpriteFloat(GuiGraphics graphics, SpriteData sprite,
                                      float x, float y, float width, float height,
                                      int uPosition, int vPosition,
                                      int spriteWidth, int spriteHeight,
                                      int nineSliceWidth, int nineSliceHeight) {
        if (width <= 0 || height <= 0 || spriteWidth <= 0 || spriteHeight <= 0) return;
        for (float i = 0; i < width; i += spriteWidth) {
            float j = Math.min(spriteWidth, width - i);
            for (float k = 0; k < height; k += spriteHeight) {
                float l = Math.min(spriteHeight, height - k);
                blitSubSpriteFloat(graphics, sprite, nineSliceWidth, nineSliceHeight,
                        uPosition, vPosition, x + i, y + k, j, l);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void blitNineSlicedSpriteFloat(GuiGraphics graphics, SpriteData sprite,
                                           float x, float y, float width, float height) {
        float left = Math.min(sprite.left(), width / 2);
        float right = Math.min(sprite.right(), width / 2);
        float top = Math.min(sprite.top(), height / 2);
        float bottom = Math.min(sprite.bottom(), height / 2);
        int nsW = sprite.width();
        int nsH = sprite.height();

        if (width == nsW && height == nsH) {
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, 0, 0, x, y, width, height);
            return;
        }

        float centerW = width - left - right;
        float centerH = height - top - bottom;
        int bLeft = sprite.left();
        int bRight = sprite.right();
        int bTop = sprite.top();
        int bBottom = sprite.bottom();

        // corners
        if (left > 0 && top > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, 0, 0, x, y, left, top);
        if (right > 0 && top > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, nsW - bRight, 0, x + width - right, y, right, top);
        if (left > 0 && bottom > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, 0, nsH - bBottom, x, y + height - bottom, left, bottom);
        if (right > 0 && bottom > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, nsW - bRight, nsH - bBottom, x + width - right, y + height - bottom, right, bottom);

        // edges
        if (centerW > 0 && top > 0)
            blitTiledSpriteFloat(graphics, sprite, x + left, y, centerW, top,
                    bLeft, 0, nsW - bLeft - bRight, bTop, nsW, nsH);
        if (centerW > 0 && bottom > 0)
            blitTiledSpriteFloat(graphics, sprite, x + left, y + height - bottom, centerW, bottom,
                    bLeft, nsH - bBottom, nsW - bLeft - bRight, bBottom, nsW, nsH);
        if (left > 0 && centerH > 0)
            blitTiledSpriteFloat(graphics, sprite, x, y + top, left, centerH,
                    0, bTop, bLeft, nsH - bTop - bBottom, nsW, nsH);
        if (right > 0 && centerH > 0)
            blitTiledSpriteFloat(graphics, sprite, x + width - right, y + top, right, centerH,
                    nsW - bRight, bTop, bRight, nsH - bTop - bBottom, nsW, nsH);

        // center
        if (centerW > 0 && centerH > 0)
            blitTiledSpriteFloat(graphics, sprite, x + left, y + top, centerW, centerH,
                    bLeft, bTop, nsW - bLeft - bRight, nsH - bTop - bBottom, nsW, nsH);
    }
}
