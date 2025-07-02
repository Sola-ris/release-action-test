package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;

/**
 * {@link RequestExpectationManager} that expects requests to be performed in any order.
 * <p>Corresponds to mockRestServerBuilder.withRequestOrder({@link io.github.solaris.jaxrs.client.test.server.RequestOrder#UNORDERED UNORDERED}).</p>
 */
public class UnorderedRequestExpectationManager extends RequestExpectationManager {
    private final RequestExpectationGroup expectationGroup = new RequestExpectationGroup();

    @Override
    void expectationsDeclared() {
        expectationGroup.addExpectations(getExpectations());
    }

    @Override
    RequestExpectation matchRequest(ClientRequestContext requestContext) throws IOException {
        RequestExpectation expectation = expectationGroup.findExpectation(requestContext);
        if (expectation == null) {
            throw createUnexpectedRequestError(requestContext);
        }

        expectationGroup.update(expectation);
        return expectation;
    }

    @Override
    public void reset() {
        super.reset();
        expectationGroup.reset();
    }
}
