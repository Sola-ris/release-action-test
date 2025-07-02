package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.client.ClientRequestContext;

import org.jspecify.annotations.Nullable;

class RequestExpectationGroup {
    private final Set<RequestExpectation> expectations = new HashSet<>();

    @Nullable RequestExpectation findExpectation(ClientRequestContext requestContext) throws IOException {
        for (RequestExpectation expectation : expectations) {
            try {
                expectation.match(requestContext);
                return expectation;
            } catch (AssertionError ignored) {
                // Return the matching expectation or null, ignore Exceptions
            }
        }
        return null;
    }

    void addExpectations(Collection<RequestExpectation> expectations) {
        this.expectations.addAll(expectations);
    }

    void update(RequestExpectation expectation) {
        expectation.incrementAndValidate();
        if (expectation.hasRemainingCount()) {
            expectations.add(expectation);
        } else {
            expectations.remove(expectation);
        }
    }

    void reset() {
        expectations.clear();
    }
}
