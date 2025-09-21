package io.github.solaris.jaxrs.client.test.internal;

import org.jspecify.annotations.Nullable;

public final class ArgumentValidator {
    private ArgumentValidator() {}

    @Contract("null, _ -> fail")
    public static void validateNotNull(@Nullable Object parameter, String message) {
        if (parameter == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void validateNotBlank(@Nullable String parameter, String message) {
        if (parameter == null || parameter.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
