package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Factory for {@link RequestMatcher} implementations related to the request {@code entity}.
 * <p>Accessed via {@link RequestMatchers#entity()}.</p>
 */
public final class EntityRequestMatchers {

    EntityRequestMatchers() {}

    /**
     * Assert that the requests {@code Content-Type} header matches the given String
     *
     * @param mediaType The expected content of the {@code Content-Type} header
     */
    public RequestMatcher mediaType(String mediaType) {
        return mediaType(MediaType.valueOf(mediaType));
    }

    /**
     * Assert that the requests {@code Content-Type} header matches the given {@link MediaType}
     *
     * @param mediaType The expected content of the {@code Content-Type} header
     */
    public RequestMatcher mediaType(MediaType mediaType) {
        return request -> {
            MediaType actual = request.getMediaType();
            assertTrue("MediaType was not set.", actual != null);
            assertEqual("MediaType", mediaType, actual);
        };
    }

    /**
     * Compare the request entity to the given Object.
     *
     * @param expected The expected request entity
     */
    public RequestMatcher isEqualTo(Object expected) {
        return request -> assertEqual("Entity", expected, request.getEntity());
    }

    /**
     * Convert the request body into a String and compare it to the given String.
     *
     * @param expected The expected request body
     */
    public RequestMatcher string(String expected) {
        return request -> {
            EntityConverter entityConverter = EntityConverter.fromRequestContext(request);
            String actual = entityConverter.convertEntity(request, String.class);
            assertEqual("Entity String", expected, actual);
        };
    }

    /**
     * Convert the request entity into a {@link Form} and compare it to the given {@code Form}.
     *
     * @param expectedForm The expected request body
     */
    public RequestMatcher form(Form expectedForm) {
        return request -> {
            EntityConverter entityConverter = EntityConverter.fromRequestContext(request);
            Form form = entityConverter.convertEntity(request, Form.class);
            assertEqual("Form", expectedForm.asMap(), form.asMap());
        };
    }

    /**
     * <p>Convert the request entity into a {@link Form} and assert that it contains the given subset.</p>
     * <p>If the given subset contains more values for a key than the request, an {@link AssertionError} will be thrown.</p>
     * <p>
     * For each key in the expected subset, the n<sup>th</sup> value will be compared to the n<sup>th</sup> value of the request {@code Form}.
     * Any additional values for the key in the request {@code Form} will be ignored.
     * </p>
     *
     * @param expectedForm The expected subset
     */
    public RequestMatcher formContains(Form expectedForm) {
        return request -> {
            EntityConverter entityConverter = EntityConverter.fromRequestContext(request);
            MultivaluedMap<String, String> expectedMap = expectedForm.asMap();
            MultivaluedMap<String, String> actualMap = entityConverter.convertEntity(request, Form.class).asMap();

            assertTrue("Expected " + expectedMap + " to be smaller or the same size as " + actualMap, expectedMap.size() <= actualMap.size());
            for (Map.Entry<String, List<String>> entry : expectedMap.entrySet()) {
                String name = entry.getKey();
                List<String> values = entry.getValue();

                assertTrue("Expected " + actualMap + " to contain parameter '" + name + "'", actualMap.get(name) != null);
                assertTrue("Expected " + values + " to be smaller or the same size as " + actualMap.get(name),
                        values.size() <= actualMap.get(name).size());
                for (int i = 0; i < values.size(); i++) {
                    assertEqual("FormParam [name=" + name + ", position=" + i + "]", values.get(i), actualMap.get(name).get(i));
                }
            }
        };
    }

    /**
     * Convert the request entity into a {@link List} of {@link EntityPart EntityParts}, buffer them
     * and assert that it contains the given subset.
     *
     * @param expectedEntityParts The expected request entity
     */
    public RequestMatcher multipartForm(List<EntityPart> expectedEntityParts) {
        return request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            List<EntityPart> expectedParts = converter.bufferExpectedMultipart(expectedEntityParts);
            List<EntityPart> actualParts = converter.bufferMultipartRequest(request);

            assertEqual("Multipart Form", expectedParts, actualParts);
        };
    }

    /**
     * Convert the request entity into a {@link List} of {@link EntityPart EntityParts}, buffer them
     * and compare them to the given {@code EntityParts}.
     *
     * <p>If the given subset contains more values than the request, an AssertionError will be thrown.</p>
     *
     * @param expectedEntityParts The expected subset
     */
    public RequestMatcher multipartFormContains(List<EntityPart> expectedEntityParts) {
        return request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            List<EntityPart> expectedParts = converter.bufferExpectedMultipart(expectedEntityParts);
            List<EntityPart> actualParts = converter.bufferMultipartRequest(request);

            assertTrue("Expected " + expectedParts + " to be smaller or the same size as " + actualParts,
                    expectedParts.size() <= actualParts.size());
            assertTrue("Expected " + actualParts + " to contain all of " + expectedParts, actualParts.containsAll(expectedParts));
        };
    }
}
