package io.github.solaris.jaxrs.client.test.manager;

import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.between;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.max;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.never;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.once;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.times;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.method;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;

class StrictlyOrderedRequestExpectationManagerTest {
    private final RequestExpectationManager manager = new StrictlyOrderedRequestExpectationManager();

    @Test
    void testNoExpectations() {
        assertThatCode(manager::verify).doesNotThrowAnyException();
    }

    @Test
    void testUnfinishedSetup() {
        manager.expectRequest(once(), requestTo("/hello"));

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Call to createResponse before responseCreator was set.");
    }

    @Test
    void testMoreCallsThanExpected() {
        manager.expectRequest(never(), requestTo("/hello"));

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Received more calls than expected.");

        assertThatThrownBy(manager::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        Some requests did not execute successfully.
                        
                        Failed request:
                        GET /hello
                        java.lang.AssertionError: Received more calls than expected.""");
    }

    @Test
    void testUnexpectedRequest() {
        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        No further requests expected: HTTP GET /hello
                        0 request(s) executed.
                        """);
    }

    @Test
    void testFirstRequestSatisfiedBeforeSecond() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        }).doesNotThrowAnyException();

        assertThatCode(manager::verify).doesNotThrowAnyException();
    }

    @Test
    void testFirstRequestSatisfiedWithCallsRemaining() {
        manager.expectRequest(between(1, 2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        }).doesNotThrowAnyException();

        assertThatCode(manager::verify).doesNotThrowAnyException();
    }

    @Test
    void testFirstRequestSatisfiedSecondWithCallsRemainingThirdSatisfied() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(max(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/hello")).andExpect(method(POST)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
            manager.validateRequest(new MockClientRequestContext(POST, "/hello")).close();
        }).doesNotThrowAnyException();

        assertThatCode(manager::verify).doesNotThrowAnyException();
    }

    @Test
    void testFirstRequestNotSatisfiedBeforeSecond() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Unexpected Request. expected: </hello> but was: </goodbye>");
    }

    @Test
    void testTooFewRequests() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        }).doesNotThrowAnyException();

        assertThatThrownBy(manager::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        Further request(s) expected leaving 1 unsatisfied expectation(s).
                        2 request(s) executed:
                        GET /hello
                        GET /hello
                        """);
    }

    @Test
    void testTooManyRequests() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        No further requests expected: HTTP GET /hello
                        3 request(s) executed:
                        GET /hello
                        GET /hello
                        GET /goodbye
                        """);
    }

    @Test
    void testExpectationAfterFirstRequest() {
        manager.expectRequest(max(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close()).doesNotThrowAnyException();

        assertThatThrownBy(() -> manager.expectRequest(once(), requestTo("/goodbye")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot declare further expectations after the first request.");
    }
}
