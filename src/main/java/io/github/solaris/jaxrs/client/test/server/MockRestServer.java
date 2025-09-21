package io.github.solaris.jaxrs.client.test.server;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;

import java.time.Duration;

import jakarta.ws.rs.core.Configurable;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.request.ExpectedCount;
import io.github.solaris.jaxrs.client.test.request.RequestMatcher;
import io.github.solaris.jaxrs.client.test.response.ResponseActions;

/**
 * Entry point into the library.
 * <p>
 * Used for tests that directly or indirectly require the use of a JAX-RS Client component like
 * {@link jakarta.ws.rs.client.ClientBuilder ClientBuilder}, {@link jakarta.ws.rs.client.Client Client}
 * or {@link jakarta.ws.rs.client.WebTarget WebTarget}. It provides a way to set up which requests will be performed
 * through ht JAX-RS Client Component as well as set up mock responses to them,
 * removing the need for starting an actual server.
 * </p>
 * <p>Example of a test scenario involving an indirect use of a client component:</p>
 * <pre><code>
 * class CancellationService {
 *     private final WebTarget userClient;
 *
 *     CancellationService(WebTarget userClient) {
 *         this.userClient = userClient;
 *     }
 *
 *     void startCancellationProcess(int userId) {
 *         Response getUserResponse = userClient.path(userId).request().get():
 *         if (getUserResponse.getStatus() == 404) {
 *             throw new UserNotFoundException();
 *         } else {
 *             // start cancellation process
 *         }
 *     }
 * }
 *
 * class CancellationServiceTest {
 *
 *     &#64;Test
 *     void testUserNotFound() {
 *          WebTarget target = ClientBuilder.newClient().target("");
 *          MockRestServer server = MockRestServer.bindTo(target).build();
 *
 *          server.expect(RequestMatchers.requestTo("/42"))
 *              .andRespond(MockResponseCreators.withNotFound());
 *
 *          CancellationService service = new CancellationService(target);
 *
 *          assertThrows(UserNotFoundException.class,
 *              () -> service.startCancellationProcess(42));
 *
 *          server.verify();
 *     }
 * }
 * </code></pre>
 */
public final class MockRestServer {
    private final RequestExpectationManager expectationManager;

    MockRestServer(RequestExpectationManager expectationManager) {
        this.expectationManager = expectationManager;
    }

    /**
     * <p>Bind a {@link Configurable Configurable} JAX-RS client component to the {@link MockRestServer}.</p>
     * Supported {@code Configurables}
     * <ul>
     *     <li>{@link jakarta.ws.rs.client.ClientBuilder ClientBuilder}</li>
     *     <li>{@link jakarta.ws.rs.client.Client Client}</li>
     *     <li>{@link jakarta.ws.rs.client.WebTarget WebTarget}</li>
     *     <li>Microprofile {@link org.eclipse.microprofile.rest.client.RestClientBuilder RestClientBuilder}</li>
     * </ul>
     *
     * @param configurable The component to bind
     * @return A {@link MockRestServerBuilder} to allow for further customization of the {@code MockRestServer}
     */
    public static MockRestServerBuilder bindTo(Configurable<?> configurable) {
        validateNotNull(configurable, "JAX-RS client component must be null.");
        return new MockRestServerBuilder(configurable);
    }

    /**
     * <p>Set up an expectation for a single HTTP request to be performed <b>exactly once</b>.</p>
     * <p>Further expectations for the request, as well as the response can be defined via the returned {@link ResponseActions}.</p>
     * <p>
     * This method may be invoked any number of times before the first request made by the bound JAX-RS client component
     * in order to set up all expected requests.
     * </p>
     */
    public ResponseActions expect(RequestMatcher requestMatcher) {
        return expect(ExpectedCount.once(), requestMatcher);
    }

    /**
     * <p>Set up an expectation for a single HTTP request to be performed <b>a specified amount of times</b>.</p>
     * <p>
     * By default, only the first invocation is expected to occur in the order of declaration.
     * Subsequent request executions may occur in any order.
     * This behavior can be changed through {@link MockRestServerBuilder#withRequestOrder(RequestOrder)}.
     * </p>
     */
    public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
        return expectationManager.expectRequest(count, matcher);
    }

    /**
     * Verify that all the set-up request expectations were satisfied.
     */
    public void verify() {
        expectationManager.verify();
    }

    /**
     * Verify that all the set-up request expectations were satisfied in the given {@link Duration}.
     * Intended for scenarios involving asynchronous requests.
     *
     * @param timeout How long to wait for all expectations to be satisfied
     */
    public void verify(Duration timeout) {
        expectationManager.verify(timeout);
    }

    /**
     * Remove all expectations, received and failed requests.
     */
    public void reset() {
        expectationManager.reset();
    }
}
