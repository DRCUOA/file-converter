package app.core;

/**
 * Status of a batch item through the conversion pipeline.
 */
public enum BatchItemStatus {
    Queued,
    Uploading,
    Converting,
    Exporting,
    Downloading,
    Saving,
    Done,
    Failed,
    Skipped,
    Canceled
}
