package com.lowdragmc.lowdraglib2.client.font.glyph;

import com.lowdragmc.lowdraglib2.LDLib2;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Rasterizes TrueType/OpenType outlines and turns them into signed distance fields.
 * <p>
 * Rasterization goes through FreeType, the same library Minecraft itself uses for TTF fonts. stb_truetype was
 * tried first and had to be abandoned: its rasterizer writes past the end of its own output buffer at small em
 * sizes, which corrupts the native heap and takes the JVM down a few allocations later. FreeType renders these
 * same fonts every time the vanilla font manager loads them.
 * <p>
 * Metrics mirror vanilla's {@link com.mojang.blaze3d.font.TrueTypeGlyphProvider}: the face is scaled so one em
 * maps to {@code emPixels} device pixels and every metric is divided by the oversample factor to land back in
 * the 9 pixel design space, so advances stay identical to what the vanilla renderer produced.
 */
@OnlyIn(Dist.CLIENT)
public class TrueTypeSdfSource implements GlyphSource {
    /**
     * Padding around each glyph, also the distance in pixels the field spreads over.
     */
    private static final int PADDING = 6;
    /**
     * How close {@code size * oversample} has to be to a whole pixel size for the exact size raster path to be
     * able to map one texel to one device pixel.
     */
    private static final float EXACT_TOLERANCE = 0.01f;

    @Nullable
    private ByteBuffer fontData;
    @Nullable
    private STBTTFontinfo face;
    private final float size;
    private final int emPixels;
    private final float oversample;
    private final float shiftX;
    private final float shiftY;
    private final IntSet skip = new IntArraySet();

    private TrueTypeSdfSource(ByteBuffer fontData, STBTTFontinfo face, float size, int emPixels,
                              float shiftX, float shiftY, String skip) {
        this.fontData = fontData;
        this.face = face;
        this.size = size;
        this.emPixels = emPixels;
        this.oversample = emPixels / size;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
        skip.codePoints().forEach(this.skip::add);
    }

