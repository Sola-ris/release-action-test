package io.github.solaris.jaxrs.client.test.response;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Response;

/**
 * <p>A contract for creating a {@link Response} for a given {@link ClientRequestContext}.</p>
 * <p>
 * Implementations can be obtained via {@link MockResponseCreators MockResponseCreators}
 * or {@link ExecutingResponseCreator ExecutingResponseCreator}.
 * </p>
 */
@FunctionalInterface
public interface ResponseCreator {

    /**
     * Create a {@link Response} for the given {@link ClientRequestContext}.
     *
     * @param request The current request
     */
    Response createResponse(ClientRequestContext request) throws IOException;
}
