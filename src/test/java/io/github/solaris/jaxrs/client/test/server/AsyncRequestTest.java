package io.github.solaris.jaxrs.client.test.server;

import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.method;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withException;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.CXF;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Nested;

import io.github.solaris.jaxrs.client.test.util.Dto;
import io.github.solaris.jaxrs.client.test.util.GreetingSendoffClient;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import io.github.solaris.jaxrs.client.test.util.extension.RunInQuarkus;

class AsyncRequestTest {
    private static final Dto BODY = new Dto("hello");
    private static final String EXCEPTION_MESSAGE = "Connection reset";

    @JaxRsVendorTest
    void testInvokeAsync_success() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(method(GET)).andRespond(withSuccess());

        try (client) {
            Future<Response> responseFuture = client.target("").request().async().get();
            assertThat(responseFuture)
                    .succeedsWithin(Duration.ofSeconds(1))
                    .satisfies(
                            response -> assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK)
                    );
        }
    }

    @JaxRsVendorTest
    void testInvokeAsync_failure() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(method(GET)).andRespond(withException(new SocketException(EXCEPTION_MESSAGE)));

        try (client) {
            Future<Response> responseFuture = client.target("").request().async().get();
            assertThat(responseFuture)
                    .failsWithin(Duration.ofSeconds(1))
                    .withThrowableOfType(ExecutionException.class)
                    .havingCause()
                    .isInstanceOf(ProcessingException.class)
                    .havingCause()
                    .isInstanceOf(SocketException.class)
                    .withMessage(EXCEPTION_MESSAGE);
        }
    }

    @JaxRsVendorTest
    void testInvokeAsyncWithCallback_success() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(method(GET)).andRespond(withSuccess(BODY, APPLICATION_JSON_TYPE));

        try (client) {
            Future<Response> responseFuture = client.target("").request().async().get(new AssertingResponseCallback());
            assertThat(responseFuture)
                    .succeedsWithin(Duration.ofSeconds(1));
        }
    }

    @JaxRsVendorTest
    void testInvokeAsyncWithCallback_failure() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(method(GET)).andRespond(withException(new SocketException(EXCEPTION_MESSAGE)));

        try (client) {
            Future<Response> responseFuture = client.target("").request().async().get(new AssertingResponseCallback());
            assertThat(responseFuture)
                    .failsWithin(Duration.ofSeconds(1));
        }
    }

    @JaxRsVendorTest
    void testInvokeRx_success() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(method(GET)).andRespond(withSuccess());

        try (client) {
            CompletionStage<Response> completionStage = client.target("").request().rx().get();
            assertThat(completionStage)
                    .succeedsWithin(Duration.ofSeconds(1))
                    .satisfies(
                            response -> assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK)
                    );
        }
    }

    @JaxRsVendorTest
    void testInvokeRx_failure() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(method(GET)).andRespond(withException(new SocketException(EXCEPTION_MESSAGE)));

        try (client) {
            CompletionStage<Response> completionStage = client.target("").request().rx().get();
            assertThat(completionStage)
                    .failsWithin(Duration.ofSeconds(1))
                    .withThrowableOfType(ExecutionException.class)
                    .havingCause()
                    .isInstanceOf(ProcessingException.class)
                    .havingCause()
                    .isInstanceOf(SocketException.class)
                    .withMessage(EXCEPTION_MESSAGE);
        }
    }

    @Nested
    @RunInQuarkus
    class MicroProfileRestClient {

        @JaxRsVendorTest(skipFor = CXF)
        void testInvokeMpRestClientAsync_success() throws Exception {
            RestClientBuilder builder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(method(GET)).andExpect(requestTo("http://localhost/hello-async")).andRespond(withSuccess());

            try (GreetingSendoffClient client = builder.build(GreetingSendoffClient.class)) {
                assertThat(client.greetAsync())
                        .succeedsWithin(Duration.ofSeconds(1));
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testInvokeMpRestClientAsync_failure() throws Exception {
            RestClientBuilder builder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(builder).build();

            server.expect(method(GET))
                    .andExpect(requestTo("http://localhost/hello-async"))
                    .andRespond(withException(new SocketException(EXCEPTION_MESSAGE)));

            try (GreetingSendoffClient client = builder.build(GreetingSendoffClient.class)) {
                assertThat(client.greetAsync())
                        .failsWithin(Duration.ofSeconds(1))
                        .withThrowableOfType(ExecutionException.class)
                        .havingCause()
                        .isInstanceOf(ProcessingException.class)
                        .havingCause()
                        .isInstanceOf(SocketException.class)
                        .withMessage(EXCEPTION_MESSAGE);
            }
        }
    }

    private record AssertingResponseCallback() implements InvocationCallback<Response> {

        @Override
        public void completed(Response response) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(response.readEntity(Dto.class)).isEqualTo(BODY);
        }

        @Override
        public void failed(Throwable throwable) {
            assertThat(throwable)
                    .rootCause() // CXF Wraps this one in a ProcessingException for some reason
                    .isInstanceOf(SocketException.class)
                    .hasMessage(EXCEPTION_MESSAGE);
        }
    }
}
