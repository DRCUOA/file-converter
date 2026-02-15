package app.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Pre-flight validation for batch items.
 */
public final class Validation {

    private static final Set<String> ALLOWED_EXTENSIONS = buildAllowedExtensions();

    private Validation() {
    }

    public static ValidationResult validate(BatchItem item) {
        if (item == null || item.input == null) {
            return ValidationResult.failure("Invalid batch item");
        }
        if (!Files.exists(item.input)) {
            return ValidationResult.failure("File does not exist");
        }
        if (!Files.isReadable(item.input)) {
            return ValidationResult.failure("File is not readable");
        }
        try {
            if (Files.size(item.input) == 0) {
                return ValidationResult.failure("File is empty");
            }
        } catch (Exception e) {
            return ValidationResult.failure("Cannot read file size: " + e.getMessage());
        }
        String ext = getExtension(item.input);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            return ValidationResult.failure("Format not supported: " + ext);
        }
        if (!isProfileCompatible(ext, item.profile)) {
            return ValidationResult.failure("Profile incompatible with file format");
        }
        return ValidationResult.success();
    }

    private static String getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? "." + name.substring(dot + 1) : "";
    }

    private static boolean isProfileCompatible(String ext, ConversionProfile profile) {
        String normalized = ext.toLowerCase().replaceFirst("^\\.", "");
        if (normalized.equals("jpeg")) {
            normalized = "jpg";
        }
        return normalized.equals(profile.inputFormat());
    }

    private static Set<String> buildAllowedExtensions() {
        Set<String> extensions = new LinkedHashSet<>();
        for (ConversionProfile profile : Profiles.all()) {
            String format = profile.inputFormat();
            if (format == null || format.isBlank()) {
                continue;
            }
            String normalized = format.toLowerCase();
            extensions.add("." + normalized);
            if ("jpg".equals(normalized)) {
                extensions.add(".jpeg");
            }
        }
        return Collections.unmodifiableSet(extensions);
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult failure(String msg) {
            return new ValidationResult(false, msg);
        }
    }
}
