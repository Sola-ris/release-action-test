package io.github.solaris.jaxrs.client.test.request;

/**
 * A type representing a range of times a request is expected to occur.
 */
public final class ExpectedCount {
    private final int min;
    private final int max;

    private ExpectedCount(int min, int max) {
        if (min < 0) {
            throw new IllegalArgumentException("'min' must be >= 0");
        }
        if (max < min) {
            throw new IllegalArgumentException("'max' must be >= 'min'");
        }
        this.min = min;
        this.max = max;
    }

    /**
     * The lower boundary of the expected count range.
     */
    public int getMin() {
        return min;
    }

    /**
     * The upper boundary of the expected count range.
     */
    public int getMax() {
        return max;
    }

    /**
     * No calls expected.
     */
    public static ExpectedCount never() {
        return new ExpectedCount(0, 0);
    }

    /**
     * Exactly one call expected.
     */
    public static ExpectedCount once() {
        return new ExpectedCount(1, 1);
    }

    /**
     * Exactly N calls expected.
     */
    public static ExpectedCount times(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("'count' must be >= 1");
        }
        return new ExpectedCount(count, count);
    }

    /**
     * At least {@code min} amount of times.
     */
    public static ExpectedCount min(int min) {
        if (min < 1) {
            throw new IllegalArgumentException("'min' must be >= 1");
        }
        return new ExpectedCount(min, Integer.MAX_VALUE);
    }

    /**
     * At most {@code max} amount of times.
     */
    public static ExpectedCount max(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("'max' must be >= 1");
        }
        return new ExpectedCount(1, max);
    }

    /**
     * Between {@code min} and {@code max} number of times.
     */
    public static ExpectedCount between(int min, int max) {
        return new ExpectedCount(min, max);
    }
}
