package app.invariants;

import app.core.ConversionProfile;
import app.core.OutputNaming;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Output path cannot escape chosen directory (path traversal).
 */
class PathTraversalTest {

    @Test
    void outputPathStaysWithinOutputDir() {
        Path outputDir = Path.of("/chosen/output");
        Path input = Path.of("/input/file.mod");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        Path output = OutputNaming.resolveInDir(input, outputDir, profile);
        assertThat(output.startsWith(outputDir)).isTrue();
        assertThat(output.getParent()).isEqualTo(outputDir);
    }

    @Test
    void baseNameExtractedWithoutPathComponents() {
        Path outputDir = Path.of("/out");
        Path input = Path.of("secret.mod");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        Path output = OutputNaming.resolveInDir(input, outputDir, profile);
        assertThat(output.getParent()).isEqualTo(outputDir);
        assertThat(output.getFileName().toString()).isEqualTo("secret.mov");
    }
}
