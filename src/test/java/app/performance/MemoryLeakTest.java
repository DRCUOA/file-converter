package app.performance;

import app.core.BatchItem;
import app.core.ConversionProfile;
import app.core.Validation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No memory growth over repeated calls.
 */
class MemoryLeakTest {

    @Test
    void noMemoryGrowthOverRepeatedValidation() throws Exception {
        Path f = Files.createTempFile("mem", ".mod");
        Files.writeString(f, "content");
        var item = new BatchItem(f,
                new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of()));
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        for (int i = 0; i < 50; i++) {
            Validation.validate(item);
        }
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long after = rt.totalMemory() - rt.freeMemory();
        assertThat(after - before).isLessThan(10_000_000);
    }
}
