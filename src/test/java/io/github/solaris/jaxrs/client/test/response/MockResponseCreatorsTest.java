package io.github.solaris.jaxrs.client.test.response;

import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.CXF;
import static jakarta.ws.rs.core.HttpHeaders.RETRY_AFTER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.OTHER;
import static jakarta.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.LENGTH_REQUIRED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.Status.UNAVAILABLE_FOR_LEGAL_REASONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.SocketException;
import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

class MockResponseCreatorsTest {

    @JaxRsVendorTest
    void testSuccess() {
        try (Response response = MockResponseCreators.withSuccess().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
        }
    }

    @JaxRsVendorTest
    void testSuccess_withEntity() {
        String json = "{\"hello\": true}";
        try (Response response = MockResponseCreators.withSuccess(json, null).createResponse(new MockClientRequestContext())) {
            assertThat(response).satisfies(
                    r -> assertThat(r.getStatusInfo().toEnum()).isEqualTo(OK),
                    r -> assertThat(r.getMediaType()).isNull(),
                    r -> assertThat(r.getEntity()).isEqualTo(json)
            );
        }
    }

    @JaxRsVendorTest
    void testSuccess_withEntityAndMediaType() {
        String json = "{\"hello\": true}";
        try (Response response = MockResponseCreators.withSuccess(json, APPLICATION_JSON_TYPE).createResponse(new MockClientRequestContext())) {
            assertThat(response).satisfies(
                    r -> assertThat(r.getStatusInfo().toEnum()).isEqualTo(OK),
                    r -> assertThat(r.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE),
                    r -> assertThat(r.getEntity()).isEqualTo(json)
            );
        }
    }

    @JaxRsVendorTest
    void testCreated() {
        URI location = URI.create("local.host");
        try (Response response = MockResponseCreators.withCreated(location).createResponse(new MockClientRequestContext())) {
            assertThat(response).satisfies(
                    r -> assertThat(r.getStatusInfo().toEnum()).isEqualTo(CREATED),
                    r -> assertThat(r.getLocation()).isEqualTo(location)
            );
        }
    }

    @JaxRsVendorTest
    void testAccepted() {
        try (Response response = MockResponseCreators.withAccepted().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(ACCEPTED);
        }
    }

    @JaxRsVendorTest
    void testNoContent() {
        try (Response response = MockResponseCreators.withNoContent().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(NO_CONTENT);
        }
    }

    @JaxRsVendorTest
    void testBadRequest() {
        try (Response response = MockResponseCreators.withBadRequest().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(BAD_REQUEST);
        }
    }

    @JaxRsVendorTest
    void testUnauthorized() {
        try (Response response = MockResponseCreators.withUnauthorized().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(UNAUTHORIZED);
        }
    }

    @JaxRsVendorTest
    void testForbidden() {
        try (Response response = MockResponseCreators.withForbidden().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(FORBIDDEN);
        }
    }

    @JaxRsVendorTest
    void testNotFound() {
        try (Response response = MockResponseCreators.withNotFound().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(NOT_FOUND);
        }
    }

    @JaxRsVendorTest
    void testConflict() {
        try (Response response = MockResponseCreators.withConflict().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(CONFLICT);
        }
    }

    @JaxRsVendorTest
    void testTooManyRequests() {
        try (Response response = MockResponseCreators.withTooManyRequests().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(TOO_MANY_REQUESTS);
        }
    }

    @JaxRsVendorTest
    void testTooManyRequests_withRetryAfter() {
        try (Response response = MockResponseCreators.withTooManyRequests(42).createResponse(new MockClientRequestContext())) {
            assertThat(response).satisfies(
                    r -> assertThat(r.getStatusInfo().toEnum()).isEqualTo(TOO_MANY_REQUESTS),
                    r -> assertThat(r.getHeaderString(RETRY_AFTER)).isEqualTo("42")
            );
        }
    }

    @JaxRsVendorTest
    void testInternalServerError() {
        try (Response response = MockResponseCreators.withInternalServerError().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(INTERNAL_SERVER_ERROR);
        }
    }

    @JaxRsVendorTest
    void testServiceUnavailable() {
        try (Response response = MockResponseCreators.withServiceUnavailable().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(SERVICE_UNAVAILABLE);
        }
    }

    @JaxRsVendorTest
    void testGatewayTimeout() {
        try (Response response = MockResponseCreators.withGatewayTimeout().createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(GATEWAY_TIMEOUT);
        }
    }

    @JaxRsVendorTest
    void testStatus() {
        try (Response response = MockResponseCreators.withStatus(LENGTH_REQUIRED).createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(LENGTH_REQUIRED);
        }
    }

    @JaxRsVendorTest
    void testCustomStatus_definedInStatusEnum() {
        try (Response response = MockResponseCreators.withStatus(451).createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(UNAVAILABLE_FOR_LEGAL_REASONS);
        }
    }

    @JaxRsVendorTest
    void testCustomStatus_notDefinedInStatusEnum() {
        try (Response response = MockResponseCreators.withStatus(418).createResponse(new MockClientRequestContext())) {
            assertThat(response).satisfies(
                    r -> assertThat(r.getStatus()).isEqualTo(418),
                    r -> assertThat(r.getStatusInfo().getFamily()).isEqualTo(CLIENT_ERROR),
                    r -> assertThat(r.getStatusInfo().toEnum()).isNull()
            );
        }
    }

    // CXF doesn't support status codes > 599
    @JaxRsVendorTest(skipFor = CXF)
    void testCustomStatus_familyOther() {
        try (Response response = MockResponseCreators.withStatus(999).createResponse(new MockClientRequestContext())) {
            assertThat(response).satisfies(
                    r -> assertThat(r.getStatus()).isEqualTo(999),
                    r -> assertThat(r.getStatusInfo().getFamily()).isEqualTo(OTHER),
                    r -> assertThat(r.getStatusInfo().toEnum()).isNull()
            );
        }
    }

    @Test
    void testException() {
        assertThatThrownBy(() -> MockResponseCreators.withException(new SocketException("Connection Reset")).createResponse(new MockClientRequestContext()).close())
                .isInstanceOf(SocketException.class)
                .hasMessage("Connection Reset");
    }
}
