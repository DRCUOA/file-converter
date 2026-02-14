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
 * Invariant: Only .mod, .jpg, .jpeg, .docx accepted for MVP profiles.
 */
class FormatValidationTest {

    private static final ConversionProfile MOD_MOV =
            new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
    private static final ConversionProfile JPG_WEBP =
            new ConversionProfile("jpg-webp", "JPEG→WEBP", "jpg", "webp", Map.of());
    private static final ConversionProfile DOCX_PDF =
            new ConversionProfile("docx-pdf", "DOCX→PDF", "docx", "pdf", Map.of());

    @Test
    void modAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".mod");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, f.getParent(), MOD_MOV));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void jpgAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".jpg");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, f.getParent(), JPG_WEBP));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void jpegAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".jpeg");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, f.getParent(), JPG_WEBP));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void docxAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".docx");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, f.getParent(), DOCX_PDF));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void txtRejected() throws Exception {
        Path f = Files.createTempFile("test", ".txt");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, f.getParent(), MOD_MOV));
        assertThat(result.valid()).isFalse();
    }
}
