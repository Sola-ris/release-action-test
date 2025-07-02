package io.github.solaris.jaxrs.client.test.request;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

record MultiPartRequestContext(Object entity, Type entityType, MultivaluedMap<String, String> stringHeaders) implements ClientRequestContext {
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

    @Override
    public Annotation[] getEntityAnnotations() {
        return new Annotation[0];
    }

    //<editor-fold desc="UnsupportedOperationExceptions">
    @Override
    public Object getProperty(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String name, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUri(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMethod() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMethod(String method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeaderString(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLanguage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntity(Object entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getEntityStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Client getClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Configuration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void abortWith(Response response) {
        throw new UnsupportedOperationException();
    }
    //</editor-fold>
}
