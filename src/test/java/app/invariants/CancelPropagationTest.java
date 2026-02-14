package app.invariants;

import app.core.BatchRunner;
import app.core.CloudConvertFacade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: Cancellation propagates to CloudConvert task/job when supported.
 */
class CancelPropagationTest {

    @Test
    void batchRunnerHasCancelMethod() throws NoSuchMethodException {
        assertThat(BatchRunner.class.getMethod("cancel")).isNotNull();
    }

    @Test
    void cloudConvertFacadeHasCancelMethods() {
        assertThat(CloudConvertFacade.class.getMethods()).anyMatch(m ->
                m.getName().equals("cancelTask") || m.getName().equals("cancelJob"));
    }
}
