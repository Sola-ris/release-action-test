package io.github.solaris.jaxrs.client.test.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public record MockClientRequestContext(
        String method, URI uri, MultivaluedMap<String, String> stringHeaders, MediaType mediaType) implements ClientRequestContext {

    public MockClientRequestContext(MultivaluedMap<String, String> stringHeaders) {
        this(null, null, stringHeaders, null);
    }

    public MockClientRequestContext(URI uri) {
        this(null, uri, new MultivaluedHashMap<>(), null);
    }

    public MockClientRequestContext(String method) {
        this(method, null, new MultivaluedHashMap<>(), null);
    }

    public MockClientRequestContext(String method, String uri) {
        this(method, URI.create(uri), new MultivaluedHashMap<>(), null);
    }

    public MockClientRequestContext(MediaType mediaType) {
        this(null, null, null, mediaType);
    }

    public MockClientRequestContext() {
        this(null, null, null, null);
    }

    //<editor-fold desc="Interface methods">
    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public Collection<String> getPropertyNames() {
        return emptyList();
    }

    @Override
    public void setProperty(String name, Object object) {

    }

    @Override
    public void removeProperty(String name) {

    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void setUri(URI uri) {

    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public void setMethod(String method) {

    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return new MultivaluedHashMap<>(stringHeaders);
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return stringHeaders;
    }

    @Override
    public String getHeaderString(String name) {
        return "";
    }

    @SuppressWarnings("unused")
    public boolean containsHeaderString(String name, String valueSeparatorRegex, Predicate<String> valuePredicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return emptyList();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return emptyList();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return emptyMap();
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public Class<?> getEntityClass() {
        return null;
    }

    @Override
    public Type getEntityType() {
        return null;
    }

    @Override
    public void setEntity(Object entity) {

    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {

    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return new Annotation[0];
    }

    @Override
    public OutputStream getEntityStream() {
        return null;
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {

    }

    @Override
    public Client getClient() {
        return null;
    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public void abortWith(Response response) {

    }
    //</editor-fold>
}
