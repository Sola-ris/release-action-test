package io.github.solaris.jaxrs.client.test.request;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import io.github.solaris.jaxrs.client.test.internal.RequestContextStub;

record MultiPartRequestContext(Object entity, Type entityType, MultivaluedMap<String, String> stringHeaders) implements RequestContextStub {
    static final GenericType<List<EntityPart>> ENTITY_PARTS = new GenericType<>() {};

    MultiPartRequestContext(List<EntityPart> entityParts) {
        this(entityParts, ENTITY_PARTS.getType(), new MultivaluedHashMap<>(Map.of(CONTENT_TYPE, MULTIPART_FORM_DATA)));
    }

    MultiPartRequestContext(BufferedEntityPart entityPart) {
        this(entityPart.getContent(), InputStream.class, entityPart.getHeaders());
    }

    @Override
    @SuppressWarnings("unchecked")
    public MultivaluedMap<String, Object> getHeaders() {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        for (Map.Entry<String, ? extends List<?>> entry : stringHeaders.entrySet()) {
            headers.put(entry.getKey(), (List<Object>) entry.getValue());
        }
        return headers;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return stringHeaders;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.valueOf(stringHeaders.getFirst(CONTENT_TYPE));
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public Class<?> getEntityClass() {
        return entity.getClass();
    }

    @Override
    public Type getEntityType() {
        return entityType;
    }
}
