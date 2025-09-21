package io.github.solaris.jaxrs.client.test.server;

import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.max;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.min;
import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.times;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.anything;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withException;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.server.RequestOrder.STRICT;
import static io.github.solaris.jaxrs.client.test.server.RequestOrder.UNORDERED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.GreetingSendoffClient;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import io.github.solaris.jaxrs.client.test.util.extension.RunInQuarkus;

class MockRestServerTest {

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void testArgumentValidation(ThrowingCallable callable, String exceptionMessage) {
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(exceptionMessage);
    }

    @SuppressWarnings("DataFlowIssue")
    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
                argumentSet("testBind_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(null), "JAX-RS client component must be null."),
                argumentSet("testBuild_order_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(ClientBuilder.newClient()).withRequestOrder(null),
                        "'order' must not be null."),
                argumentSet("testVerify_timeout_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(ClientBuilder.newClient()).build().verify(null),
                        "'timeout' must not be null."),
                argumentSet("testExpect_expectedCount_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(ClientBuilder.newClient()).build().expect(null, null),
                        "'expectedCount' must not be null."),
                argumentSet("testExpect_requestMatcher_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(ClientBuilder.newClient()).build().expect(null),
                        "'requestMatcher' must not be null."),
                argumentSet("testExpect_secondRequestMatcher_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(ClientBuilder.newClient()).build().expect(anything()).andExpect(null),
                        "'requestMatcher' must not be null."),
                argumentSet("testRespond_null",
                        (ThrowingCallable) () -> MockRestServer.bindTo(ClientBuilder.newClient()).build().expect(anything()).andRespond(null),
                        "'responseCreator' must not be null.")
        );
    }

    @Nested
    class BindClientBuilder {
        private final ClientBuilder builder = ClientBuilder.newBuilder();

        @JaxRsVendorTest
        void testOrderedExpectations() {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (Client client = builder.build()) {
                    assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testUnorderedExpectations() {
            MockRestServer server = MockRestServer.bindTo(builder).withRequestOrder(UNORDERED).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (Client client = builder.build()) {
                    assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testStrictExpectations() {
            MockRestServer server = MockRestServer.bindTo(builder).withRequestOrder(STRICT).build();

            server.expect(times(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (Client client = builder.build()) {
                    assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                        .isInstanceOf(AssertionError.class)
                        .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
            }
        }

        @JaxRsVendorTest
        void testStrictExpectations_firstRequestUnsatisfied(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).withRequestOrder(STRICT).build();

            server.expect(min(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                        .isInstanceOf(AssertionError.class)
                        .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
            }
        }

        @JaxRsVendorTest
        void testReset(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().get().close())
                        .isInstanceOf(AssertionError.class)
                        .hasMessage("""
                                No further requests expected: HTTP GET /hello
                                0 request(s) executed.
                                """);

                // Clear failed request
                server.reset();

                server.expect(requestTo("/goodbye")).andRespond(withSuccess());
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testReset_unordered(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).withRequestOrder(UNORDERED).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().get().close())
                        .isInstanceOf(AssertionError.class)
                        .hasMessage("""
                                No further requests expected: HTTP GET /hello
                                0 request(s) executed.
                                """);

                // Clear failed request
                server.reset();

                server.expect(requestTo("/goodbye")).andRespond(withSuccess());
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testReset_strict(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).withRequestOrder(STRICT).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().get().close())
                        .isInstanceOf(AssertionError.class)
                        .hasMessage("""
                                No further requests expected: HTTP GET /hello
                                0 request(s) executed.
                                """);

                // Clear failed request
                server.reset();

                server.expect(requestTo("/goodbye")).andRespond(withSuccess());
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testUnsatisfiedExpectation() {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                Assertions.assertThatThrownBy(server::verify)
                        .isInstanceOf(AssertionError.class)
                        .hasMessageMatching("""
                                Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                                1 request\\(s\\) executed:
                                GET /hello.*$
                                """);
            }
        }

        @JaxRsVendorTest
        void testMultipleBuilds() {
            ClientBuilder clientBuilder = ClientBuilder.newBuilder();
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(clientBuilder);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            try (Client client = clientBuilder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.withRequestOrder(UNORDERED).build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (Client client = clientBuilder.build()) {
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.build();
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            try (Client client = clientBuilder.build()) {
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.withRequestOrder(STRICT).build();
            server.expect(min(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (Client client = clientBuilder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testVerifyWithTimeout() {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant start = Instant.now();
                Assertions.assertThatThrownBy(() -> server.verify(verifyDuration))
                        .isInstanceOf(AssertionError.class)
                        .hasMessageMatching("""
                                Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                                1 request\\(s\\) executed:
                                GET /hello.*$
                                """);
                assertThat(Duration.between(start, Instant.now()))
                        .isGreaterThan(verifyDuration);
            }

            MockRestServer otherServer = MockRestServer.bindTo(builder).build();
            otherServer.expect(requestTo("/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant start = Instant.now();
                assertThatCode(() -> otherServer.verify(verifyDuration))
                        .doesNotThrowAnyException();
                assertThat(Duration.between(start, Instant.now()))
                        .isLessThan(verifyDuration);
            }
        }

        @JaxRsVendorTest
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                        .isInstanceOf(AssertionError.class)
                        .hasMessageStartingWith("No further requests expected");
            }

            assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("Some requests did not execute successfully.");
        }

        @JaxRsVendorTest
        void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();

                filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                        .isInstanceOf(AssertionError.class)
                        .hasMessageStartingWith("No further requests expected");

                server.reset();

                server.expect(requestTo("/goodbye")).andRespond(withSuccess());

                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(requestTo("/error")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (Client client = builder.build()) {
                filterExceptionAssert.assertThatThrownBy(() -> client.target("/error").request().get())
                        .isInstanceOf(SocketException.class)
                        .hasMessage("Connection Reset");

                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testBuilderUnaffectedByBoundClient() {
            Client client = builder.build();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(min(1), requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }

            try (Client otherClient = builder.build()) {
                assertThatThrownBy(() -> otherClient.target("/hello").request().get())
                        .isInstanceOf(ProcessingException.class);
            }

            server.verify();
        }

        @JaxRsVendorTest
        void testBuilderUnaffectedByBoundWebTarget() {
            Client client = builder.build();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(min(1), requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }

            try (Client otherClient = builder.build()) {
                assertThatThrownBy(() -> otherClient.target("/hello").request().get())
                        .isInstanceOf(ProcessingException.class);
            }

            server.verify();
        }
    }

    @Nested
    class BindClient {

        @AutoClose
        private final Client client = ClientBuilder.newClient();

        @JaxRsVendorTest
        void testOrderedExpectations() {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testUnorderedExpectations() {
            MockRestServer server = MockRestServer.bindTo(client).withRequestOrder(UNORDERED).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testStrictExpectations() {
            MockRestServer server = MockRestServer.bindTo(client).withRequestOrder(STRICT).build();

            server.expect(times(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
        }

        @JaxRsVendorTest
        void testStrictExpectations_firstRequestUnsatisfied(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).withRequestOrder(STRICT).build();

            server.expect(min(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
        }

        @JaxRsVendorTest
        void testReset(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().get().close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                            No further requests expected: HTTP GET /hello
                            0 request(s) executed.
                            """);

            // Clear failed request
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testReset_unordered(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().get().close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                            No further requests expected: HTTP GET /hello
                            0 request(s) executed.
                            """);

            // Clear failed request
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testReset_strict(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().get().close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                            No further requests expected: HTTP GET /hello
                            0 request(s) executed.
                            """);

            // Clear failed request
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testUnsatisfiedExpectation() {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Assertions.assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageMatching("""
                            Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                            1 request\\(s\\) executed:
                            GET /hello.*$
                            """);
        }

        @JaxRsVendorTest
        void testMultipleBuilds() {
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(client);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.withRequestOrder(UNORDERED).build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.build();
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.withRequestOrder(STRICT).build();
            server.expect(min(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }

        @JaxRsVendorTest
        void testVerifyWithTimeout() {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Instant start = Instant.now();
            Assertions.assertThatThrownBy(() -> server.verify(verifyDuration))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageMatching("""
                            Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                            1 request\\(s\\) executed:
                            GET /hello.*$
                            """);
            assertThat(Duration.between(start, Instant.now()))
                    .isGreaterThan(verifyDuration);

            MockRestServer otherServer = MockRestServer.bindTo(client).build();
            otherServer.expect(requestTo("/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Instant otherStart = Instant.now();
            assertThatCode(() -> otherServer.verify(verifyDuration))
                    .doesNotThrowAnyException();
            assertThat(Duration.between(otherStart, Instant.now()))
                    .isLessThan(verifyDuration);
        }

        @JaxRsVendorTest
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");

            assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("Some requests did not execute successfully.");
        }

        @JaxRsVendorTest
        void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");

            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }

        @JaxRsVendorTest
        void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) {
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/error")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("/hello")).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/error").request().get())
                    .isInstanceOf(SocketException.class)
                    .hasMessage("Connection Reset");

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testClientUnaffectedByBoundWebTarget() {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(min(1), requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            assertThatThrownBy(() -> client.target("/hello").request().get())
                    .isInstanceOf(ProcessingException.class);

            server.verify();
        }
    }

    @Nested
    class BindWebTarget {

        @AutoClose
        private final Client client = ClientBuilder.newClient();

        @JaxRsVendorTest
        void testOrderedExpectations() {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testUnorderedExpectations() {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).withRequestOrder(UNORDERED).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testStrictExpectations() {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).withRequestOrder(STRICT).build();

            server.expect(times(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
        }

        @JaxRsVendorTest
        void testStrictExpectations_firstRequestUnsatisfied(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).withRequestOrder(STRICT).build();

            server.expect(min(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
        }

        @JaxRsVendorTest
        void testReset(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            filterExceptionAssert.assertThatThrownBy(() -> target.path("/hello").request().get().close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                            No further requests expected: HTTP GET /hello
                            0 request(s) executed.
                            """);

            // Clear failed request
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testReset_unordered(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            filterExceptionAssert.assertThatThrownBy(() -> target.path("/hello").request().get().close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                            No further requests expected: HTTP GET /hello
                            0 request(s) executed.
                            """);

            // Clear failed request
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testReset_strict(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(max(2), requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            filterExceptionAssert.assertThatThrownBy(() -> target.path("/hello").request().get().close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("""
                            No further requests expected: HTTP GET /hello
                            0 request(s) executed.
                            """);

            // Clear failed request
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }

        @JaxRsVendorTest
        void testUnsatisfiedExpectation() {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Assertions.assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageMatching("""
                            Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                            1 request\\(s\\) executed:
                            GET /hello.*$
                            """);
        }

        @JaxRsVendorTest
        void testMultipleBuilds() {
            WebTarget target = client.target("");
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(target);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.withRequestOrder(UNORDERED).build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.build();
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.withRequestOrder(STRICT).build();
            server.expect(min(2), requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }

        @JaxRsVendorTest
        void testVerifyWithTimeout() {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Instant start = Instant.now();
            Assertions.assertThatThrownBy(() -> server.verify(verifyDuration))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageMatching("""
                            Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                            1 request\\(s\\) executed:
                            GET /hello.*$
                            """);
            assertThat(Duration.between(start, Instant.now()))
                    .isGreaterThan(verifyDuration);

            MockRestServer otherServer = MockRestServer.bindTo(target).build();
            otherServer.expect(requestTo("/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Instant otherStart = Instant.now();
            assertThatCode(() -> otherServer.verify(verifyDuration))
                    .doesNotThrowAnyException();
            assertThat(Duration.between(otherStart, Instant.now()))
                    .isLessThan(verifyDuration);
        }

        @JaxRsVendorTest
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");

            assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("Some requests did not execute successfully.");
        }

        @JaxRsVendorTest
        void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");

            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }

        @JaxRsVendorTest
        void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) {
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/error")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("/hello")).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> target.path("/error").request().get())
                    .isInstanceOf(SocketException.class)
                    .hasMessage("Connection Reset");

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }
    }

    @Nested
    @RunInQuarkus
    class BindMicroProfileRestClientBuilder {
        private final RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");

        @JaxRsVendorTest
        void testOrderedExpectations() {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                    assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testUnorderedExpectations() {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).withRequestOrder(UNORDERED).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                    assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testStrictExpectations() {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).withRequestOrder(STRICT).build();

            server.expect(times(2), requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                    assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                        .isInstanceOf(AssertionError.class)
                        .hasMessageEndingWith("Unexpected Request. expected: <http://localhost/hello> but was: <http://localhost/goodbye>");
            }
        }

        @JaxRsVendorTest
        void testStrictExpectations_firstRequestUnsatisfied(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).withRequestOrder(STRICT).build();

            server.expect(min(2), requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                        .isInstanceOf(AssertionError.class)
                        .hasMessageEndingWith("Unexpected Request. expected: <http://localhost/hello> but was: <http://localhost/goodbye>");
            }
        }

        @JaxRsVendorTest
        void testReset(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(max(2), requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                filterExceptionAssert.assertThatThrownBy(() -> client.greeting().close())
                        .isInstanceOf(AssertionError.class)
                        .hasMessage("""
                                No further requests expected: HTTP GET http://localhost/hello
                                0 request(s) executed.
                                """);

                // Clear failed request
                server.reset();

                server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testReset_unordered(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(max(2), requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                filterExceptionAssert.assertThatThrownBy(() -> client.greeting().close())
                        .isInstanceOf(AssertionError.class)
                        .hasMessage("""
                                No further requests expected: HTTP GET http://localhost/hello
                                0 request(s) executed.
                                """);

                // Clear failed request
                server.reset();

                server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testReset_strict(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(max(2), requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                filterExceptionAssert.assertThatThrownBy(() -> client.greeting().close())
                        .isInstanceOf(AssertionError.class)
                        .hasMessage("""
                                No further requests expected: HTTP GET http://localhost/hello
                                0 request(s) executed.
                                """);

                // Clear failed request
                server.reset();

                server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testUnsatisfiedExpectation() throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                Assertions.assertThatThrownBy(server::verify)
                        .isInstanceOf(AssertionError.class)
                        .hasMessageMatching("""
                                Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                                1 request\\(s\\) executed:
                                GET http://localhost/hello.*$
                                """);
            }
        }

        @JaxRsVendorTest
        void testMultipleBuilds() throws Exception {
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(restClientBuilder);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.withRequestOrder(UNORDERED).build();
            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.build();
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());
            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.withRequestOrder(STRICT).build();
            server.expect(min(2), requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testVerifyWithTimeout() throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant start = Instant.now();
                Assertions.assertThatThrownBy(() -> server.verify(verifyDuration))
                        .isInstanceOf(AssertionError.class)
                        .hasMessageMatching("""
                                Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                                1 request\\(s\\) executed:
                                GET http://localhost/hello.*$
                                """);
                assertThat(Duration.between(start, Instant.now()))
                        .isGreaterThan(verifyDuration);
            }

            MockRestServer otherServer = MockRestServer.bindTo(restClientBuilder).build();
            otherServer.expect(requestTo("http://localhost/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant start = Instant.now();
                assertThatCode(() -> otherServer.verify(verifyDuration))
                        .doesNotThrowAnyException();
                assertThat(Duration.between(start, Instant.now()))
                        .isLessThan(verifyDuration);
            }
        }

        @JaxRsVendorTest
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                        .isInstanceOf(AssertionError.class)
                        .hasMessageStartingWith("No further requests expected");
            }

            assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("Some requests did not execute successfully.");
        }

        @JaxRsVendorTest
        void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();

                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                        .isInstanceOf(AssertionError.class)
                        .hasMessageStartingWith("No further requests expected");

                server.reset();

                server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) throws Exception {
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/goodbye")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                        .isInstanceOf(SocketException.class)
                        .hasMessage("Connection Reset");

                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }
    }
}
