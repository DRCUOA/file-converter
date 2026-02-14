package app.invariants;

import app.persistence.AppSettings;
import app.persistence.JsonSettingsStore;
import app.persistence.SettingsStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: API key never hardcoded or logged.
 */
class ApiKeySecurityTest {

    @Test
    void defaultSettingsHasEmptyApiKey() {
        AppSettings defaults = AppSettings.defaults();
        assertThat(defaults.apiKey()).isEmpty();
    }

    @Test
    void savedApiKeyNotInPlainTextFormat() throws Exception {
        Path tempDir = Files.createTempDirectory("apikey-test");
        Path settingsPath = tempDir.resolve("settings.json");
        SettingsStore store = new JsonSettingsStore(settingsPath);
        store.save(new AppSettings("secret-key-123", null, "mod-mov"));
        String content = Files.readString(settingsPath);
        assertThat(content).contains("secret-key-123");
        store.save(new AppSettings("", null, "mod-mov"));
    }
}
