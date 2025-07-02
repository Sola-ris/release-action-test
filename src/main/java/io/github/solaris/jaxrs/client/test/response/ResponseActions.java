package io.github.solaris.jaxrs.client.test.response;

import io.github.solaris.jaxrs.client.test.request.RequestMatcher;

/**
 * <p>Contract for adding further {@link RequestMatcher RequestMatcher} to a request expectation as well as defining a response</p>
 * <p>
 * Implementations can be obtained via
 * {@link io.github.solaris.jaxrs.client.test.server.MockRestServer#expect(RequestMatcher) MockRestServer.expect(RequestMatcher)} or
 * {@link io.github.solaris.jaxrs.client.test.server.MockRestServer#expect(io.github.solaris.jaxrs.client.test.request.ExpectedCount, RequestMatcher)
 * MockRestServer.expect(ExpectedCount, RequestMatcher)}
 * </p>
 */
public interface ResponseActions {

    /**
     * Add a matching criteria to the expectation.
     *
     * @param requestMatcher The matching criteria
     * @return The expectation
     */
    ResponseActions andExpect(RequestMatcher requestMatcher);

    /**
     * Define the response for the expectation
     *
     * @param responseCreator The creator of the response
     */
    void andRespond(ResponseCreator responseCreator);
}
