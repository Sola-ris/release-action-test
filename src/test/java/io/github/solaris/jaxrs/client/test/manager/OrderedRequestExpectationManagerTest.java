package io.github.solaris.jaxrs.client.test.manager;

import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.between;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.max;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.min;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.once;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.times;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.method;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withException;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.SocketException;

import org.junit.jupiter.api.Test;

import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;

class OrderedRequestExpectationManagerTest {
    private final RequestExpectationManager manager = new OrderedRequestExpectationManager();

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
    void testSequentialRequests() throws IOException {
        manager.expectRequest(once(), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();

        assertThatCode(manager::verify).doesNotThrowAnyException();
    }

    @Test
    void testTooManySequentialRequests() throws IOException {
        manager.expectRequest(once(), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/uhm")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        No further requests expected: HTTP GET /uhm
                        2 request(s) executed:
                        GET /hello
                        GET /goodbye
                        """);
    }

    @Test
    void testTooFewSequentialRequests() throws IOException {
        this.manager.expectRequest(min(1), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        this.manager.expectRequest(min(1), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        this.manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();

        assertThatThrownBy(manager::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        Further request(s) expected leaving 1 unsatisfied expectation(s).
                        1 request(s) executed:
                        GET /hello
                        """);
    }

    @Test
    void testRepeatedRequests() throws IOException {
        manager.expectRequest(times(3), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(times(3), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();

        assertThatCode(manager::verify).doesNotThrowAnyException();
    }

    @Test
    void testTooManyRepeatedRequests() throws IOException {
        manager.expectRequest(max(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(max(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        No further requests expected: HTTP GET /hello
                        4 request(s) executed:
                        GET /hello
                        GET /goodbye
                        GET /hello
                        GET /goodbye
                        """);
    }

    @Test
    void testTooFewRepeatedRequests() throws IOException {
        manager.expectRequest(min(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(min(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();

        assertThatThrownBy(manager::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessage("""
                        Further request(s) expected leaving 1 unsatisfied expectation(s).
                        3 request(s) executed:
                        GET /hello
                        GET /goodbye
                        GET /hello
                        """);
    }

    @Test
    void testRepeatedRequestsOutOfOrder() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(times(2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(POST, "/goodbye")).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Unexpected Request. expected: </hello> but was: </goodbye>");
    }

    @Test
    void testSequentialRequestsWithDifferentCounts() {
        manager.expectRequest(times(2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(once(), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        }).doesNotThrowAnyException();
    }

    @Test
    void repeatedRequestsInSequentialOrder() {
        manager.expectRequest(between(1, 2), requestTo("/hello")).andExpect(method(GET)).andRespond(withSuccess());
        manager.expectRequest(between(1, 2), requestTo("/goodbye")).andExpect(method(GET)).andRespond(withSuccess());

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/hello")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
            manager.validateRequest(new MockClientRequestContext(GET, "/goodbye")).close();
        }).doesNotThrowAnyException();
    }

    @Test
    void testSequentialRequestsWithExceptionOnFirstRequest() {
        manager.expectRequest(once(), requestTo("/failure"))
                .andExpect(method(GET))
                .andRespond(withException(new SocketException("Connection Reset")));
        manager.expectRequest(once(), requestTo("/hello"))
                .andExpect(method(POST))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> manager.validateRequest(new MockClientRequestContext(GET, "/failure")).close())
                .isInstanceOf(SocketException.class)
                .hasMessage("Connection Reset");

        assertThatCode(() -> {
            manager.validateRequest(new MockClientRequestContext(POST, "/hello")).close();
            manager.verify();
        }).doesNotThrowAnyException();
    }
}
