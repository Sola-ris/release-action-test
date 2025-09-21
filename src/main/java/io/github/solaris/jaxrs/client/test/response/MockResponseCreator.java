package io.github.solaris.jaxrs.client.test.response;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.core.Variant;

import org.jspecify.annotations.Nullable;

import io.github.solaris.jaxrs.client.test.request.EntityConverter;

/**
 * A {@link ResponseCreator} that creates a mock {@link Response} without calling an external service.
 */
public class MockResponseCreator implements ResponseCreator {
    private final StatusType status;

    private @Nullable Object entity;

    private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private final List<NewCookie> cookies = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final List<Variant> variants = new ArrayList<>();

    MockResponseCreator(StatusType status) {
        validateNotNull(status, "'status' must not be null.");
        this.status = status;
    }

    /**
     * Set the response entity.
     */
    public MockResponseCreator entity(Object entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Set the {@code Content-Type} header to the given {@link MediaType}
     */
    public MockResponseCreator mediaType(MediaType mediaType) {
        validateNotNull(mediaType, "'mediaType' must not be null.");
        headers.putSingle(CONTENT_TYPE, mediaType);
        return this;
    }

    /**
     * Add a Header with one or more values to the response.
     */
    public MockResponseCreator header(String name, Object... values) {
        validateNotNull(name, "'name' must not be null.");
        validateNotNull(values, "'values' must not be null.");
        for (Object value : values) {
            headers.add(name, value);
        }
        return this;
    }

    /**
     * Add one or more {@link NewCookie NewCookies} to the response.
     */
    public MockResponseCreator cookies(NewCookie... cookies) {
        validateNotNull(cookies, "'cookies' must not be null.");
        this.cookies.addAll(Arrays.asList(cookies));
        return this;
    }

    /**
     * Add one or more {@link Link Links} to the response.
     */
    public MockResponseCreator links(Link... links) {
        validateNotNull(links, "'links' must not be null.");
        this.links.addAll(Arrays.asList(links));
        return this;
    }

    /**
     * Add one or more {@link Variant Variants} to the response.
     */
    public MockResponseCreator variants(Variant... variants) {
        validateNotNull(variants, "'variants' must not be null.");
        this.variants.addAll(Arrays.asList(variants));
        return this;
    }

    @Override
    public Response createResponse(ClientRequestContext request) throws IOException {
        Response.ResponseBuilder responseBuilder = Response.status(status)
                .entity(entity)
                .replaceAll(headers)
                .links(links.toArray(new Link[0]))
                .cookie(cookies.toArray(new NewCookie[0]));

        // RestEasy Reactive throws an NPE when passed an empty list of Variants
        if (!variants.isEmpty()) {
            responseBuilder.variants(variants);
        }

        Response response = responseBuilder.build();

        // CXF does not serialize the entity when aborting a request,
        // breaking Response::readEntity for everything except Strings and Numbers.
        // Checking directly for a specific implementation is ugly,
        // but I can't think of another way that does not involve buffering or exceptions.
        if (entity != null && response.getClass().getPackageName().contains("cxf")) {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            InputStream serialized = converter.convertEntity(new SerializingRequestContext(response), InputStream.class);
            try (response) {
                return Response.fromResponse(response)
                        .entity(serialized)
                        .build();
            }
        }

        return response;
    }
}
