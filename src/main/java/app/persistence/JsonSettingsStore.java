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

    private final Path settingsPath;

    public JsonSettingsStore() {
        this(Paths.get(System.getProperty("user.home"), ".file-converter", "settings.json"));
    }

    public JsonSettingsStore(Path settingsPath) {
        this.settingsPath = settingsPath;
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
