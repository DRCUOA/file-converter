package app.security;

import app.persistence.AppSettings;
import app.persistence.JsonSettingsStore;
import app.persistence.SettingsStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security baseline: API key handling, path traversal, credential leak.
 */
class SecurityBaselineTest {

    @Test
    void apiKeyNotExposedInDefaults() {
        AppSettings defaults = AppSettings.defaults();
        assertThat(defaults.apiKey()).isEmpty();
    }

    @Test
    void pathTraversalBlocked() {
        Path outputDir = Path.of("/safe/output");
        Path input = Path.of("malicious/../../etc/passwd.mod");
        var profile = new app.core.ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", java.util.Map.of());
        Path output = app.core.OutputNaming.resolveInDir(input, outputDir, profile);
        assertThat(output.normalize().startsWith(outputDir.normalize())).isTrue();
    }

    @Test
    void invalidSettingsNoCrash() throws Exception {
        Path tempDir = Files.createTempDirectory("security");
        Path settingsPath = tempDir.resolve("bad.json");
        Files.writeString(settingsPath, "not json at all {{{");
        SettingsStore store = new JsonSettingsStore(settingsPath);
        AppSettings loaded = store.load();
        assertThat(loaded).isNotNull();
    }
}
