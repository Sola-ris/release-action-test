package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;
import java.util.Iterator;

import jakarta.ws.rs.client.ClientRequestContext;

import org.jspecify.annotations.Nullable;

/**
 * <p>{@link RequestExpectationManager} that expects the first invocation of each request to be performed in order of declaration.</p>
 * <p>This is the default {@code RequestExpectationManager}.</p>
 * <p>
 * Can be explicitly set by calling
 * mockRestServerBuilder.withRequestOrder({@link io.github.solaris.jaxrs.client.test.server.RequestOrder#ORDERED ORDERED})
 * </p>
 */
public class OrderedRequestExpectationManager extends RequestExpectationManager {
    private final RequestExpectationGroup expectationGroup = new RequestExpectationGroup();

    private @Nullable Iterator<RequestExpectation> expectationIterator;

    @Override
    void expectationsDeclared() {
        expectationIterator = getExpectations().iterator();
    }

    @Override
    RequestExpectation matchRequest(ClientRequestContext requestContext) throws IOException {
        RequestExpectation expectation = expectationGroup.findExpectation(requestContext);
        if (expectation == null) {
            if (expectationIterator == null || !expectationIterator.hasNext()) {
                throw createUnexpectedRequestError(requestContext);
            }
            expectation = expectationIterator.next();
            expectation.match(requestContext);
        }
        expectationGroup.update(expectation);
        return expectation;
    }

    @Override
    public void reset() {
        super.reset();
        expectationIterator = null;
        expectationGroup.reset();
    }
}
