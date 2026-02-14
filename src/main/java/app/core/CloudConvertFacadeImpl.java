package app.core;

import com.cloudconvert.client.CloudConvertClient;
import com.cloudconvert.client.setttings.StringSettingsProvider;
import com.cloudconvert.dto.request.ConvertFilesTaskRequest;
import com.cloudconvert.dto.request.TaskRequest;
import com.cloudconvert.dto.request.UploadImportRequest;
import com.cloudconvert.dto.request.UrlExportRequest;
import com.cloudconvert.dto.response.JobResponse;
import com.cloudconvert.dto.response.TaskResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * CloudConvert SDK implementation. Requires API key in settings.
 */
public class CloudConvertFacadeImpl implements CloudConvertFacade {

    private final CloudConvertClient client;

    public CloudConvertFacadeImpl(String apiKey) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API key not configured");
        }
        this.client = new CloudConvertClient(
                new StringSettingsProvider(apiKey, "", false));
    }

    @Override
    public String createJobForFile(String uploadTaskId, String convertTaskName, String exportTaskName,
            ConversionProfile profile) throws Exception {
        ConvertFilesTaskRequest convertReq = new ConvertFilesTaskRequest()
                .setInput(uploadTaskId)
                .setInputFormat(profile.inputFormat())
                .setOutputFormat(profile.outputFormat());
        for (Map.Entry<String, Object> e : profile.convertOptions().entrySet()) {
            convertReq.set(e.getKey(), e.getValue());
        }
        UrlExportRequest exportReq = new UrlExportRequest().setInput(convertTaskName);
        Map<String, TaskRequest> tasks = new HashMap<>();
        tasks.put(convertTaskName, convertReq);
        tasks.put(exportTaskName, exportReq);
        JobResponse job = client.jobs().create(tasks).getBody();
        return job.getId();
    }

    @Override
    public TaskResult createUploadTaskAndUpload(Path file) throws Exception {
        String filename = file.getFileName().toString();
        try (InputStream is = Files.newInputStream(file)) {
            TaskResponse resp = client.importUsing()
                    .upload(new UploadImportRequest(), is, filename)
                    .getBody();
            return new TaskResult(resp.getId(),
                    resp.getStatus() != null ? resp.getStatus().toString() : "",
                    resp.getResult());
        }
    }

    @Override
    public JobResult getJob(String jobId) throws Exception {
        JobResponse job = client.jobs().show(jobId).getBody();
        return new JobResult(job.getStatus() != null ? job.getStatus().toString() : "",
                job.getTasks());
    }

    @Override
    public TaskResult getTask(String jobId, String taskId) throws Exception {
        TaskResponse task = client.tasks().show(taskId).getBody();
        return new TaskResult(task.getId(),
                task.getStatus() != null ? task.getStatus().toString() : "",
                task.getResult());
    }

    @Override
    public InputStream download(String url) throws Exception {
        return client.files().download(url).getBody();
    }

    @Override
    public void cancelTask(String jobId, String taskId) throws Exception {
        client.tasks().delete(taskId);
    }

    @Override
    public void cancelJob(String jobId) throws Exception {
        client.jobs().delete(jobId);
    }
}
