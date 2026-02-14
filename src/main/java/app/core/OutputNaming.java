package app.core;

import java.nio.file.Path;

/**
 * Resolves output file paths for converted files.
 */
public final class OutputNaming {

    private OutputNaming() {
    }

    public static Path resolveInDir(Path input, Path outputDir, ConversionProfile profile) {
        String baseName = getBaseName(input);
        String outputExt = profile.outputFormat();
        return outputDir.resolve(baseName + "." + outputExt);
    }

    private static String getBaseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}
