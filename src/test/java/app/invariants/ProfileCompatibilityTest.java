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
 * Invariant: Profile compatibility enforced before API call.
 */
class ProfileCompatibilityTest {

    @Test
    void modFileWithModMovProfileCompatible() throws Exception {
        Path f = Files.createTempFile("test", ".mod");
        Files.writeString(f, "x");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        var result = Validation.validate(new BatchItem(f, f.getParent(), profile));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void modFileWithDocxPdfProfileIncompatible() throws Exception {
        Path f = Files.createTempFile("test", ".mod");
        Files.writeString(f, "x");
        var profile = new ConversionProfile("docx-pdf", "DOCX→PDF", "docx", "pdf", Map.of());
        var result = Validation.validate(new BatchItem(f, f.getParent(), profile));
        assertThat(result.valid()).isFalse();
    }
}
