package app.core;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the conversion pipeline for a single file. Never runs on FX thread.
 */
public class PipelineWorker implements Runnable {

    private final BatchItem item;
    private final CloudConvertFacade facade;
    private final AtomicBoolean cancelRequested;

    public PipelineWorker(BatchItem item, CloudConvertFacade facade, AtomicBoolean cancelRequested) {
        this.item = item;
        this.facade = facade;
        this.cancelRequested = cancelRequested;
    }

    @Override
    public void run() {
        Validation.ValidationResult result = Validation.validate(item);
        if (!result.valid()) {
            item.status = BatchItemStatus.Failed.name();
            item.message = result.message();
            return;
        }
        if (cancelRequested.get()) {
            item.status = BatchItemStatus.Canceled.name();
            return;
        }
        item.status = BatchItemStatus.Uploading.name();
        try {
            executeConversion();
        } catch (Exception e) {
            item.status = BatchItemStatus.Failed.name();
            item.message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void executeConversion() throws Exception {
        CloudConvertFacade.TaskResult uploadResult = facade.createUploadTaskAndUpload(item.input);
        item.uploadTaskId = uploadResult.taskId();
        if (cancelRequested.get()) {
            item.status = BatchItemStatus.Canceled.name();
            return;
        }
        String convertName = "convert-" + java.util.UUID.randomUUID();
        String exportName = "export-" + java.util.UUID.randomUUID();
        String jobId = facade.createJobForFile(uploadResult.taskId(), convertName, exportName, item.profile);
        item.jobId = jobId;
        item.status = BatchItemStatus.Converting.name();
        String exportTaskId = pollUntilComplete(jobId, exportName);
        if (cancelRequested.get()) {
            item.status = BatchItemStatus.Canceled.name();
            return;
        }
        item.status = BatchItemStatus.Downloading.name();
        String url = getExportUrl(exportTaskId);
        Path outputPath = OutputNaming.resolveInDir(item.input, item.outputDir, item.profile);
        Path tmpDir = item.outputDir.resolve(".tmp");
        Files.createDirectories(tmpDir);
        String baseName = item.input.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        String nameWithoutExt = dot >= 0 ? baseName.substring(0, dot) : baseName;
        Path partFile = tmpDir.resolve(nameWithoutExt + ".part");
        try (InputStream in = facade.download(url)) {
            Files.copy(in, partFile, StandardCopyOption.REPLACE_EXISTING);
        }
        item.status = BatchItemStatus.Saving.name();
        Files.move(partFile, outputPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        item.outputPath = outputPath;
        item.status = BatchItemStatus.Done.name();
        item.progress = 1.0;
    }

    private String pollUntilComplete(String jobId, String exportTaskName) throws Exception {
        int maxPolls = 600;
        for (int i = 0; i < maxPolls && !cancelRequested.get(); i++) {
            CloudConvertFacade.JobResult job = facade.getJob(jobId);
            if ("finished".equals(job.status())) {
                return findExportTaskId(job, exportTaskName);
            }
            if ("error".equals(job.status())) {
                throw new RuntimeException("Job failed");
            }
            Thread.sleep(3000);
        }
        throw new RuntimeException("Job timed out");
    }

    @SuppressWarnings("unchecked")
    private String findExportTaskId(CloudConvertFacade.JobResult job, String exportTaskName) {
        Object tasks = job.tasks();
        if (tasks instanceof List) {
            for (Object t : (List<?>) tasks) {
                if (t instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) t;
                    if (exportTaskName.equals(m.get("name"))) {
                        Object id = m.get("id");
                        return id != null ? id.toString() : null;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String getExportUrl(String exportTaskId) throws Exception {
        CloudConvertFacade.TaskResult task = facade.getTask(item.jobId, exportTaskId);
        Object result = task.output();
        if (result instanceof Map) {
            Object files = ((Map<String, Object>) result).get("files");
            if (files instanceof List && !((List<?>) files).isEmpty()) {
                Object first = ((List<?>) files).get(0);
                if (first instanceof Map) {
                    Object url = ((Map<String, Object>) first).get("url");
                    return url != null ? url.toString() : null;
                }
            }
        }
        throw new RuntimeException("No export URL in task result");
    }
}
