package app.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JSON-based settings persistence.
 */
public class JsonSettingsStore implements SettingsStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final Path LEGACY_HOME_SETTINGS_PATH =
            Paths.get(System.getProperty("user.home"), ".file-converter", SETTINGS_FILE_NAME);

    private final Path settingsPath;

    public JsonSettingsStore() {
        this(resolveDefaultSettingsPath());
    }

    public JsonSettingsStore(Path settingsPath) {
        this.settingsPath = settingsPath;
    }

    private static Path resolveDefaultSettingsPath() {
        Path projectRoot = findProjectRoot(Paths.get(System.getProperty("user.dir")));
        if (projectRoot != null) {
            return projectRoot.resolve(SETTINGS_FILE_NAME);
        }
        return LEGACY_HOME_SETTINGS_PATH;
    }

    private static Path findProjectRoot(Path startPath) {
        Path current = startPath.toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))
                    || Files.exists(current.resolve("settings.gradle"))
                    || Files.exists(current.resolve("build.gradle"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public AppSettings load() {
        if (!Files.exists(settingsPath)) {
            return AppSettings.defaults();
        }
        try {
            byte[] bytes = Files.readAllBytes(settingsPath);
            SettingsDto dto = MAPPER.readValue(bytes, SettingsDto.class);
            return new AppSettings(
                    dto.apiKey != null ? dto.apiKey : "",
                    dto.lastOutputDir != null ? Paths.get(dto.lastOutputDir) : null,
                    dto.lastProfileId != null ? dto.lastProfileId : "mod-mov"
            );
        } catch (IOException e) {
            return AppSettings.defaults();
        }
    }

    @Override
    public void save(AppSettings settings) {
        try {
            Path parent = settingsPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            SettingsDto dto = new SettingsDto(
                    settings.apiKey(),
                    settings.lastOutputDir() != null ? settings.lastOutputDir().toString() : null,
                    settings.lastProfileId()
            );
            MAPPER.writeValue(settingsPath.toFile(), dto);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings", e);
        }
    }

    @Override
    public Path getSettingsPath() {
        return settingsPath;
    }

    private record SettingsDto(String apiKey, String lastOutputDir, String lastProfileId) {
    }
}
