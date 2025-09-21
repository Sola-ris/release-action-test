package io.github.solaris.jaxrs.client.test.response;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.internal.ClientCleaner;

/**
 * A {@link ResponseCreator} that obtains the {@link Response} by calling an external REST service.
 * Intended for scenarios in which only some service calls need to be mocked.
 *
 * <pre><code>
 *  MockRestServer server = MockRestServer.bindTo(client).build();
 *  ResponseCreator withServiceCall = new ExecutingResponseCreator()
 *  //...
 *  server.expect(requestTo("/hello")).andRespond(withSuccess());
 *  server.expect(requestTo("/goodbye")).andRespond(withServiceCall);
 * </code></pre>
 *
 * @see MockResponseCreators
 */
public class ExecutingResponseCreator implements ResponseCreator {
    private final Client client;

    /**
     * Create an instance with a {@link Client} obtained through {@link ClientBuilder#newClient()}.
     * The {@code Client} is registered to a {@link java.lang.ref.Cleaner Cleaner} and closed once it goes out of scope.
     */
    @SuppressWarnings("this-escape")
    public ExecutingResponseCreator() {
        this.client = ClientBuilder.newClient();
        ClientCleaner.register(this, client);
    }

    /**
     * Create an instance with a caller-supplied {@link Client}.
     * The caller is responsible for closing the {@code Client}.
     */
    public ExecutingResponseCreator(Client client) {
        validateNotNull(client, "'client' must not be null.");
        this.client = client;
    }

    @Override
    public Response createResponse(ClientRequestContext request) {
        Invocation.Builder invocationBuilder = client.target(request.getUri())
                .request()
                .headers(request.getHeaders());
        if (request.hasEntity()) {
            return invocationBuilder.method(request.getMethod(), Entity.entity(request.getEntity(), request.getMediaType()));
        }
        return invocationBuilder.method(request.getMethod());
    }
}
