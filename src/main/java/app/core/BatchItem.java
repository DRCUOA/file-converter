package app.core;

import java.nio.file.Path;

/**
 * Represents a single file in a batch conversion.
 * Output directory is resolved at conversion time, not at creation.
 */
public final class BatchItem {

    public final Path input;
    public final ConversionProfile profile;

    public volatile String status;
    public volatile double progress;
    public volatile String message;
    public volatile Path outputPath;

    public volatile String jobId;
    public volatile String uploadTaskId;
    public volatile String exportTaskId;

    public BatchItem(Path input, ConversionProfile profile) {
        this.input = input;
        this.profile = profile;
        this.status = BatchItemStatus.Queued.name();
        this.progress = 0.0;
        this.message = "";
    }
}
