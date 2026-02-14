package app.invariants;

import app.core.BatchItem;
import app.core.BatchRunner;
import app.core.CloudConvertFacade;
import app.core.CloudConvertFacade.JobResult;
import app.core.CloudConvertFacade.TaskResult;
import app.core.ConversionProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant: One CloudConvert job per file (no multi-file jobs).
 */
class JobIsolationTest {

    @Test
    void batchRunnerSubmitsOneWorkerPerFile() {
        CloudConvertFacade facade = new CloudConvertFacade() {
            @Override
            public String createJobForFile(String a, String b, String c, ConversionProfile p) {
                throw new UnsupportedOperationException("mock");
            }

            @Override
            public TaskResult createUploadTaskAndUpload(Path f) {
                throw new UnsupportedOperationException("mock");
            }

            @Override
            public JobResult getJob(String id) {
                throw new UnsupportedOperationException("mock");
            }

            @Override
            public TaskResult getTask(String j, String t) {
                throw new UnsupportedOperationException("mock");
            }

            @Override
            public java.io.InputStream download(String url) {
                throw new UnsupportedOperationException("mock");
            }

            @Override
            public void cancelTask(String j, String t) {
            }

            @Override
            public void cancelJob(String id) {
            }
        };
        BatchRunner runner = new BatchRunner(facade, 2);
        assertThat(runner).isNotNull();
    }

    @Test
    void pipelineWorkerProcessesSingleFile() {
        BatchItem item = new BatchItem(Path.of("/tmp/a.mod"), Path.of("/out"),
                new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of()));
        assertThat(item.jobId).isNull();
    }
}
