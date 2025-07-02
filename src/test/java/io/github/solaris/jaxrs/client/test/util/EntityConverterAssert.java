package io.github.solaris.jaxrs.client.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Providers;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.github.solaris.jaxrs.client.test.request.ClientEntityConverter;
import io.github.solaris.jaxrs.client.test.request.EntityConverter;
import io.github.solaris.jaxrs.client.test.request.ProvidersEntityConverter;
import io.github.solaris.jaxrs.client.test.request.RequestMatcher;

public abstract sealed class EntityConverterAssert {
    private static final GenericType<MultivaluedMap<String, String>> GENERIC_TYPE = new GenericType<>() {};

    public abstract RequestMatcher typeAsserter(Object expectedEntity, int times);

    public abstract RequestMatcher genericTypeAsserter(Object expectedEntity, int times);

    public abstract void assertConversionFailure(ThrowingCallable throwingCallable);

    public static final class ClientEntityConverterAssert extends EntityConverterAssert {

        @Override
        public RequestMatcher typeAsserter(Object expectedEntity, int times) {
            return asserter(expectedEntity, times, (converter, requestContext) -> converter.convertEntity(requestContext, String.class));
        }

        @Override
        public RequestMatcher genericTypeAsserter(Object expectedEntity, int times) {
            return asserter(expectedEntity, times, (converter, requestContext) -> converter.convertEntity(requestContext, GENERIC_TYPE));
        }

        @Override
        public void assertConversionFailure(ThrowingCallable throwingCallable) {
            assertThatThrownBy(throwingCallable).isInstanceOf(ProcessingException.class);
        }

        private <T> RequestMatcher asserter(
                Object expectedEntity, int times, ThrowingBiFunction<EntityConverter, ClientRequestContext, T> conversionFunction) {
            return request -> {
                Client client = ClientBuilder.newClient();
                try (MockedStatic<ClientBuilder> builderMock = Mockito.mockStatic(ClientBuilder.class)) {
                    builderMock.when(ClientBuilder::newClient).thenReturn(client);

                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    assertThat(converter)
                            .isNotNull()
                            .isInstanceOf(ClientEntityConverter.class);
                    T actualEntity = conversionFunction.apply(converter, request);
                    assertThat(actualEntity).isEqualTo(expectedEntity);

                    builderMock.verify(ClientBuilder::newClient, times(times));
                }
            };
        }
    }

    public static final class ProvidersEntityConverterAssert extends EntityConverterAssert {
        private Providers providersSpy;

        @Override
        public RequestMatcher typeAsserter(Object expectedEntity, int times) {
            return request -> {
                EntityConverter converter = EntityConverter.fromRequestContext(request);

                assertThat(converter)
                        .isNotNull()
                        .isInstanceOf(ProvidersEntityConverter.class);

                String entityString = recreateConverterWithSpy(converter).convertEntity(request, String.class);

                assertThat(entityString).isEqualTo(expectedEntity);

                verify(providersSpy, times(times)).getMessageBodyWriter(
                        request.getEntityClass(),
                        request.getEntityType(),
                        request.getEntityAnnotations(),
                        request.getMediaType());
                verify(providersSpy, times(times)).getMessageBodyReader(
                        String.class,
                        String.class,
                        request.getEntityAnnotations(),
                        request.getMediaType());
            };
        }

        @Override
        public RequestMatcher genericTypeAsserter(Object expectedEntity, int times) {
            return request -> {
                EntityConverter converter = EntityConverter.fromRequestContext(request);

                assertThat(converter)
                        .isNotNull()
                        .isInstanceOf(ProvidersEntityConverter.class);

                MultivaluedMap<String, String> entityMap = recreateConverterWithSpy(converter).convertEntity(request, GENERIC_TYPE);

                assertThat(entityMap).isEqualTo(expectedEntity);

                verify(providersSpy, times(times)).getMessageBodyWriter(
                        request.getEntityClass(),
                        request.getEntityType(),
                        request.getEntityAnnotations(),
                        request.getMediaType());
                verify(providersSpy, times(times)).getMessageBodyReader(
                        GENERIC_TYPE.getRawType(),
                        GENERIC_TYPE.getType(),
                        request.getEntityAnnotations(),
                        request.getMediaType());
            };
        }

        @Override
        public void assertConversionFailure(ThrowingCallable throwingCallable) {
            assertThatThrownBy(throwingCallable)
                    .isInstanceOf(ProcessingException.class)
                    .hasMessageMatching("^Unable to obtain MessageBody(Reader|Writer) for type=class.+? and genericType=.*$");
        }

        private ProvidersEntityConverter recreateConverterWithSpy(EntityConverter converter) {
            try {
                Field providersField = converter.getClass().getDeclaredField("providers");
                providersField.setAccessible(true);
                Providers actualProviders = (Providers) providersField.get(converter);

                providersSpy = Mockito.spy(actualProviders);

                return new ProvidersEntityConverter(providersSpy);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingBiFunction<T, U, R> {
        R apply(T t, U u) throws IOException;
    }
}
