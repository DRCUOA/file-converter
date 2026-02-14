package app.unit;

import app.core.ConversionProfile;
import app.core.OutputNaming;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutputNamingTest {

    @Test
    void resolveInDirProducesCorrectExtension() {
        Path input = Path.of("holiday.mod");
        Path outputDir = Path.of("/output");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        Path output = OutputNaming.resolveInDir(input, outputDir, profile);
        assertThat(output.toString()).endsWith("holiday.mov");
        assertThat(output.getParent()).isEqualTo(outputDir);
    }

    @Test
    void jpegToWebp() {
        Path input = Path.of("photo.jpg");
        Path outputDir = Path.of("/out");
        var profile = new ConversionProfile("jpg-webp", "JPEG→WEBP", "jpg", "webp", Map.of());
        Path output = OutputNaming.resolveInDir(input, outputDir, profile);
        assertThat(output.toString()).endsWith("photo.webp");
    }
}
