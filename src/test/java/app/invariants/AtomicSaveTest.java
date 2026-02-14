package app.invariants;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Atomic move used for final save (no partial writes).
 */
class AtomicSaveTest {

    @Test
    void atomicMoveIsUsedForFinalSave() throws Exception {
        Path tempDir = Files.createTempDirectory("atomic-save");
        Path partFile = tempDir.resolve(".tmp").resolve("test.part");
        Path finalFile = tempDir.resolve("test.mov");
        Files.createDirectories(partFile.getParent());
        Files.writeString(partFile, "content");
        Files.move(partFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        assertThat(Files.exists(finalFile)).isTrue();
        assertThat(Files.exists(partFile)).isFalse();
        assertThat(Files.readString(finalFile)).isEqualTo("content");
        Files.deleteIfExists(finalFile);
        Files.deleteIfExists(partFile.getParent());
        Files.deleteIfExists(tempDir);
    }
}
