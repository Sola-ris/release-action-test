package io.github.solaris.jaxrs.client.test.internal;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
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
import jakarta.ws.rs.core.Response;

public interface RequestContextStub extends ClientRequestContext {

    @Override
    default Annotation[] getEntityAnnotations() {
        return new Annotation[0];
    }

    @Override
    default boolean hasEntity() {
        return true;
    }

    //<editor-fold desc="UnsupportedOperationExceptions">
    @Override
    default Object getProperty(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Collection<String> getPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setProperty(String name, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void removeProperty(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    default URI getUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setUri(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getMethod() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setMethod(String method) {
        throw new UnsupportedOperationException();
    }

    @Override
    default String getHeaderString(String name) {
        throw new UnsupportedOperationException();
    }

    // @Override in JAX-RS 4
    @SuppressWarnings("unused")
    default boolean containsHeaderString(String name, String valueSeparatorRegex, Predicate<String> valuePredicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Date getDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Locale getLanguage() {
        throw new UnsupportedOperationException();
    }

    @Override
    default List<MediaType> getAcceptableMediaTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    default List<Locale> getAcceptableLanguages() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Map<String, Cookie> getCookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setEntity(Object entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        throw new UnsupportedOperationException();
    }

    @Override
    default OutputStream getEntityStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void setEntityStream(OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Client getClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    default Configuration getConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    default void abortWith(Response response) {
        throw new UnsupportedOperationException();
    }
    //</editor-fold>
}
