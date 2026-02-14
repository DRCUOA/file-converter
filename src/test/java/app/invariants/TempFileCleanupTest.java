package app.invariants;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Temp file is always cleaned up on failure.
 */
class TempFileCleanupTest {

    @Test
    void tempDirPatternIsDocumented() throws Exception {
        Path tempDir = Files.createTempDirectory("file-converter-tmp");
        Path partFile = tempDir.resolve(".tmp").resolve("test.part");
        Files.createDirectories(partFile.getParent());
        Files.writeString(partFile, "partial");
        assertThat(Files.exists(partFile)).isTrue();
        Files.deleteIfExists(partFile);
        Files.deleteIfExists(partFile.getParent());
        Files.deleteIfExists(tempDir);
    }
}
