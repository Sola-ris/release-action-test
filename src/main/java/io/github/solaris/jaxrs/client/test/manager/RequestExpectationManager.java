package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.request.ExpectedCount;
import io.github.solaris.jaxrs.client.test.request.RequestMatcher;
import io.github.solaris.jaxrs.client.test.response.ResponseActions;

/**
 * Base class for all {@code RequestExpectationManager} implementations.
 * <p>Responsible for holding expectations, executed and failed requests, as well as checking for unsatisfied request expectations.</p>
 * <p>The order in which the requests are expected to occur depends on the subclass.</p>
 */
public abstract class RequestExpectationManager {
    private final List<RequestExpectation> expectations = new ArrayList<>();
    private final List<ClientRequestContext> requests = new ArrayList<>();
    private final Map<ClientRequestContext, Throwable> failedRequests = new LinkedHashMap<>();

    private boolean expectationsDeclared = false;

    RequestExpectationManager() {}

    abstract void expectationsDeclared();

    abstract RequestExpectation matchRequest(ClientRequestContext requestContext) throws IOException;

    List<RequestExpectation> getExpectations() {
        return expectations;
    }

    /**
     * @see io.github.solaris.jaxrs.client.test.server.MockRestServer#expect(RequestMatcher) MockRestServer.expect(RequestMatcher)
     * @see io.github.solaris.jaxrs.client.test.server.MockRestServer#expect(ExpectedCount, RequestMatcher) MockRestServer.expect(ExpectedCount, RequestMatcher)
     */
    public ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
        if (expectationsDeclared) {
            throw new IllegalStateException("Cannot declare further expectations after the first request.");
        }

        RequestExpectation expectation = new RequestExpectation(count, requestMatcher);
        expectations.add(expectation);
        return expectation;
    }

    /**
     * Validate the incoming request against the set-up expectations and respond if a match was found.
     *
     * @param requestContext The incoming request
     * @return The set-up {@link Response}
     * @throws IOException If thrown from a {@link RequestMatcher}
     */
    public Response validateRequest(ClientRequestContext requestContext) throws IOException {
        RequestExpectation expectation;
        synchronized (requests) {
            if (requests.isEmpty()) {
                expectationsDeclared = true;
                expectationsDeclared();
            }

            try {
                expectation = matchRequest(requestContext);
            } catch (Throwable t) {
                failedRequests.put(requestContext, t);
                throw t;
            } finally {
                requests.add(requestContext);
            }
        }

        return expectation.createResponse(requestContext);
    }

    /**
     * @see io.github.solaris.jaxrs.client.test.server.MockRestServer#verify() MockRestServer.verify()
     */
    public void verify() {
        long unsatisfied = countUnsatisfiedExpectations();
        if (unsatisfied != 0) {
            String builder = "Further request(s) expected leaving "
                    + unsatisfied
                    + " unsatisfied expectation(s).\n"
                    + getRequestDetails();
            throw new AssertionError(builder);
        }
    }

    /**
     * @see io.github.solaris.jaxrs.client.test.server.MockRestServer#verify(Duration) MockRestServer.verify(Duration)
     */
    public void verify(Duration timeout) {
        Instant end = Instant.now().plus(timeout);
        do {
            if (countUnsatisfiedExpectations() == 0) {
                return;
            }
        }
        while (Instant.now().isBefore(end));

        verify();
    }

    /**
     * @see io.github.solaris.jaxrs.client.test.server.MockRestServer#reset() MockRestServer.reset()
     */
    public void reset() {
        requests.clear();
        expectations.clear();
        failedRequests.clear();
        expectationsDeclared = false;
    }

    private long countUnsatisfiedExpectations() {
        if (expectations.isEmpty()) {
            return 0;
        }

        if (!failedRequests.isEmpty()) {
            throw new AssertionError("Some requests did not execute successfully.\n" +
                    failedRequests.entrySet().stream()
                            .map(entry -> "Failed request:\n" + contextToString(entry.getKey()) + "\n" + entry.getValue())
                            .collect(Collectors.joining("\n", "\n", "")));
        }

        return expectations.stream()
                .filter(expectation -> !expectation.isSatisfied())
                .count();
    }

    AssertionError createUnexpectedRequestError(ClientRequestContext requestContext) {
        String method = requestContext.getMethod();
        URI uri = requestContext.getUri();
        return new AssertionError("No further requests expected: HTTP " + method + " " + uri + "\n" + getRequestDetails());
    }

    private String getRequestDetails() {
        StringBuilder builder = new StringBuilder();
        builder.append(requests.size()).append(" request(s) executed");
        if (!requests.isEmpty()) {
            builder.append(":\n");
            for (ClientRequestContext request : requests) {
                builder.append(contextToString(request)).append('\n');
            }
        } else {
            builder.append(".\n");
        }

        return builder.toString();
    }

    private static String contextToString(ClientRequestContext requestContext) {
        StringBuilder builder = new StringBuilder(requestContext.getMethod()).append(' ').append(requestContext.getUri());
        if (!requestContext.getStringHeaders().isEmpty()) {
            builder.append(", headers: ").append(requestContext.getStringHeaders());
        }
        return builder.toString();
    }
}
