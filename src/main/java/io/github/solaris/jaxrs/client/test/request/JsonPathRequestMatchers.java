package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotBlank;
import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;

import org.jspecify.annotations.Nullable;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * Factory for {@link RequestMatcher} implementations that use a <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression.
 * <p>Accessed via {@link RequestMatchers#jsonPath(String, Object...)}</p>
 * <p>
 * Requires an Entity Provider for {@code application/json} to be present and registered with the JAX-RS client component that executed the request.
 * </p>
 */
public final class JsonPathRequestMatchers {
    private static final Supplier<MappingProvider> JACKSON_SUPPLIER = JacksonMappingProvider::new;

    private final String expression;
    private final JsonPath jsonPath;

    JsonPathRequestMatchers(String expression, Object... args) {
        validateNotBlank(expression, "JsonPath expression must not be null or blank.");
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
     * <h4>Note:</h4>
     * If {@code expectedValue} is a {@code record}, Jackson must be available at runtime.
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

    /**
     * Evaluate the JsonPath expression, convert it into {@code targetType} and assert the resulting value with the supplied assertion
     *
     * @param valueAssertion An arbitrary assertion with which to assert the resulting value
     * @param targetType     The expected type of the resulting value
     * @param <T>            The expected type of the resulting value. Possibly null.
     *                       <h4>Note:</h4>
     *                       If {@code <T>} is a {@code record}, Jackson must be available at runtime.
     */
    public <T extends @Nullable Object> RequestMatcher valueSatisfies(ThrowingConsumer<T> valueAssertion, Class<T> targetType) {
        validateNotNull(valueAssertion, "'valueAssertion' must not be null.");
        validateNotNull(targetType, "'targetType' must not be null.");
        return request -> {
            try {
                valueAssertion.accept(evaluate(getJsonString(request), targetType));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        };
    }

    /**
     * Evaluate the JsonPath expression, convert it into {@code targetType} and assert the resulting value with the supplied assertion
     *
     * @param valueAssertion An arbitrary assertion with which to assert the resulting value
     * @param targetType     The expected generic type of the resulting value
     * @param <T>            The expected generic type of the resulting value. Possibly null.
     *                       <h4>Note:</h4>
     *                       This {@link RequestMatcher} requires Jackson to be available at runtime.
     */
    public <T extends @Nullable Object> RequestMatcher valueSatisfies(ThrowingConsumer<T> valueAssertion, GenericType<T> targetType) {
        validateNotNull(valueAssertion, "'valueAssertion' must not be null.");
        validateNotNull(targetType, "'targetType' must not be null.");
        return request -> {
            try {
                valueAssertion.accept(evaluate(getJsonString(request), targetType));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
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

    private <T extends @Nullable Object> T evaluate(String jsonString, Class<T> type) {
        try {
            if (type.isRecord()) {
                return JsonPath.parse(jsonString, getJacksonConfiguration()).read(expression, type);
            } else {
                return JsonPath.parse(jsonString).read(expression, type);
            }
        } catch (Throwable t) {
            throw new AssertionError("Failed to evaluate JSON path \"" + expression + "\" with type " + type, t);
        }
    }

    private <T extends @Nullable Object> T evaluate(String jsonString, GenericType<T> type) {
        try {
            return JsonPath.parse(jsonString, getJacksonConfiguration()).read(expression, new TypeRefAdapter<>(type));
        } catch (Throwable t) {
            throw new AssertionError("Failed to evaluate JSON path \"" + expression + "\" with type " + type, t);
        }
    }

    private static Configuration getJacksonConfiguration() {
        try {
            return Configuration.defaultConfiguration().mappingProvider(JACKSON_SUPPLIER.get());
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to load Jackson.", e);
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

    private static final class TypeRefAdapter<T> extends TypeRef<T> {
        private final Type type;

        private TypeRefAdapter(GenericType<T> genericType) {
            this.type = genericType.getType();
        }

        @Override
        public Type getType() {
            return type;
        }
    }
}
