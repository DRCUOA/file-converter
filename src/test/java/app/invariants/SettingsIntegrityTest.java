package app.invariants;

import app.persistence.AppSettings;
import app.persistence.JsonSettingsStore;
import app.persistence.SettingsStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Settings file is valid JSON; invalid file does not corrupt app state.
 */
class SettingsIntegrityTest {

    @Test
    void invalidJsonDoesNotCorruptAppState() throws Exception {
        Path tempDir = Files.createTempDirectory("settings-integrity");
        Path settingsPath = tempDir.resolve("settings.json");
        Files.writeString(settingsPath, "{ invalid json }");
        SettingsStore store = new JsonSettingsStore(settingsPath);
        AppSettings loaded = store.load();
        assertThat(loaded).isNotNull();
        assertThat(loaded.apiKey()).isEqualTo("");
        assertThat(loaded.lastProfileId()).isEqualTo("mod-mov");
    }

    @Test
    void missingFileReturnsDefaults() throws Exception {
        Path tempDir = Files.createTempDirectory("settings-missing");
        Path settingsPath = tempDir.resolve("nonexistent.json");
        SettingsStore store = new JsonSettingsStore(settingsPath);
        AppSettings loaded = store.load();
        assertThat(loaded).isNotNull();
        assertThat(loaded).isEqualTo(AppSettings.defaults());
    }
}
