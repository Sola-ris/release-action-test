package io.github.solaris.jaxrs.client.test.manager;

import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.max;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.min;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.once;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.times;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.method;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;

class UnorderedRequestExpectationManagerTest {
    private final RequestExpectationManager manager = new UnorderedRequestExpectationManager();

    @Test
    void testNoExpectations() {
        assertThatCode(manager::verify).doesNotThrowAnyException();
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
    void testMultipleRequests() {
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();

            manager.verify();
        }).doesNotThrowAnyException();
    }

    @Test
    void repeatedRequests() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(times(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();

            manager.verify();
        }).doesNotThrowAnyException();
    }

    @Test
    void testTooManyRepeatedRequests() {
        manager.expectRequest(max(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(max(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        No further requests expected: HTTP GET /hello
                        4 request(s) executed:
                        GET /goodbye
                        GET /hello
                        GET /goodbye
                        GET /hello
                        """);
    }

    @Test
    void testTooFewRepeatedRequests() {
        manager.expectRequest(min(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(min(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        }).doesNotThrowAnyException();

        assertThatThrownBy(manager::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        Further request(s) expected leaving 1 unsatisfied expectation(s).
                        3 request(s) executed:
                        GET /hello
                        GET /goodbye
                        GET /goodbye
                        """);
    }
}
