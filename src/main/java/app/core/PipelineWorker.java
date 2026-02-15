package app.core;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the conversion pipeline for a single file. Never runs on FX thread.
 * Output directory is resolved at conversion time, not from BatchItem.
 */
public class PipelineWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineWorker.class);

    private final BatchItem item;
    private final Path outputDir;
    private final CloudConvertFacade facade;
    private final AtomicBoolean cancelRequested;

    public PipelineWorker(BatchItem item, Path outputDir, CloudConvertFacade facade, AtomicBoolean cancelRequested) {
        this.item = item;
        this.outputDir = outputDir;
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
            item.message = ErrorMessages.fromException(e);
            LOG.error("Worker failed for {}: {}", item.input, item.message, e);
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
        Path outputPath = OutputNaming.resolveInDir(item.input, outputDir, item.profile);
        Path tmpDir = outputDir.resolve(".tmp");
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
            if (isStatus(job.status(), "finished")) {
                LOG.debug("Job {} finished", jobId);
                String exportTaskId = findExportTaskId(job, exportTaskName);
                if (exportTaskId == null || exportTaskId.isBlank()) {
                    throw new RuntimeException("Export task not found in finished job");
                }
                return exportTaskId;
            }
            if (isStatus(job.status(), "error")) {
                LOG.warn("Job {} failed", jobId);
                throw new RuntimeException("Job failed");
            }
            Thread.sleep(3000);
        }
        if (cancelRequested.get()) {
            LOG.info("Polling canceled for job {}", jobId);
            return null;
        }
        LOG.warn("Job {} timed out after {} polls", jobId, maxPolls);
        throw new RuntimeException("Job timed out");
    }

    private boolean isStatus(String actualStatus, String expectedStatus) {
        if (actualStatus == null) {
            return false;
        }
        return expectedStatus.equals(actualStatus.toLowerCase(Locale.ROOT));
    }

    private String findExportTaskId(CloudConvertFacade.JobResult job, String exportTaskName) {
        Object tasks = job.tasks();
        if (tasks instanceof List) {
            String exportByOperation = null;
            for (Object t : (List<?>) tasks) {
                String id = readTaskField(t, "id");
                String name = readTaskField(t, "name");
                if (id == null || id.isBlank()) {
                    continue;
                }
                if (exportTaskName.equals(name)) {
                    return id;
                }
                String operation = readTaskField(t, "operation");
                if (isStatus(operation, "export/url")) {
                    exportByOperation = id;
                }
            }
            return exportByOperation;
        }
        return null;
    }

    private String readTaskField(Object task, String key) {
        return readStringField(task, key);
    }

    private String getExportUrl(String exportTaskId) throws Exception {
        CloudConvertFacade.TaskResult task = facade.getTask(item.jobId, exportTaskId);
        String url = extractFirstExportUrl(task.output());
        if (url != null && !url.isBlank()) {
            LOG.debug("Resolved export URL for job {}", item.jobId);
            return url;
        }
        throw new RuntimeException("No export URL in task result");
    }

    private String extractFirstExportUrl(Object taskOutput) {
        Object files = readField(taskOutput, "files");
        if (files instanceof List) {
            for (Object file : (List<?>) files) {
                String url = readStringField(file, "url");
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object readField(Object source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return null;
        }
        if (source instanceof Map) {
            return ((Map<String, Object>) source).get(key);
        }
        try {
            String methodName = "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            Method method = source.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(source);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readStringField(Object source, String key) {
        Object value = readField(source, key);
        return value != null ? value.toString() : null;
    }
}
