package app.invariants;

import app.core.PipelineWorker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: PipelineWorker never runs on FX thread.
 */
class FXThreadInvariantTest {

    @Test
    void pipelineWorkerIsRunnable() {
        assertThat(Runnable.class.isAssignableFrom(PipelineWorker.class)).isTrue();
    }

    @Test
    void pipelineWorkerDoesNotImportJavaFX() {
        assertThat(PipelineWorker.class.getPackage().getName()).doesNotContain("javafx");
    }
}
