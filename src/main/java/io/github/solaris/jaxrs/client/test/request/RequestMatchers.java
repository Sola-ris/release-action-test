package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertTrue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Static factory methods for the built-in {@link RequestMatcher} implementations.
 */
public final class RequestMatchers {
    private static final MultivaluedMap<String, String> EMPTY_QUERY_PARAMS = new MultivaluedHashMap<>();

    private RequestMatchers() {}

    /**
     * Match any request.
     */
    public static RequestMatcher anything() {
        return request -> {};
    }

    /**
     * Match the request method.
     *
     * @param httpMethod The HTTP method / verb
     * @see jakarta.ws.rs.HttpMethod HttpMethod
     */
    public static RequestMatcher method(String httpMethod) {
        validateNotNull(httpMethod, "'httpMethod' must not be null.");
        return request -> assertEqual("Unexpected Method.", httpMethod, request.getMethod());
    }

    /**
     * Match the request URI to the given string.
     *
     * @param uri The expected URI string
     */
    public static RequestMatcher requestTo(String uri) {
        validateNotNull(uri, "'uri' must not be null.");
        return request -> assertEqual("Unexpected Request.", URI.create(uri), request.getUri());
    }

    /**
     * Match the request URI to the given URI.
     *
     * @param uri The expected URI
     */
    public static RequestMatcher requestTo(URI uri) {
        validateNotNull(uri, "'uri' must not be null.");
        return request -> assertEqual("Unexpected Request.", uri, request.getUri());
    }

    /**
     * <p>Assert the values of a single query parameter</p>
     * <p>If the list of query parameter values is longer than {@code expectedValues}, all additional values will be ignored.</p>
     * <p>If {@code expectedValues} is longer than the list of query parameter values, an {@link AssertionError} will be thrown.</p>
     *
     * @param name           The name of the query parameter whose existence and value(s) to assert
     * @param expectedValues The expected values of the query parameter, in order.
     *                       <p>The n<sup>th</sup> expected value is compared to the n<sup>th</sup> query parameter value</p>
     */
    public static RequestMatcher queryParam(String name, String... expectedValues) {
        validateNotNull(name, "'name' must not be null.");
        validateNotNull(expectedValues, "'expectedValues' must not be null.");
        return request -> {
            MultivaluedMap<String, String> queryParams = getQueryParams(request.getUri());

            String message = "Expected QueryParam <" + name + ">";
            assertTrue(message + " to exist but was null", queryParams.get(name) != null);
            assertTrue(message + " to have at least <" + expectedValues.length + "> values but found " + queryParams.get(name),
                    expectedValues.length <= queryParams.get(name).size());

            for (int i = 0; i < expectedValues.length; i++) {
                assertEqual("QueryParam [name=" + name + ", position=" + i + "]", expectedValues[i], queryParams.get(name).get(i));
            }
        };
    }

    /**
     * Assert that the given query parameter is not present in the request URI.
     *
     * @param name The name of query parameter
     */
    public static RequestMatcher queryParamDoesNotExist(String name) {
        validateNotNull(name, "'name' must not be null.");
        return request -> {
            List<String> queryParamsValues = getQueryParams(request.getUri()).get(name);
            if (queryParamsValues != null) {
                throw new AssertionError("Expected QueryParam <" + name + "> to not exist, but it exists with values: " + queryParamsValues);
            }
        };
    }

    /**
     * Assert that the given number of query parameters are present in the request URI.
     *
     * @param expectedCount The expected number of query parameters
     */
    public static RequestMatcher queryParamCount(int expectedCount) {
        return request -> {
            Set<String> queryParamNames = getQueryParams(request.getUri()).keySet();
            int actualSize = queryParamNames.size();
            if (expectedCount != actualSize) {
                throw new AssertionError("Expected %s QueryParams but found %s: %s".formatted(expectedCount, actualSize, queryParamNames));
            }
        };
    }

