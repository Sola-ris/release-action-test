package io.github.solaris.jaxrs.client.test.request;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;

/**
 * <p>A contract for matching requests to expectations.</p>
 * <p>Built-in implementations can be obtained via {@link RequestMatchers}</p>
 *
 * @see RequestMatchers
 * @see EntityRequestMatchers
 * @see JsonPathRequestMatchers
 * @see XpathRequestMatchers
 */
@FunctionalInterface
public interface RequestMatcher {

    /**
     * Match the given {@link ClientRequestContext} against specific expectations.
     *
     * @param request The current request to match on
     * @throws IOException    In case of an I/O error
     * @throws AssertionError If the request does not match
     */
    void match(ClientRequestContext request) throws IOException, AssertionError;
}
