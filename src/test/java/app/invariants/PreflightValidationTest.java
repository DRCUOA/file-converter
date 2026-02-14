package app.invariants;

import app.core.BatchItem;
import app.core.ConversionProfile;
import app.core.Validation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: File must exist, readable, size > 0 before upload.
 */
class PreflightValidationTest {

    @Test
    void nonexistentFileFails() {
        Path fake = Path.of("/nonexistent/path/file.mod");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        var result = Validation.validate(new BatchItem(fake, Path.of("/out"), profile));
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("exist");
    }

    @Test
    void emptyFileFails() throws Exception {
        Path f = Files.createTempFile("empty", ".mod");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        var result = Validation.validate(new BatchItem(f, f.getParent(), profile));
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("empty");
    }

    @Test
    void validFilePasses() throws Exception {
        Path f = Files.createTempFile("valid", ".mod");
        Files.writeString(f, "content");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        var result = Validation.validate(new BatchItem(f, f.getParent(), profile));
        assertThat(result.valid()).isTrue();
    }
}
