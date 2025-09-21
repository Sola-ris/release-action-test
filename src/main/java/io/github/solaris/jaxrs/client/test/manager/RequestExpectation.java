package io.github.solaris.jaxrs.client.test.manager;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Response;

import org.jspecify.annotations.Nullable;

import io.github.solaris.jaxrs.client.test.request.ExpectedCount;
import io.github.solaris.jaxrs.client.test.request.RequestMatcher;
import io.github.solaris.jaxrs.client.test.response.ResponseActions;
import io.github.solaris.jaxrs.client.test.response.ResponseCreator;

class RequestExpectation implements RequestMatcher, ResponseActions, ResponseCreator {

    private int matchedCount;

    private @Nullable ResponseCreator responseCreator;

    private final List<RequestMatcher> matchers = new ArrayList<>();
    private final ExpectedCount expectedCount;

    RequestExpectation(ExpectedCount expectedCount, RequestMatcher requestMatcher) {
        validateNotNull(expectedCount, "'expectedCount' must not be null.");
        validateNotNull(requestMatcher, "'requestMatcher' must not be null.");
        this.expectedCount = expectedCount;
        matchers.add(requestMatcher);
    }

    @Override
    public void match(ClientRequestContext request) throws IOException {
        for (RequestMatcher matcher : matchers) {
            matcher.match(request);
        }
    }

    @Override
    public ResponseActions andExpect(RequestMatcher requestMatcher) {
        validateNotNull(requestMatcher, "'requestMatcher' must not be null.");
        matchers.add(requestMatcher);
        return this;
    }

    @Override
    public void andRespond(ResponseCreator responseCreator) {
        validateNotNull(responseCreator, "'responseCreator' must not be null.");
        this.responseCreator = responseCreator;
    }

    boolean hasRemainingCount() {
        return matchedCount < expectedCount.getMax();
    }

    boolean isSatisfied() {
        return matchedCount >= expectedCount.getMin();
    }

    void incrementAndValidate() {
        matchedCount++;
        if (matchedCount > expectedCount.getMax()) {
            throw new AssertionError("Received more calls than expected.");
        }
    }

    @Override
    public Response createResponse(ClientRequestContext request) throws IOException {
        if (responseCreator == null) {
            throw new IllegalStateException("Call to createResponse before responseCreator was set.");
        }
        return responseCreator.createResponse(request);
    }
}
