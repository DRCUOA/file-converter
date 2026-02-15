package app.unit;

import app.core.BatchItem;
import app.core.ConversionProfile;
import app.core.Validation;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest {

    @Test
    void nullItemFails() {
        var result = Validation.validate(null);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void nullInputFails() {
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        var item = new BatchItem(null, profile);
        var result = Validation.validate(item);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void validModFilePasses() throws Exception {
        Path f = Files.createTempFile("test", ".mod");
        Files.writeString(f, "data");
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        var result = Validation.validate(new BatchItem(f, profile));
        assertThat(result.valid()).isTrue();
    }
}
