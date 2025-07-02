package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;

/**
 * {@link RequestExpectationManager} that expects each request to be satisfied in order of declaration.
 * <p>Corresponds to mockRestServerBuilder.withRequestOrder({@link io.github.solaris.jaxrs.client.test.server.RequestOrder#STRICT STRICT}).</p>
 */
public class StrictlyOrderedRequestExpectationManager extends RequestExpectationManager {
    private final List<RequestExpectation> expectations = new ArrayList<>();

    @Override
    void expectationsDeclared() {
        expectations.addAll(getExpectations());
    }

    @Override
    RequestExpectation matchRequest(ClientRequestContext requestContext) throws IOException {
        RequestExpectation matchingExpectation = null;
        for (RequestExpectation expectation : expectations) {
            if (expectation.isSatisfied()) {
                try {
                    expectation.match(requestContext);
                    matchingExpectation = expectation;
                    break;
                } catch (AssertionError ignore) {}
            } else {
                expectation.match(requestContext);
                matchingExpectation = expectation;
                break;
            }
        }

        if (matchingExpectation == null) {
            throw createUnexpectedRequestError(requestContext);
        }

        matchingExpectation.incrementAndValidate();
        if (!matchingExpectation.hasRemainingCount()) {
            expectations.remove(matchingExpectation);
        }
        return matchingExpectation;
    }

    @Override
    public void reset() {
        super.reset();
        expectations.clear();
    }
}
