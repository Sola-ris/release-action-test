package io.github.solaris.jaxrs.client.test.internal;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Simple assertions to not have a dependency on external assertion libraries.
 */
public final class Assertions {
    private Assertions() {}

    public static void assertEqual(String message, @Nullable Object expected, @Nullable Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    @Contract("_, false -> fail")
    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
