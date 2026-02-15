package app.core;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs batch conversions with configurable concurrency.
 */
public class BatchRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRunner.class);

    private final CloudConvertFacade facade;
    private final int concurrency;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public BatchRunner(CloudConvertFacade facade, int concurrency) {
        this.facade = facade;
        this.concurrency = Math.max(1, concurrency);
    }

    public void run(List<BatchItem> items) {
        cancelRequested.set(false);
        LOG.debug("Batch run started: items={}, concurrency={}", items.size(), concurrency);
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        try {
            for (BatchItem item : items) {
                if (cancelRequested.get()) {
                    LOG.info("Batch run stopping due to cancellation");
                    break;
                }
                Validation.ValidationResult result = Validation.validate(item);
                if (!result.valid()) {
                    item.status = BatchItemStatus.Skipped.name();
                    item.message = result.message();
                    LOG.debug("Skipping invalid item {}: {}",
                            item.input != null ? item.input.getFileName() : "<null>", result.message());
                    continue;
                }
                pool.submit(new PipelineWorker(item, facade, cancelRequested));
                LOG.debug("Submitted item {}", item.input != null ? item.input.getFileName() : "<null>");
            }
        } finally {
            pool.shutdown();
        }
        try {
            pool.awaitTermination(24, TimeUnit.HOURS);
            LOG.debug("Batch run finished");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Batch run interrupted", e);
        }
    }

    public void cancel() {
        cancelRequested.set(true);
        LOG.info("Cancel flag set for batch");
    }
}
