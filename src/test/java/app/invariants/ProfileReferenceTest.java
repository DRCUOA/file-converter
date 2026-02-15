package app.invariants;

import app.core.BatchItem;
import app.core.ConversionProfile;
import app.core.Profiles;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: BatchItem references valid ConversionProfile.
 */
class ProfileReferenceTest {

    @Test
    void batchItemHoldsProfileReference() {
        var profile = new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of());
        BatchItem item = new BatchItem(Path.of("/a.mod"), profile);
        assertThat(item.profile).isNotNull();
        assertThat(item.profile.id()).isEqualTo("mod-mov");
    }

    @Test
    void predefinedProfilesAreValid() {
        for (ConversionProfile p : Profiles.all()) {
            assertThat(p.id()).isNotBlank();
            assertThat(p.inputFormat()).isNotBlank();
            assertThat(p.outputFormat()).isNotBlank();
        }
    }
}
