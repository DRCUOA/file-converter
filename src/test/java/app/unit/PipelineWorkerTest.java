package app.unit;

import app.core.BatchItem;
import app.core.BatchItemStatus;
import app.core.CloudConvertFacade;
import app.core.ConversionProfile;
import app.core.PipelineWorker;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineWorkerTest {

    @Test
    void uppercaseFinishedStatusCompletesConversion() throws Exception {
        Path outputDir = Files.createTempDirectory("pipeline-worker");
        Path input = Files.createTempFile(outputDir, "video", ".mod");
        Files.writeString(input, "raw");
        BatchItem item = new BatchItem(
                input,
                new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of())
        );
        StubCloudConvertFacade facade = new StubCloudConvertFacade();

        new PipelineWorker(item, outputDir, facade, new AtomicBoolean(false)).run();

        assertThat(item.status).isEqualTo(BatchItemStatus.Done.name());
        assertThat(item.outputPath).isNotNull();
        assertThat(Files.exists(item.outputPath)).isTrue();
        assertThat(Files.readString(item.outputPath)).isEqualTo("converted");
    }

    @Test
    void objectBackedTaskListCompletesConversion() throws Exception {
        Path outputDir = Files.createTempDirectory("pipeline-worker-object");
        Path input = Files.createTempFile(outputDir, "video", ".mod");
        Files.writeString(input, "raw");
        BatchItem item = new BatchItem(
                input,
                new ConversionProfile("mod-mov", "MOD→MOV", "mod", "mov", Map.of())
        );
        ObjectTaskFacade facade = new ObjectTaskFacade();

        new PipelineWorker(item, outputDir, facade, new AtomicBoolean(false)).run();

        assertThat(item.status).isEqualTo(BatchItemStatus.Done.name());
        assertThat(item.outputPath).isNotNull();
        assertThat(Files.exists(item.outputPath)).isTrue();
    }

    private static final class StubCloudConvertFacade implements CloudConvertFacade {

        private String exportTaskName;

        @Override
        public String createJobForFile(String uploadTaskName, String convertTaskName, String exportTaskName,
                ConversionProfile profile) {
            this.exportTaskName = exportTaskName;
            return "job-1";
        }

        @Override
        public TaskResult createUploadTaskAndUpload(Path file) {
            return new TaskResult("upload-1", "FINISHED", Map.of());
        }

        @Override
        public JobResult getJob(String jobId) {
            return new JobResult("FINISHED", List.of(Map.of("name", exportTaskName, "id", "export-task-1")));
        }

        @Override
        public TaskResult getTask(String jobId, String taskId) {
            return new TaskResult(taskId, "FINISHED", new ExportResult("https://example.invalid/file.mov"));
        }

        @Override
        public InputStream download(String url) {
            return new ByteArrayInputStream("converted".getBytes());
        }

        @Override
        public void cancelTask(String jobId, String taskId) {
        }

        @Override
        public void cancelJob(String jobId) {
        }
    }

    private static final class ObjectTaskFacade implements CloudConvertFacade {

        private String exportTaskName;

        @Override
        public String createJobForFile(String uploadTaskName, String convertTaskName, String exportTaskName,
                ConversionProfile profile) {
            this.exportTaskName = exportTaskName;
            return "job-2";
        }

        @Override
        public TaskResult createUploadTaskAndUpload(Path file) {
            return new TaskResult("upload-2", "FINISHED", Map.of());
        }

        @Override
        public JobResult getJob(String jobId) {
            return new JobResult("FINISHED", List.of(new TaskEntry("export-task-2", exportTaskName, "export/url")));
        }

        @Override
        public TaskResult getTask(String jobId, String taskId) {
            return new TaskResult(
                    taskId,
                    "FINISHED",
                    Map.of("files", List.of(Map.of("url", "https://example.invalid/file.mov")))
            );
        }

        @Override
        public InputStream download(String url) {
            return new ByteArrayInputStream("converted".getBytes());
        }

        @Override
        public void cancelTask(String jobId, String taskId) {
        }

        @Override
        public void cancelJob(String jobId) {
        }
    }

    private static final class TaskEntry {

        private final String id;
        private final String name;
        private final String operation;

        private TaskEntry(String id, String name, String operation) {
            this.id = id;
            this.name = name;
            this.operation = operation;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getOperation() {
            return operation;
        }
    }

    private static final class ExportResult {

        private final List<ExportFile> files;

        private ExportResult(String url) {
            this.files = List.of(new ExportFile(url));
        }

        public List<ExportFile> getFiles() {
            return files;
        }
    }

    private static final class ExportFile {

        private final String url;

        private ExportFile(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
}
