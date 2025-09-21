package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;

import org.jspecify.annotations.Nullable;

/**
 * Utility class for {@link RequestMatcher} implementations to convert a request {@code entity} into another type, e.g. from a POJO into a String.
 * <p>Can be obtained inside a {@code RequestMatcher} implementation via {@link #fromRequestContext(ClientRequestContext)}.</p>
 * <pre><code>
 *  RequestMatcher customMatcher = request -> {
 *      EntityConverter converter = EntityConverter.fromRequestContext(request);
 *      String jsonString = converter.convert(request, String.class);
 *      // assertions on the JSON String
 *  }
 *
 *  // Client and MockRestServer setup
 *
 *  server.expect(customMatcher).andRespond(MockResponseCreators.withSuccess());
 *
 *  // execute request
 * </code></pre>
 */
public abstract sealed class EntityConverter permits ClientEntityConverter, ProvidersEntityConverter {

    EntityConverter() {}

    /**
     * Obtain the {@code EntityConverter} from the current {@link ClientRequestContext} while inside a {@code RequestMatcher}.
     * <p>Will throw an {@link IllegalStateException} when called outside a {@code RequestMatcher}.</p>
     *
     * @param requestContext The current request
     * @return The {@code EntityConverter} instance
     */
    public static EntityConverter fromRequestContext(ClientRequestContext requestContext) {
        validateNotNull(requestContext, "'requestContext' must not be null.");
        if (requestContext.getProperty(EntityConverter.class.getName()) instanceof EntityConverter entityConverter) {
            return entityConverter;
        }

        throw new IllegalStateException("Unable to obtain EntityConverter from RequestContext.");
    }

    /**
     * <p>Buffer the given {@code List<EntityPart>} to allow for repeated accessing of the part's contents.</p>
     * <p>The buffered {@code EntityParts} implement {@code equals}, so their equality can be asserted inside a {@code RequestMatcher}.</p>
     *
     * @param entityParts The {@code EntityParts} to buffer
     * @return The buffered {@code EntityParts}
     * @throws IOException If an I/O error occurs during buffering
     */
    public List<EntityPart> bufferExpectedMultipart(List<EntityPart> entityParts) throws IOException {
        validateNotNull(entityParts, "'expectedParts' must not be null.");
        List<EntityPart> bufferedParts = new ArrayList<>();
        for (EntityPart entityPart : serializeEntityParts(new MultiPartRequestContext(entityParts))) {
            bufferedParts.add(new BufferedEntityPart(entityPart, this));
        }

        return bufferedParts;
    }

    /**
     * <p>
     * Buffer the request entity of type {@code List<EntityPart>} to allow for repeated accessing
     * of the part's contents and set it as the request entity.
     * </p>
     * <p>The buffered {@code EntityParts} implement {@code equals}, so their equality can be asserted inside a {@code RequestMatcher}.</p>
     *
     * @param requestContext The current request
     * @return The buffered request entity
     * @throws IOException If an I/O error occurs during buffering
     */
    public List<EntityPart> bufferMultipartRequest(ClientRequestContext requestContext) throws IOException {
        assertMultiPartEntityPresent(requestContext);

        List<BufferedEntityPart> bufferedParts = new ArrayList<>();
        for (EntityPart entityPart : serializeEntityParts(requestContext)) {
            bufferedParts.add(new BufferedEntityPart(entityPart, this));
        }

        // RESTEasy will reuse the boundary parameter if it's set on the Content-Type header
        // so it must be reset after each serialization to prevent silent parsing failures
        requestContext.getHeaders().putSingle(CONTENT_TYPE, MULTIPART_FORM_DATA);

        requestContext.setEntity(recreateEntityParts(bufferedParts));

        return new ArrayList<>(bufferedParts);
    }

    /**
     * Obtain the entity from the current {@link ClientRequestContext} and convert it to the type.
     *
     * @param requestContext The current request
     * @param type           The target type
     * @return The converted entity
     * @throws IOException If an I/O error occurs during conversion
     */
    public abstract <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) throws IOException;

    /**
     * Obtain the entity from the current {@link ClientRequestContext} and convert it to the generic type.
     *
     * @param requestContext The current request
     * @param genericType    The target type
     * @return The converted entity
     * @throws IOException If an I/O error occurs during conversion
     */
    public abstract <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) throws IOException;

    abstract List<EntityPart> serializeEntityParts(ClientRequestContext requestContext) throws IOException;

    static boolean canShortCircuit(ClientRequestContext requestContext, Class<?> type, @Nullable Type genericType) {
        if (genericType == null) {
            return type.isAssignableFrom(requestContext.getEntityClass());
        }
        return type.isAssignableFrom(requestContext.getEntityClass())
                && Objects.equals(requestContext.getEntityType(), genericType);
    }

    static void assertEntityPresent(ClientRequestContext requestContext) {
        if (!requestContext.hasEntity()) {
            throw new AssertionError("Request contains no entity to convert.");
        }
    }

    static void assertMultiPartEntityPresent(ClientRequestContext requestContext) {
        validateNotNull(requestContext, "'requestContext' must not be null.");
        assertEntityPresent(requestContext);
        if (!MULTIPART_FORM_DATA_TYPE.equals(requestContext.getMediaType())) {
            throw new AssertionError("MediaType must be " + MULTIPART_FORM_DATA);
        }
    }

    // Jersey expects each EntityPart to be an instance of org.glassfish.jersey.media.multipart.BodyPart which BufferedEntityPart isn't,
    // so the EntityParts that replace the request entity must be recreated to prevent ClassCastExceptions during serialization.
    // RESTEasy doesn't care so the EntityParts are recreated for it as well.
    private static GenericEntity<List<EntityPart>> recreateEntityParts(List<BufferedEntityPart> parts) throws IOException {
        List<EntityPart> recreated = new ArrayList<>(parts.size());
        for (BufferedEntityPart part : parts) {
            EntityPart.Builder builder;
            if (part.getFileName().isPresent()) {
                builder = EntityPart.withFileName(part.getFileName().get()).content(new ByteArrayInputStream(part.getBuffer()));
            } else {
                builder = EntityPart.withName(part.getName()).content(part.getBuffer());
            }

            recreated.add(builder.headers(part.getHeaders()).mediaType(part.getMediaType()).build());
        }

        return new GenericEntity<>(recreated) {};
    }
}
