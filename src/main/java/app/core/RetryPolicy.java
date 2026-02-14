package app.core;

/**
 * Retry policy for transient failures.
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final long initialBackoffMs;

    public RetryPolicy(int maxRetries, long initialBackoffMs) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(1, 1000);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long backoffForAttempt(int attempt) {
        return initialBackoffMs * (1L << Math.min(attempt, 5));
    }
}
