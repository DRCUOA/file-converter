package app.persistence;

import java.nio.file.Path;

/**
 * Application settings (API key, last output dir, last profile).
 */
public record AppSettings(
        String apiKey,
        Path lastOutputDir,
        String lastProfileId
) {
    public static AppSettings defaults() {
        return new AppSettings("", null, "mod-mov");
    }
}
