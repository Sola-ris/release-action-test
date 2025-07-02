package io.github.solaris.jaxrs.client.test.response;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static jakarta.ws.rs.core.HttpHeaders.RETRY_AFTER;
import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import org.jspecify.annotations.Nullable;

/**
 * Factory methods for {@link ResponseCreator ResponseCreators} with a given status code.
 *
 * @see ExecutingResponseCreator
 */
public final class MockResponseCreators {
    private MockResponseCreators() {}

    /**
     * {@code ResponseCreator} for status code 200 ({@link jakarta.ws.rs.core.Response.Status#OK OK})
     */
    public static MockResponseCreator withSuccess() {
        return new MockResponseCreator(OK);
    }

    /**
     * {@code ResponseCreator} for status code 200 ({@link jakarta.ws.rs.core.Response.Status#OK OK}) and a response body.
     *
     * @param entity    The response entity
     * @param mediaType The {@code Content-Type} of the entity (possibly {@code null})
     */
    public static MockResponseCreator withSuccess(Object entity, @Nullable MediaType mediaType) {
        MockResponseCreator responseCreator = new MockResponseCreator(OK).entity(entity);
        return mediaType == null ? responseCreator : responseCreator.mediaType(mediaType);
    }

    /**
     * {@code ResponseCreator} for status code 201 ({@link jakarta.ws.rs.core.Response.Status#CREATED CREATED}) with a {@code Location} header.
     *
     * @param location The value of the {@code Location} header
     */
    public static MockResponseCreator withCreated(URI location) {
        return new MockResponseCreator(CREATED).header(LOCATION, location);
    }

    /**
     * {@code ResponseCreator} for status code 202 ({@link jakarta.ws.rs.core.Response.Status#ACCEPTED ACCEPTED})
     */
    public static MockResponseCreator withAccepted() {
        return new MockResponseCreator(ACCEPTED);
    }

    /**
     * {@code ResponseCreator} for status code 203 ({@link jakarta.ws.rs.core.Response.Status#NO_CONTENT NO_CONTENT})
     */
    public static MockResponseCreator withNoContent() {
        return new MockResponseCreator(NO_CONTENT);
    }

    /**
     * {@code ResponseCreator} for status code 400 ({@link jakarta.ws.rs.core.Response.Status#BAD_REQUEST BAD_REQUEST})
     */
    public static MockResponseCreator withBadRequest() {
        return new MockResponseCreator(BAD_REQUEST);
    }

    /**
     * {@code ResponseCreator} for status code 401 ({@link jakarta.ws.rs.core.Response.Status#UNAUTHORIZED UNAUTHORIZED})
     */
    public static MockResponseCreator withUnauthorized() {
        return new MockResponseCreator(UNAUTHORIZED);
    }

    /**
     * {@code ResponseCreator} for status code 403 ({@link jakarta.ws.rs.core.Response.Status#FORBIDDEN FORBIDDEN})
     */
    public static MockResponseCreator withForbidden() {
        return new MockResponseCreator(FORBIDDEN);
    }

    /**
     * {@code ResponseCreator} for status code 404 ({@link jakarta.ws.rs.core.Response.Status#NOT_FOUND NOT_FOUND})
     */
    public static MockResponseCreator withNotFound() {
        return new MockResponseCreator(NOT_FOUND);
    }

    /**
     * {@code ResponseCreator} for status code 409 ({@link jakarta.ws.rs.core.Response.Status#CONFLICT CONFLICT})
     */
    public static MockResponseCreator withConflict() {
        return new MockResponseCreator(CONFLICT);
    }

    /**
     * {@code ResponseCreator} for status code 429 ({@link jakarta.ws.rs.core.Response.Status#TOO_MANY_REQUESTS TOO_MANY_REQUESTS})
     */
    public static MockResponseCreator withTooManyRequests() {
        return new MockResponseCreator(TOO_MANY_REQUESTS);
    }

    /**
     * {@code ResponseCreator} for status code 429 ({@link jakarta.ws.rs.core.Response.Status#TOO_MANY_REQUESTS TOO_MANY_REQUESTS})
     * and a {@code Retry-After} header
     *
     * @param retryAfter The value of the {@code Retry-After} header in seconds
     */
    public static MockResponseCreator withTooManyRequests(int retryAfter) {
        return new MockResponseCreator(TOO_MANY_REQUESTS).header(RETRY_AFTER, retryAfter);
    }

    /**
     * {@code ResponseCreator} for status code 500 ({@link jakarta.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR INTERNAL_SERVER_ERROR})
     */
    public static MockResponseCreator withInternalServerError() {
        return new MockResponseCreator(INTERNAL_SERVER_ERROR);
    }

    /**
     * {@code ResponseCreator} for status code 503 ({@link jakarta.ws.rs.core.Response.Status#SERVICE_UNAVAILABLE SERVICE_UNAVAILABLE})
     */
    public static MockResponseCreator withServiceUnavailable() {
        return new MockResponseCreator(SERVICE_UNAVAILABLE);
    }

    /**
     * {@code ResponseCreator} for status code 504 ({@link jakarta.ws.rs.core.Response.Status#GATEWAY_TIMEOUT GATEWAY_TIMEOUT})
     */
    public static MockResponseCreator withGatewayTimeout() {
        return new MockResponseCreator(GATEWAY_TIMEOUT);
    }

    /**
     * {@code ResponseCreator} for a specific {@link StatusType StatusType}
     */
    public static MockResponseCreator withStatus(StatusType status) {
        return new MockResponseCreator(status);
    }

    /**
     * {@code ResponseCreator} for a specific {@code status code}
     */
    public static MockResponseCreator withStatus(int statusCode) {
        StatusType statusType = Objects.requireNonNullElseGet(Status.fromStatusCode(statusCode), () -> new CustomStatus(statusCode));
        return new MockResponseCreator(statusType);
    }

    /**
     * {@code ResponseCreator} that throws an {@link IOException} when called.
     * Can for example be used to simulate a {@link java.net.SocketTimeoutException SocketTimeoutException}.
     *
     * @param ioe The {@code IOException} to throw
     */
    public static ResponseCreator withException(IOException ioe) {
        return request -> {
            throw ioe;
        };
    }

    private record CustomStatus(int statusCode) implements StatusType {

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Family getFamily() {
            return Family.familyOf(statusCode);
        }

        @Override
        public String getReasonPhrase() {
            return "";
        }
    }
}
