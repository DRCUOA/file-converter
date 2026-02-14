package app.core;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs batch conversions with configurable concurrency.
 */
public class BatchRunner {

    private final CloudConvertFacade facade;
    private final int concurrency;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public BatchRunner(CloudConvertFacade facade, int concurrency) {
        this.facade = facade;
        this.concurrency = Math.max(1, concurrency);
    }

    public void run(List<BatchItem> items) {
        cancelRequested.set(false);
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        try {
            for (BatchItem item : items) {
                if (cancelRequested.get()) {
                    break;
                }
                Validation.ValidationResult result = Validation.validate(item);
                if (!result.valid()) {
                    item.status = BatchItemStatus.Skipped.name();
                    item.message = result.message();
                    continue;
                }
                pool.submit(new PipelineWorker(item, facade, cancelRequested));
            }
        } finally {
            pool.shutdown();
        }
        try {
            pool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void cancel() {
        cancelRequested.set(true);
    }
}
