package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.request.MultiPartRequestContext.ENTITY_PARTS;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.internal.ClientCleaner;

/**
 * Fallback {@link EntityConverter} that indirectly uses the available JAX-RS {@link jakarta.ws.rs.ext.Providers Providers}
 * through a {@link Client} to convert the entity.
 * <p>Must not be directly instantiated, use {@link EntityConverter#fromRequestContext(ClientRequestContext)}.</p>
 */
public final class ClientEntityConverter extends EntityConverter {
    private static final URI LOCALHOST = URI.create("http://localhost");
    private static final RoundTripFilter ROUND_TRIP_FILTER = new RoundTripFilter();

    public ClientEntityConverter() {}

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) {
        if (canShortCircuit(requestContext, type, null)) {
            return (T) requestContext.getEntity();
        }

        try (Response response = convertEntity(requestContext)) {
            return response.readEntity(type);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) {
        if (canShortCircuit(requestContext, genericType.getRawType(), genericType.getType())) {
            return (T) requestContext.getEntity();
        }

        try (Response response = convertEntity(requestContext)) {
            return response.readEntity(genericType);
        }
    }

    @Override
    List<EntityPart> serializeEntityParts(ClientRequestContext requestContext) {
        try (Response response = convertEntity(requestContext)) {
            return response.readEntity(ENTITY_PARTS);
        }
    }

    private Response convertEntity(ClientRequestContext requestContext) {
        // Directly closing the 'inner' client here causes Jersey to close the 'outer' client as well
        Client client = ClientBuilder.newClient().register(ROUND_TRIP_FILTER);
        ClientCleaner.register(this, client);
        return client.target(LOCALHOST)
                .request(requestContext.getMediaType())
                .post(Entity.entity(requestContext.getEntity(), requestContext.getMediaType()));
    }

    private static final class RoundTripFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.abortWith(Response.ok(new GenericEntity<>(requestContext.getEntity()) {}, requestContext.getMediaType()).build());
        }
    }
}
