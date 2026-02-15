package app.invariants;

import app.core.BatchItem;
import app.core.Profiles;
import app.core.Validation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Formats with configured conversion pathways are accepted.
 */
class FormatValidationTest {

    @Test
    void modAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".mod");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.MOD_TO_MOV));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void jpgAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".jpg");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.JPEG_TO_WEBP));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void jpegAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".jpeg");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.JPEG_TO_WEBP));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void docxAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".docx");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.DOCX_TO_PDF));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void pngAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".png");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.PNG_TO_WEBP));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void pdfAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".pdf");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.PDF_TO_DOCX));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void wavAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".wav");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.WAV_TO_MP3));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void movAccepted() throws Exception {
        Path f = Files.createTempFile("test", ".mov");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.MOV_TO_MP4));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void txtRejected() throws Exception {
        Path f = Files.createTempFile("test", ".txt");
        Files.writeString(f, "x");
        var result = Validation.validate(new BatchItem(f, Profiles.MOD_TO_MOV));
        assertThat(result.valid()).isFalse();
    }
}
