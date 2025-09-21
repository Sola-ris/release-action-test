package io.github.solaris.jaxrs.client.test.response;

import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.internal.RequestContextStub;

record SerializingRequestContext(Response response) implements RequestContextStub {

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return response.getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return response.getStringHeaders();
    }

    @Override
    public MediaType getMediaType() {
        return response.getMediaType() != null ? response.getMediaType() : MediaType.WILDCARD_TYPE;
    }

    @Override
    public Object getEntity() {
        return response.getEntity();
    }

    @Override
    public Class<?> getEntityClass() {
        return getEntity().getClass();
    }

    @Override
    public Type getEntityType() {
        return getEntity().getClass();
    }
}
