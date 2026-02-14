package app.core;

import java.nio.file.Path;

/**
 * Represents a single file in a batch conversion.
 */
public final class BatchItem {

    public final Path input;
    public final Path outputDir;
    public final ConversionProfile profile;

    public volatile String status;
    public volatile double progress;
    public volatile String message;
    public volatile Path outputPath;

    public volatile String jobId;
    public volatile String uploadTaskId;
    public volatile String exportTaskId;

    public BatchItem(Path input, Path outputDir, ConversionProfile profile) {
        this.input = input;
        this.outputDir = outputDir;
        this.profile = profile;
        this.status = BatchItemStatus.Queued.name();
        this.progress = 0.0;
        this.message = "";
    }
}
