package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;

import org.jspecify.annotations.Nullable;

import com.jayway.jsonpath.JsonPath;

/**
 * Factory for {@link RequestMatcher} implementations that use a <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression.
 * <p>Accessed via {@link RequestMatchers#jsonPath(String, Object...)}</p>
 * <p>
 * Requires an Entity Provider for {@code application/json} to be present and registered with the JAX-RS client component that executed the request.
 * </p>
 */
public class JsonPathRequestMatchers {

    private final String expression;
    private final JsonPath jsonPath;

    JsonPathRequestMatchers(String expression, Object... args) {
        this.expression = expression.formatted(args);
        this.jsonPath = JsonPath.compile(this.expression);
    }

    /**
     * Evaluate the JsonPath expression and assert the result is equal to and of the same type as the given Object.
     * <p>
     * If the JsonPath expression is not {@linkplain JsonPath#isDefinite() definite}
     * and evaluates to an array containing a <b>single</b> value, it will be compared to the given Object.
     * </p>
     * <p>
     * If the JsonPath expression is not {@linkplain JsonPath#isDefinite() definite}
     * and evaluates to an array containing <b>multiple</b> values, an {@link AssertionError} will be thrown.
     * </p>
     *
     * @param expectedValue The expected value, possibly {@code null}
     */
    public RequestMatcher value(@Nullable Object expectedValue) {
        return request -> {
            String jsonString = getJsonString(request);
            Object value = evaluate(jsonString);
            if (value instanceof List<?> valueList && !(expectedValue instanceof List<?>)) {
                if (valueList.isEmpty()) {
                    throw new AssertionError("Found no value matching " + expectedValue + " at JSON path \"" + expression + "\"");
                }

                if (valueList.size() > 1) {
                    throw new AssertionError("Found list of values " + valueList + " instead of the expected single value " + expectedValue);
                }

                value = valueList.get(0);
            } else if (value != null && expectedValue != null && !value.getClass().equals(expectedValue.getClass())) {
                try {
                    value = evaluate(jsonString, expectedValue.getClass());
                } catch (AssertionError e) {
                    throw new AssertionError(value
                            + " cannot be converted to type "
                            + expectedValue.getClass().getTypeName()
                            + " at JSON path \"" + expression + "\"", e);
                }
            }

            assertEqual("JSON Path \"" + expression + "\"", expectedValue, value);
        };
    }

    /**
     * Evaluate the JsonPath expression and assert the result is a {@code non-null} value.
     * <p>
     * If the JsonPath expression is not {@linkplain JsonPath#isDefinite() definite}
     * this {@code RequestMatcher} asserts that the result is not empty.
     * </p>
     */
    public RequestMatcher exists() {
        return request -> assertExistsAndGet(getJsonString(request));
    }

    /**
     * Evaluate the JsonPath expression and assert the result is {@code null}.
     * <p>
     * If the JsonPath expression is not {@linkplain JsonPath#isDefinite() definite}
     * this {@code RequestMatcher} asserts that the result is empty.
     * </p>
     */
    public RequestMatcher doesNotExist() {
        return request -> {
            Object value;
            try {
                value = evaluate(getJsonString(request));
            } catch (AssertionError e) {
                return;
            }

            String failureMessage = createFailureMessage("no value", value);
            if (!jsonPath.isDefinite() && value instanceof List<?> list) {
                assertTrue(failureMessage, list.isEmpty());
            } else {
                assertTrue(failureMessage, value == null);
            }
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that <b>any value</b>, including {@code null}, exists.
     * <p>
     * If the JsonPath expression is not {@linkplain JsonPath#isDefinite() definite}
     * this {@code RequestMatcher} asserts that the result is not empty.
     * </p>
     */
    public RequestMatcher hasJsonPath() {
        return request -> {
            Object value = evaluate(getJsonString(request));
            if (!jsonPath.isDefinite() && value instanceof List<?> list) {
                assertTrue("No values for JSON Path \"" + expression + "\"", !list.isEmpty());
            }
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that <b>no value</b>, including {@code null}, exists.
     * <p>
     * If the JsonPath expression is not {@linkplain JsonPath#isDefinite() definite}
     * this {@code RequestMatcher} asserts that the result is empty.
     * </p>
     */
    public RequestMatcher doesNotHaveJsonPath() {
        return request -> {
            Object value;
            try {
                value = evaluate(getJsonString(request));
            } catch (AssertionError e) {
                return;
            }

            if (!jsonPath.isDefinite() && value instanceof List<?> list) {
                assertTrue(createFailureMessage("no values", value), list.isEmpty());
            } else {
                String message = createFailureMessage("no value", value);
                throw new AssertionError(message);
            }
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that the result is a {@link String}.
     */
    public RequestMatcher isString() {
        return request -> {
            Object value = assertExistsAndGet(getJsonString(request));
            assertTrue(createFailureMessage("a string", value), value instanceof String);
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that the result is a {@link Boolean}.
     */
    public RequestMatcher isBoolean() {
        return request -> {
            Object value = assertExistsAndGet(getJsonString(request));
            assertTrue(createFailureMessage("a boolean", value), value instanceof Boolean);
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that the result is a {@link Number}.
     */
    public RequestMatcher isNumber() {
        return request -> {
            Object value = assertExistsAndGet(getJsonString(request));
            assertTrue(createFailureMessage("a number", value), value instanceof Number);
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that the result is an {@code Array}.
     */
    public RequestMatcher isArray() {
        return request -> {
            Object value = assertExistsAndGet(getJsonString(request));
            assertTrue(createFailureMessage("an array", value), value instanceof List<?>);
        };
    }

    /**
     * Evaluate the JsonPath expression and assert that the result is a {@link Map}.
     */
    public RequestMatcher isMap() {
        return request -> {
            Object value = assertExistsAndGet(getJsonString(request));
            assertTrue(createFailureMessage("a map", value), value instanceof Map<?, ?>);
        };
    }

    private Object assertExistsAndGet(String jsonString) {
        Object value = evaluate(jsonString);
        String message = "Found no value for JSON path \"" + expression + "\"";
        if (value == null) {
            throw new AssertionError(message);
        }
        if (!jsonPath.isDefinite() && value instanceof List<?> list && list.isEmpty()) {
            throw new AssertionError(message);
        }

        return value;
    }

    private @Nullable Object evaluate(String jsonString) {
        try {
            return jsonPath.read(jsonString);
        } catch (Throwable t) {
            if (t.getMessage() != null && t.getMessage().contains("This is not a json object")) {
                throw t;
            }
            throw new AssertionError("Found no value for JSON path \"" + expression + "\"", t);
        }
    }

    private <T> T evaluate(String jsonString, Class<T> type) {
        try {
            return JsonPath.parse(jsonString).read(expression, type);
        } catch (Throwable t) {
            throw new AssertionError("Failed to evaluate JSON path \"" + expression + "\" with type " + type, t);
        }
    }

    private String createFailureMessage(String description, @Nullable Object value) {
        String valueString = (value instanceof CharSequence) ? ("'" + value + "'") : String.valueOf(value);
        return "Expected %s at JSON Path \"%s\" but found %s".formatted(description, expression, valueString);
    }

    private static String getJsonString(ClientRequestContext requestContext) throws IOException {
        EntityConverter converter = EntityConverter.fromRequestContext(requestContext);
        return converter.convertEntity(requestContext, String.class);
    }
}
