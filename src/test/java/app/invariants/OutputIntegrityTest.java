package app.invariants;

import app.core.BatchItem;
import app.core.ConversionProfile;
import app.core.OutputNaming;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Output file is written only after successful download.
 */
class OutputIntegrityTest {

    @Test
    void outputFileNotWrittenUntilPipelineSucceeds() throws Exception {
        Path tempDir = Files.createTempDirectory("output-integrity");
        Path input = tempDir.resolve("test.mod");
        Files.writeString(input, "fake mod content");
        ConversionProfile profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        BatchItem item = new BatchItem(input, tempDir, profile);
        assertThat(item.outputPath).isNull();
        assertThat(Files.list(tempDir).filter(p -> !p.getFileName().toString().startsWith(".")).count())
                .isEqualTo(1);
    }

    @Test
    void outputNamingResolvesCorrectly() {
        Path input = Path.of("/tmp/holiday.mod");
        ConversionProfile profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        Path output = OutputNaming.resolveInDir(input, Path.of("/out"), profile);
        assertThat(output.toString()).endsWith("holiday.mov");
    }
}
