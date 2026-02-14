package app.core;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Thin wrapper around CloudConvert SDK for upload, job creation, polling, download.
 */
public interface CloudConvertFacade {

    String createJobForFile(String uploadTaskName, String convertTaskName, String exportTaskName,
            ConversionProfile profile) throws Exception;

    TaskResult createUploadTaskAndUpload(Path file) throws Exception;

    JobResult getJob(String jobId) throws Exception;

    TaskResult getTask(String jobId, String taskId) throws Exception;

    InputStream download(String url) throws Exception;

    void cancelTask(String jobId, String taskId) throws Exception;

    void cancelJob(String jobId) throws Exception;

    record TaskResult(String taskId, String status, Object output) {
    }

    record JobResult(String status, Object tasks) {
    }
}
