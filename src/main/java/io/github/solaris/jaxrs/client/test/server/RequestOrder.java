package io.github.solaris.jaxrs.client.test.server;

/**
 * The order in which the {@link MockRestServer} will expect the requests to be performed.
 */
public enum RequestOrder {

    /**
     * Expect the first instance of each request to be performed in order of declaration.
     * <p>
     * If a request is expected to be performed more than once,
     * invocations after the first one may be performed in any order.
     * </p>
     *
     * @see io.github.solaris.jaxrs.client.test.manager.OrderedRequestExpectationManager OrderedRequestExpectationManager
     */
    ORDERED,

    /**
     * Expect requests to be performed in any order.
     *
     * @see io.github.solaris.jaxrs.client.test.manager.UnorderedRequestExpectationManager UnorderedRequestExpectationManager
     */
    UNORDERED,

    /**
     * Expect all instances of each request to be performed in order of declaration until the expectation is satisfied.
     *
     * @see io.github.solaris.jaxrs.client.test.manager.StrictlyOrderedRequestExpectationManager StrictlyOrderedRequestExpectationManager
     */
    STRICT
}