    /**
     * <p>Assert the values of a single request header</p>
     * <p>If the list of header values is longer than {@code expectedValues}, all additional values will be ignored.</p>
     * <p>If {@code expectedValues} is longer than the list of header values, an {@link AssertionError} will be thrown.</p>
     *
     * @param name           The name of the header whose existence and value(s) to assert
     * @param expectedValues The expected values of the header, in order.
     *                       <p>The n<sup>th</sup> expected value is compared to the n<sup>th</sup> header value</p>
     */
    public static RequestMatcher header(String name, String... expectedValues) {
        validateNotNull(name, "'name' must not be null.");
        validateNotNull(expectedValues, "'expectedValues' must not be null.");
        return request -> {
            List<String> actualValues = request.getStringHeaders().get(name);

            String message = "Expected header <" + name + ">";
            assertTrue(message + " to exist but was null", actualValues != null);
            assertTrue(message + " to have at least <" + expectedValues.length + "> values but found " + actualValues,
                    expectedValues.length <= actualValues.size());

            for (int i = 0; i < expectedValues.length; i++) {
                assertEqual("Request header [name=" + name + ", position=" + i + "]", expectedValues[i], actualValues.get(i));
            }
        };
    }

    /**
     * Assert that the given header is not present in the request.
     *
     * @param name The name of the header
     */
    public static RequestMatcher headerDoesNotExist(String name) {
        validateNotNull(name, "'name' must not be null.");
        return request -> {
            List<Object> headerValues = request.getHeaders().get(name);
            if (headerValues != null) {
                throw new AssertionError("Expected header <" + name + "> to not exist, but it exists with values: " + headerValues);
            }
        };
    }

    /**
     * Access to request entity / body matchers.
     */
    public static EntityRequestMatchers entity() {
        return new EntityRequestMatchers();
    }

    /**
     * Access to request body matchers using a <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
     * to inspect a specific subset of the body.
     * <p>The JSON path expression can be parameterized using formatting specifiers as defined in{@link String#format(String, Object...)}</p>
     *
     * @param expression The JSON path expression, possibly parameterized
     * @param args       Arguments to parameterize the JSON path expression with
     */
    public static JsonPathRequestMatchers jsonPath(String expression, Object... args) {
        return new JsonPathRequestMatchers(expression, args);
    }

    /**
     * Access to request body matchers using an {@link javax.xml.xpath.XPath XPath} expression
     * to inspect a specific subset of the body.
     * <p>The XPath path expression can be parameterized using formatting specifiers as defined in{@link String#format(String, Object...)}</p>
     *
     * @param expression The XPath path expression, possibly parameterized
     * @param args       Arguments to parameterize the XPath path expression with
     * @throws XPathExpressionException On invalid XPath expressions
     */
    public static XpathRequestMatchers xpath(String expression, Object... args) throws XPathExpressionException {
        return new XpathRequestMatchers(expression, emptyMap(), args);
    }

    /**
     * Access to request body matchers using a <b>namespace-aware</b> {@link javax.xml.xpath.XPath XPath} expression
     * to inspect a specific subset of the body.
     * <p>The XPath path expression can be parameterized using formatting specifiers as defined in{@link String#format(String, Object...)}</p>
     *
     * @param expression The XPath path expression, possibly parameterized
     * @param namespaces The namespaces referenced in the XPath expression
     * @param args       Arguments to parameterize the XPath path expression with
     * @throws XPathExpressionException On invalid XPath expressions
     */
    public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args) throws XPathExpressionException {
        return new XpathRequestMatchers(expression, namespaces, args);
    }

    private static MultivaluedMap<String, String> getQueryParams(URI uri) {
        if (uri.getRawQuery() == null || uri.getRawQuery().isEmpty()) {
            return EMPTY_QUERY_PARAMS;
        }

        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(query -> query.split("=", 2))
                .collect(MultivaluedHashMap::new, (map, query) -> map.add(
                        URLDecoder.decode(query[0], UTF_8),
                        query.length == 2 ? URLDecoder.decode(query[1], UTF_8) : ""
                ), MultivaluedMap::putAll);
    }
}
