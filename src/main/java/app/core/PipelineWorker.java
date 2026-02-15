package app.core;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the conversion pipeline for a single file. Never runs on FX thread.
 */
public class PipelineWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineWorker.class);

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
        LOG.debug("Worker started for {}", item.input);
        Validation.ValidationResult result = Validation.validate(item);
        if (!result.valid()) {
            item.status = BatchItemStatus.Failed.name();
            item.message = result.message();
            LOG.warn("Validation failed for {}: {}", item.input, result.message());
            return;
        }
        if (cancelRequested.get()) {
            item.status = BatchItemStatus.Canceled.name();
            LOG.debug("Worker canceled before upload for {}", item.input);
            return;
        }
        item.status = BatchItemStatus.Uploading.name();
        try {
            executeConversion();
        } catch (Exception e) {
            item.status = BatchItemStatus.Failed.name();
            item.message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOG.error("Worker failed for {}", item.input, e);
        }
    }

    private void executeConversion() throws Exception {
        LOG.debug("Creating upload task for {}", item.input);
        CloudConvertFacade.TaskResult uploadResult = facade.createUploadTaskAndUpload(item.input);
        item.uploadTaskId = uploadResult.taskId();
        LOG.debug("Upload task created: taskId={}", item.uploadTaskId);
        if (cancelRequested.get()) {
            item.status = BatchItemStatus.Canceled.name();
            LOG.debug("Worker canceled after upload for {}", item.input);
            return;
        }
        String convertName = "convert-" + java.util.UUID.randomUUID();
        String exportName = "export-" + java.util.UUID.randomUUID();
        String jobId = facade.createJobForFile(uploadResult.taskId(), convertName, exportName, item.profile);
        item.jobId = jobId;
        LOG.debug("Conversion job created: jobId={}", item.jobId);
        item.status = BatchItemStatus.Converting.name();
        String exportTaskId = pollUntilComplete(jobId, exportName);
        if (cancelRequested.get()) {
            item.status = BatchItemStatus.Canceled.name();
            LOG.debug("Worker canceled during conversion for {}", item.input);
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
        LOG.debug("Worker completed for {} -> {}", item.input, item.outputPath);
    }

    private String pollUntilComplete(String jobId, String exportTaskName) throws Exception {
        int maxPolls = 600;
        for (int i = 0; i < maxPolls && !cancelRequested.get(); i++) {
            CloudConvertFacade.JobResult job = facade.getJob(jobId);
            if (i % 10 == 0) {
                LOG.debug("Polling job {} status={} (poll {}/{})",
                        jobId, job.status(), i + 1, maxPolls);
            }
            if ("finished".equals(job.status())) {
                LOG.debug("Job {} finished", jobId);
                return findExportTaskId(job, exportTaskName);
            }
            if ("error".equals(job.status())) {
                LOG.warn("Job {} failed", jobId);
                throw new RuntimeException("Job failed");
            }
            Thread.sleep(3000);
        }
        LOG.warn("Job {} timed out after {} polls", jobId, maxPolls);
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
                    LOG.debug("Resolved export URL for job {}", item.jobId);
                    return url != null ? url.toString() : null;
                }
            }
        }
        throw new RuntimeException("No export URL in task result");
    }
}
