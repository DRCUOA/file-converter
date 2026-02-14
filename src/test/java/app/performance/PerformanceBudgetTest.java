package app.performance;

import app.core.BatchItem;
import app.core.ConversionProfile;
import app.core.OutputNaming;
import app.core.Validation;
import app.persistence.AppSettings;
import app.persistence.JsonSettingsStore;
import app.persistence.SettingsStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance budgets: Validation < 50ms, OutputNaming < 5ms, Settings < 100ms.
 */
class PerformanceBudgetTest {

    @Test
    void validationCompletesUnder50ms() throws Exception {
        Path f = Files.createTempFile("perf", ".mod");
        Files.writeString(f, "content");
        var item = new BatchItem(f, f.getParent(),
                new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of()));
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            Validation.validate(item);
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(durationMs).isLessThan(5000);
    }

    @Test
    void outputNamingCompletesUnder5ms() {
        Path input = Path.of("/tmp/file.mod");
        Path outputDir = Path.of("/out");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            OutputNaming.resolveInDir(input, outputDir, profile);
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(durationMs).isLessThan(100);
    }

    @Test
    void settingsLoadSaveUnder100ms() throws Exception {
        Path tempDir = Files.createTempDirectory("perf-settings");
        Path settingsPath = tempDir.resolve("settings.json");
        SettingsStore store = new JsonSettingsStore(settingsPath);
        long start = System.nanoTime();
        store.save(AppSettings.defaults());
        AppSettings loaded = store.load();
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(durationMs).isLessThan(100);
        assertThat(loaded).isNotNull();
    }
}
