package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;
import static io.github.solaris.jaxrs.client.test.request.MultiPartRequestContext.ENTITY_PARTS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

/**
 * {@link EntityConverter} that directly uses the available JAX-RS {@link Providers} to convert the entity.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p><strong>This class must not be directly instantiated and may change without warning!</strong></p>
 * <p>To obtain an {@link EntityConverter}, use {@link EntityConverter#fromRequestContext(ClientRequestContext)}.</p>
 */
public final class ProvidersEntityConverter extends EntityConverter {
    private static final Annotation[] ANNOTATIONS = new Annotation[]{};

    private final Providers providers;

    public ProvidersEntityConverter(Providers providers) {
        this.providers = providers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) throws IOException {
        validateNotNull(requestContext, "'requestContext' must not be null.");
        validateNotNull(type, "'type' must not be null.");
        assertEntityPresent(requestContext);

        if (canShortCircuit(requestContext, type, null)) {
            return (T) requestContext.getEntity();
        }
        return convertEntity(requestContext, type, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) throws IOException {
        validateNotNull(requestContext, "'requestContext' must not be null.");
        validateNotNull(genericType, "'genericType' must not be null.");

        assertEntityPresent(requestContext);
        if (canShortCircuit(requestContext, genericType.getRawType(), genericType.getType())) {
            return (T) requestContext.getEntity();
        }
        return convertEntity(requestContext, (Class<T>) genericType.getRawType(), genericType.getType());
    }

    @Override
    @SuppressWarnings("unchecked")
    List<EntityPart> serializeEntityParts(ClientRequestContext requestContext) throws IOException {
        return convertEntity(requestContext, (Class<List<EntityPart>>) ENTITY_PARTS.getRawType(), ENTITY_PARTS.getType());
    }

    @SuppressWarnings("unchecked")
    private <T> T convertEntity(ClientRequestContext requestContext, Class<T> type, Type genericType) throws IOException {
        MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(
                requestContext.getEntityClass(),
                requestContext.getEntityType(),
                requestContext.getEntityAnnotations(),
                requestContext.getMediaType()
        );

        if (writer == null) {
            throw new ProcessingException("Unable to obtain MessageBodyWriter for type=" + type + " and genericType=" + genericType);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.writeTo(
                requestContext.getEntity(),
                requestContext.getEntityClass(),
                requestContext.getEntityType(),
                requestContext.getEntityAnnotations(),
                requestContext.getMediaType(),
                requestContext.getHeaders(),
                outputStream
        );

        MessageBodyReader<T> reader = providers.getMessageBodyReader(
                type,
                genericType,
                ANNOTATIONS,
                requestContext.getMediaType()
        );

        if (reader == null) {
            throw new ProcessingException("Unable to obtain MessageBodyReader for type=" + type + " and genericType=" + genericType);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return reader.readFrom(
                type,
                genericType,
                ANNOTATIONS,
                requestContext.getMediaType(),
                requestContext.getStringHeaders(),
                inputStream
        );
    }
}