    /**
     * @param emPixels pixel height one em is rasterized at, higher keeps corners sharper at large font sizes
     */
    @Nullable
    public static TrueTypeSdfSource load(ResourceManager resourceManager, TrueTypeGlyphProviderDefinition definition,
                                         int emPixels) {
        var file = definition.location().withPrefix("font/");
        ByteBuffer data = null;
        STBTTFontinfo face = null;
        try (InputStream stream = resourceManager.open(file)) {
            var bytes = stream.readAllBytes();
            data = MemoryUtil.memAlloc(bytes.length);
            data.put(bytes).flip();
            face = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(face, data)) {
                throw new IOException("Invalid font data");
            }
            return new TrueTypeSdfSource(data, face, definition.size(), emPixels,
                    definition.shift().x(), definition.shift().y(), definition.skip());
        } catch (IOException | RuntimeException e) {
            if (face != null) {
                face.free();
            }
            if (data != null) {
                MemoryUtil.memFree(data);
            }
            LDLib2.LOGGER.error("Failed to load font {}", file, e);
            return null;
        }
    }

    @Nullable
    @Override
    public synchronized GlyphMetrics metrics(int codepoint) {
        var index = glyphIndex(codepoint);
        if (index == 0) {
            return null;
        }
        var face = this.face;
        if (face == null) {
            return null;
        }
        try (var stack = MemoryStack.stackPush()) {
            var advance = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(face, index, advance, null);
            return GlyphMetrics.of(advance.get(0) * STBTruetype.stbtt_ScaleForMappingEmToPixels(face, emPixels)
                    / oversample);
        }
    }

    @Nullable
    @Override
    public synchronized RawGlyph glyph(int codepoint) {
        var index = glyphIndex(codepoint);
        if (index == 0) {
            return null;
        }
        var rendered = render(index, emPixels);
        if (rendered == null) {
            return null;
        }
        if (rendered.coverage() == null) {
            return RawGlyph.space(rendered.advance() / oversample);
        }

        // fromCoverage, not fromMask: FreeType's grey levels say where the outline runs inside each edge
        // pixel, and throwing that away is what makes large text look jagged
        var sdf = DistanceTransform.fromCoverage(rendered.coverage(), rendered.width(), rendered.height(),
                PADDING, PADDING);
        var designPadding = PADDING / oversample;
        var left = rendered.bearingLeft() / oversample + shiftX - designPadding;
        var top = 7f - rendered.bearingTop() / oversample + shiftY - designPadding;
        var width = rendered.width() + PADDING * 2;
        var height = rendered.height() + PADDING * 2;
        return RawGlyph.of(sdf, width, height, left, top,
                left + width / oversample, top + height / oversample, rendered.advance() / oversample);
    }

    @Nullable
    @Override
    public synchronized RawGlyph rasterGlyph(int codepoint, float oversample, boolean exactOnly) {
        var index = glyphIndex(codepoint);
        if (index == 0) {
            return null;
        }
        var exactPixelSize = size * oversample;
        var pixelSize = Math.round(exactPixelSize);
        if (pixelSize <= 0) {
            return null;
        }
        // Under nearest sampling one texel has to land on one device pixel, or columns get dropped or doubled.
        // The tolerance is relative because the same absolute slip matters far less on a tall glyph.
        if (exactOnly && Math.abs(exactPixelSize - pixelSize) / pixelSize > EXACT_TOLERANCE) {
            return null;
        }
        // the advance stays in the distance field's design space, layout must not shift between buckets
        var metrics = metrics(codepoint);
        if (metrics == null) {
            return null;
        }
        var rendered = render(index, pixelSize);
        if (rendered == null) {
            return null;
        }
        if (rendered.coverage() == null) {
            return RawGlyph.space(metrics.advance());
        }
        var left = rendered.bearingLeft() / oversample + shiftX;
        var top = 7f - rendered.bearingTop() / oversample + shiftY;
        return RawGlyph.of(rendered.coverage(), rendered.width(), rendered.height(), left, top,
                left + rendered.width() / oversample, top + rendered.height() / oversample, metrics.advance());
    }

    /**
     * @param coverage 8 bit grayscale rows, or null when the glyph has no bitmap at all (a space)
     */
    private record Rendered(@Nullable byte[] coverage, int width, int height,
                            int bearingLeft, int bearingTop, float advance) {
    }

    @Nullable
    private Rendered render(int glyphIndex, int pixelSize) {
        var face = this.face;
        if (face == null) {
            return null;
        }
        var scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(face, pixelSize);
        try (var stack = MemoryStack.stackPush()) {
            var advanceWidth = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(face, glyphIndex, advanceWidth, null);
            var advance = advanceWidth.get(0) * scale;
            var x0 = stack.mallocInt(1);
            var y0 = stack.mallocInt(1);
            var x1 = stack.mallocInt(1);
            var y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphBitmapBox(face, glyphIndex, scale, scale, x0, y0, x1, y1);
            var width = x1.get(0) - x0.get(0);
            var height = y1.get(0) - y0.get(0);
            if (width <= 0 || height <= 0) {
                return new Rendered(null, 0, 0, 0, 0, advance);
            }
            var buffer = MemoryUtil.memCalloc(width * height + width + 64);
            try {
                STBTruetype.stbtt_MakeGlyphBitmap(face, buffer, width, height, width, scale, scale, glyphIndex);
                var coverage = new byte[width * height];
                buffer.get(0, coverage);
                return new Rendered(coverage, width, height, x0.get(0), -y0.get(0), advance);
            } finally {
                MemoryUtil.memFree(buffer);
            }
        }
    }

    private int glyphIndex(int codepoint) {
        var face = this.face;
        if (face == null || skip.contains(codepoint)) {
            return 0;
        }
        return STBTruetype.stbtt_FindGlyphIndex(face, codepoint);
    }

    @Override
    public synchronized void close() {
        if (face != null) {
            face.free();
            face = null;
        }
        if (fontData != null) {
            MemoryUtil.memFree(fontData);
            fontData = null;
        }
    }
}
