package io.github.solaris.jaxrs.client.test.server;

import jakarta.ws.rs.core.Configurable;

import io.github.solaris.jaxrs.client.test.manager.OrderedRequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.StrictlyOrderedRequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.UnorderedRequestExpectationManager;

/**
 * Builder to create a {@link MockRestServer}.
 */
public class MockRestServerBuilder {
    private final Configurable<?> configurable;

    private RequestOrder order = RequestOrder.ORDERED;

    MockRestServerBuilder(Configurable<?> configurable) {
        this.configurable = configurable;
    }

    /**
     * Set the desired {@link RequestOrder}. Defaults to {@link RequestOrder#ORDERED ORDERED}.
     *
     * @param order The request ordering
     */
    public MockRestServerBuilder withRequestOrder(RequestOrder order) {
        this.order = order;
        return this;
    }

    /**
     * Build the {@link MockRestServer} with the given request ordering and bind the given JAX-RS component to it.
     *
     * @return The {@code MockRestServer}
     */
    public MockRestServer build() {
        RequestExpectationManager expectationManager = switch (order) {
            case ORDERED -> new OrderedRequestExpectationManager();
            case UNORDERED -> new UnorderedRequestExpectationManager();
            case STRICT -> new StrictlyOrderedRequestExpectationManager();
        };

        if (!configurable.getConfiguration().isRegistered(MockResponseFilter.class)) {
            configurable.register(MockResponseFilter.class, Integer.MAX_VALUE);
        }
        configurable.property(RequestExpectationManager.class.getName(), expectationManager);

        return new MockRestServer(expectationManager);
    }
}
