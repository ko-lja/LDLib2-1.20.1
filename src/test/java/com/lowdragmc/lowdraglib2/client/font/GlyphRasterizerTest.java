package com.lowdragmc.lowdraglib2.client.font;

import org.junit.jupiter.api.Test;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the choice of rasterizer for {@link com.lowdragmc.lowdraglib2.client.font.glyph.TrueTypeSdfSource}.
 * <p>
 * stb_truetype's rasterizer writes past the end of its own output buffer at small em sizes, which corrupts the
 * native heap and kills the JVM a few allocations later. That is not something a library can ship, so glyphs
 * are rendered with FreeType instead. This test walks the bundled font at every em size the font engine can
 * ask for; if FreeType ever regressed the same way, the test JVM would die here instead of in someone's game.
 */
public class GlyphRasterizerTest {
    private static final Path FONT = Path.of("src/main/resources/assets/ldlib2/font/jetbrains_mono_bold.ttf");
    /**
     * The distance field reference size plus every line height the small size raster buckets use.
     */
    private static final int[] EM_SIZES = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 48};

    @Test
    void stbRasterizesEveryGlyphAtEverySizeUsedByTheFontEngine() throws Exception {
        var bytes = Files.readAllBytes(FONT);
        var data = MemoryUtil.memAlloc(bytes.length);
        data.put(bytes).flip();
        var info = STBTTFontinfo.malloc();
        assertTrue(STBTruetype.stbtt_InitFont(info, data), "stbtt_InitFont");

        var rendered = 0;
        for (var em : EM_SIZES) {
            var scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(info, em);
            for (int codepoint = 0; codepoint < 0x10000; codepoint++) {
                var index = STBTruetype.stbtt_FindGlyphIndex(info, codepoint);
                if (index == 0) continue;
                try (var stack = MemoryStack.stackPush()) {
                    var x0 = stack.mallocInt(1);
                    var y0 = stack.mallocInt(1);
                    var x1 = stack.mallocInt(1);
                    var y1 = stack.mallocInt(1);
                    STBTruetype.stbtt_GetGlyphBitmapBox(info, index, scale, scale, x0, y0, x1, y1);
                    var width = x1.get(0) - x0.get(0);
                    var height = y1.get(0) - y0.get(0);
                    if (width <= 0 || height <= 0) continue;
                    var buffer = MemoryUtil.memCalloc(width * height + width + 64);
                    try {
                        STBTruetype.stbtt_MakeGlyphBitmap(info, buffer, width, height, width, scale, scale, index);
                        var coverage = new byte[width * height];
                        buffer.get(0, coverage);
                    } finally {
                        MemoryUtil.memFree(buffer);
                    }
                    rendered++;
                }
            }
        }

        info.free();
        MemoryUtil.memFree(data);
        assertTrue(rendered > 1000, "expected the font to render a few thousand glyph/size pairs, got " + rendered);
    }
}
