package com.lowdragmc.lowdraglib2.client.shader;

import lombok.experimental.UtilityClass;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
@UtilityClass
public final class LDProgramDefineManager {
    private static final Set<String> PROGRAM_DEFINES = new LinkedHashSet<>();
    private static final Pattern VERSION_DIRECTIVE = Pattern.compile("(?m)^[\\t \\f]*(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/[\\t \\f]*)*#[\\t \\f]*version\\b[^\\r\\n]*");

    public static void addProgramDefine(String define) {
        PROGRAM_DEFINES.add(define);
    }

    public static void removeProgramDefine(String define) {
        PROGRAM_DEFINES.remove(define);
    }

    public static void clearProgramDefines() {
        PROGRAM_DEFINES.clear();
    }

    public static boolean hasProgramDefine(String define) {
        return PROGRAM_DEFINES.contains(define);
    }

    public static boolean hasProgramDefines() {
        return !PROGRAM_DEFINES.isEmpty();
    }

    public static String createProgramDefinesString() {
        return createProgramDefinesString('\n');
    }

    public static String createProgramDefinesString(char separator) {
        return String.join(String.valueOf(separator), PROGRAM_DEFINES.stream()
                .map(define -> "#define " + define)
                .toList());
    }

    public static String createProgramNameWithDefines(String name) {
        return PROGRAM_DEFINES.isEmpty() ? name : name + "_" + createProgramDefinesString('_');
    }

    public static Set<String> getProgramDefines() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(PROGRAM_DEFINES));
    }

    public static List<String> insertProgramDefines(List<String> source) {
        if (!hasProgramDefines() || source.isEmpty()) {
            return source;
        }

        var defines = createProgramDefinesString();
        for (var index = 0; index < source.size(); index++) {
            var segment = source.get(index);
            var matcher = VERSION_DIRECTIVE.matcher(segment);
            if (matcher.find()) {
                var result = new ArrayList<>(source);
                result.set(index, segment.substring(0, matcher.end()) + '\n' + defines + segment.substring(matcher.end()));
                return result;
            }
        }

        var result = new ArrayList<>(source);
        result.set(0, defines + '\n' + source.get(0));
        return result;
    }
}
