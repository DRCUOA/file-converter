package app.invariants;

import app.core.BatchItem;
import app.core.Validation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Failure of one file does not affect others in batch.
 */
class BatchIsolationTest {

    @Test
    void validationFailureMarksOnlyThatItem() throws Exception {
        Path tempDir = Files.createTempDirectory("batch-isolation");
        Path validFile = tempDir.resolve("valid.mod");
        Path invalidFile = tempDir.resolve("invalid.txt");
        Files.writeString(validFile, "content");
        Files.writeString(invalidFile, "content");
        var profile = new app.core.ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        BatchItem valid = new BatchItem(validFile, profile);
        BatchItem invalid = new BatchItem(invalidFile, profile);
        var validResult = Validation.validate(valid);
        var invalidResult = Validation.validate(invalid);
        assertThat(validResult.valid()).isTrue();
        assertThat(invalidResult.valid()).isFalse();
    }
}
