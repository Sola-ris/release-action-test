package io.github.solaris.jaxrs.client.test.response;

import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

@NullUnmarked
class ExecutingResponseCreatorTest {
    private static final AssertableHandler HANDLER = new AssertableHandler();
    private static final String REQUEST_BODY = "{\"hello\": true}";

    private static URI requestUri;
    private static HttpServer httpServer;

    @AutoClose
    private final Client client = ClientBuilder.newClient();

    @BeforeAll
    static void startServer() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        requestUri = UriBuilder.fromUri("http://localhost")
                .port(port)
                .path("hello")
                .build();

        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/hello", HANDLER);
        httpServer.setExecutor(null);
        httpServer.start();
    }

    @BeforeEach
    void resetHandler() {
        HANDLER.reset();
    }

    @AfterAll
    static void stopServer() {
        httpServer.stop(0);
    }

    @JaxRsVendorTest
    void testDefaultClient() {
        testResponseCreator(new ExecutingResponseCreator());
    }

    @JaxRsVendorTest
    void testCustomClient() {
        try (Client client = ClientBuilder.newClient()) {
            testResponseCreator(new ExecutingResponseCreator(client));
        }
    }

    @JaxRsVendorTest
    void testDefaultClientWithoutBody() {
        testResponseCreatorWithoutBody(new ExecutingResponseCreator());
    }

    @JaxRsVendorTest
    void testCustomClientWithoutBody() {
        try (Client client = ClientBuilder.newClient()) {
            testResponseCreatorWithoutBody(new ExecutingResponseCreator(client));
        }
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testCustomClient_null() {
        assertThatThrownBy(() -> new ExecutingResponseCreator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'client' must not be null.");
    }

    private void testResponseCreator(ExecutingResponseCreator responseCreator) {
        HeaderCaptor headerCaptor = new HeaderCaptor();
        MockRestServer mockServer = MockRestServer.bindTo(client).build();

        mockServer.expect(requestTo(requestUri)).andRespond(responseCreator);
        mockServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

        assertThatCode(() -> {
            Response serverResponse = client.target(requestUri)
                    .register(headerCaptor)
                    .request()
                    .header("X-Custom", "Custom-X")
                    .post(Entity.entity(REQUEST_BODY, TEXT_PLAIN));
            assertThat(serverResponse.getStatusInfo().toEnum()).isEqualTo(OK);
            serverResponse.close();

            Response mockResponse = client.target("/goodbye").request().get();
            assertThat(mockResponse.getStatusInfo().toEnum()).isEqualTo(OK);
        }).doesNotThrowAnyException();

        mockServer.verify();

        // Implementations add / compute additional headers
        assertThat(HANDLER.headers).containsAllEntriesOf(headerCaptor.headers);
        assertThat(HANDLER.requestUri).isEqualTo(requestUri);
        assertThat(HANDLER.body).isEqualTo(REQUEST_BODY);
    }

    private void testResponseCreatorWithoutBody(ExecutingResponseCreator responseCreator) {
        HeaderCaptor headerCaptor = new HeaderCaptor();
        MockRestServer mockServer = MockRestServer.bindTo(client).build();

        mockServer.expect(requestTo(requestUri)).andRespond(responseCreator);
        mockServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

        assertThatCode(() -> {
            Response serverResponse = client.target(requestUri)
                    .register(headerCaptor)
                    .request()
                    .header("X-Custom", "Custom-X")
                    .get();
            assertThat(serverResponse.getStatusInfo().toEnum()).isEqualTo(OK);
            serverResponse.close();

            Response mockResponse = client.target("/goodbye").request().get();
            assertThat(mockResponse.getStatusInfo().toEnum()).isEqualTo(OK);
        }).doesNotThrowAnyException();

        mockServer.verify();

        // Implementations add / compute additional headers
        assertThat(HANDLER.headers).containsAllEntriesOf(headerCaptor.headers);
        assertThat(HANDLER.requestUri).isEqualTo(requestUri);
    }

    private static class AssertableHandler implements HttpHandler {
        private String body = null;
        private URI requestUri = null;
        private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
                headers.addAll(header.getKey().toLowerCase(), header.getValue());
            }

            requestUri = UriBuilder.fromUri("http://" + exchange.getRequestURI())
                    .host(exchange.getLocalAddress().getHostName())
                    .port(exchange.getLocalAddress().getPort())
                    .build();
            body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);

            exchange.sendResponseHeaders(OK.getStatusCode(), 0);
            exchange.close();
        }

        private void reset() {
            body = null;
            requestUri = null;
            headers.clear();
        }
    }

    private static class HeaderCaptor implements ClientRequestFilter {
        private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        @Override
        public void filter(ClientRequestContext requestContext) {
            for (Map.Entry<String, List<String>> header : requestContext.getStringHeaders().entrySet()) {
                headers.addAll(header.getKey().toLowerCase(), header.getValue());
            }
        }
    }
}
