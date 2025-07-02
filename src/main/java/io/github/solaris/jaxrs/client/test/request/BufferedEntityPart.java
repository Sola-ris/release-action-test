package io.github.solaris.jaxrs.client.test.request;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.CHARSET_PARAMETER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

class BufferedEntityPart implements EntityPart {
    private final EntityPart actualPart;
    private final EntityConverter converter;
    private final byte[] bufferedContent;

    BufferedEntityPart(EntityPart actualPart, EntityConverter converter) throws IOException {
        this.actualPart = actualPart;
        this.converter = converter;

        this.bufferedContent = actualPart.getContent().readAllBytes();
    }

    @Override
    public String getName() {
        return actualPart.getName();
    }

    @Override
    public Optional<String> getFileName() {
        return actualPart.getFileName();
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(bufferedContent);
    }

    @Override
    public <T> T getContent(Class<T> type) throws IOException {
        return converter.convertEntity(new MultiPartRequestContext(this), type);
    }

    @Override
    public <T> T getContent(GenericType<T> genericType) throws IOException {
        return converter.convertEntity(new MultiPartRequestContext(this), genericType);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return actualPart.getHeaders()
                .entrySet()
                .stream()
                .map(BufferedEntityPart::removeDefaultTextCharset)
                .collect(MultivaluedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), MultivaluedMap::putAll);
    }

    // RESTEasy will add charset=us-ascii to the Content-Type header if none is specified it's of type text/*,
    // which will cause unexpected results on repeated buffering, namely assertion errors due to differences in the headers.
    // Removing it here will cause RESTEasy to re-add it the next time the EntityPart is buffered.
    private static Entry<String, List<String>> removeDefaultTextCharset(Entry<String, List<String>> entry) {
        if (CONTENT_TYPE.equals(entry.getKey())) {
            String contentType = entry.getValue().get(0);
            if (!contentType.contains(CHARSET_PARAMETER)) {
                return entry;
            }

            MediaType mediaType = MediaType.valueOf(contentType);
            if (!"text".equals(mediaType.getType())) {
                return entry;
            }

            Map<String, String> parameters = mediaType.getParameters()
                    .entrySet()
                    .stream()
                    .filter(param -> !CHARSET_PARAMETER.equals(param.getKey()))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            return Map.entry(CONTENT_TYPE, List.of(new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters).toString()));
        }
        return entry;
    }

    @Override
    public MediaType getMediaType() {
        return actualPart.getMediaType();
    }

    byte[] getBuffer() {
        return bufferedContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BufferedEntityPart that = (BufferedEntityPart) o;
        return Arrays.equals(bufferedContent, that.bufferedContent)
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getFileName(), that.getFileName())
                && Objects.equals(getMediaType(), that.getMediaType())
                && Objects.equals(getHeaders(), that.getHeaders());
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(bufferedContent), getName(), getFileName(), getMediaType(), getHeaders());
    }

    @Override
    public String toString() {
        return "BufferedEntityPart{"
                + "name=" + getName()
                + ", fileName=" + getFileName()
                + ", mediaType=" + getMediaType()
                + ", headers=" + getHeaders()
                + ", content=" + bufferedContent.length + " bytes"
                + '}';
    }
}
