package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.LIST_CONTENT;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.PLAIN_CONTENT;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.imagePart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.jsonPart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.listPart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.plainPart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.toMultiPartEntity;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.JERSEY;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.RESTEASY_REACTIVE;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static jakarta.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.Dto;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.extension.vendor.EnableJackson3;
import io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendorTest;

class EntityConverterTest {

    @AutoClose
    private final Client client = ClientBuilder.newClient();

    private final MockRestServer server = MockRestServer.bindTo(client).build();

    @JaxRsVendorTest
    void testConvertEntity_type(EntityConverterAssert converterAssert) {
        String entity = "hello";

        server.expect(converterAssert.typeAsserter(entity, 1)).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Response response = client.target("/hello").request().post(Entity.text(entity.getBytes()))) {
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testConvertEntity_type_shortCircuit(EntityConverterAssert converterAssert) {
        String entity = "hello";

        server.expect(converterAssert.typeAsserter(entity, 0)).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Response response = client.target("/hello").request().post(Entity.text(entity))) {
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testConvertEntity_type_noEntityPresent(FilterExceptionAssert filterExceptionAssert) {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, String.class);
        }).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("").request().get().close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Request contains no entity to convert.");
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType(EntityConverterAssert converterAssert) {
        Form form = new Form("greeting", "hello");

        server.expect(converterAssert.genericTypeAsserter(form.asMap(), 1)).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Response response = client.target("/hello").request().post(Entity.form(form))) {
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType_shortCircuit(EntityConverterAssert converterAssert) {
        Form form = new Form("greeting", "hello");
        GenericEntity<MultivaluedMap<String, String>> genericMap = new GenericEntity<>(form.asMap()) {};

        server.expect(converterAssert.genericTypeAsserter(form.asMap(), 0)).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Response response = client.target("/hello").request().post(Entity.entity(genericMap, APPLICATION_FORM_URLENCODED_TYPE))) {
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType_noEntityPresent(FilterExceptionAssert filterExceptionAssert) {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, new GenericType<MultivaluedMap<String, String>>() {});
        }).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("").request().get().close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Request contains no entity to convert.");
    }

    @JaxRsVendorTest
    void testUnableToConvertEntity_writingFails(EntityConverterAssert converterAssert) {
        Form form = new Form("greeting", "hello");

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, Dto.class);
        }).andRespond(withSuccess());

        converterAssert.assertConversionFailure(() -> client.target("/hello").request().post(Entity.form(form)).close());
    }

    @JaxRsVendorTest
    void testUnableToConvertEntity_readingFails(EntityConverterAssert converterAssert) {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, Dto.class);
        }).andRespond(withSuccess());

        converterAssert.assertConversionFailure(() -> client.target("/hello")
                .request()
                .post(Entity.entity(Class.class, TEXT_HTML_TYPE))
                .close());
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferExpectedMultipart_repeatedReads() {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart plainPart = converter.bufferExpectedMultipart(List.of(plainPart())).getFirst();

            assertThat(plainPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
        }).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().get().close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferExpectedMultipart_repeatedGenericReads() {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart listPart = converter.bufferExpectedMultipart(List.of(listPart())).getFirst();

            assertThat(listPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
        }).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().get().close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_repeatedReads() {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart plainPart = converter.bufferMultipartRequest(request).getFirst();

            assertThat(plainPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
        }).andRespond(withSuccess());

        assertThatCode(
                () -> client.target("/hello")
                        .request()
                        .post(toMultiPartEntity(plainPart()))
                        .close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_repeatedGenericReads() {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart listPart = converter.bufferMultipartRequest(request).getFirst();

            assertThat(listPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
        }).andRespond(withSuccess());

        assertThatCode(
                () -> client.target("/hello")
                        .request()
                        .post(toMultiPartEntity(listPart()))
                        .close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_boundaryRemoved() {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart plainPart = converter.bufferMultipartRequest(request).getFirst();

            assertThat(plainPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(request.getHeaderString(CONTENT_TYPE))
                    .contains(MULTIPART_FORM_DATA)
                    .doesNotContain("boundary");
        }).andRespond(withSuccess());

        assertThatCode(
                () -> client.target("/hello")
                        .request()
                        .post(toMultiPartEntity(plainPart()))
                        .close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_entityPartsRecreated() {
        server.expect(request -> {
            @SuppressWarnings("unchecked")
            EntityPart entityBefore = ((List<EntityPart>) request.getEntity()).getFirst();

            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.bufferMultipartRequest(request);

            @SuppressWarnings("unchecked")
            EntityPart entityAfter = ((List<EntityPart>) request.getEntity()).getFirst();

            assertThat(entityBefore).isNotSameAs(entityAfter);
        }).andRespond(withSuccess());

        assertThatCode(
                () -> client.target("/hello")
                        .request()
                        .post(toMultiPartEntity(plainPart()))
                        .close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_notMultiPartFormData(FilterExceptionAssert filterExceptionAssert) {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.bufferMultipartRequest(request);
        }).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("").request().post(Entity.json(new Dto("hello"))).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("MediaType must be %s", MULTIPART_FORM_DATA);
    }

    @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
    void testBufferedMultipart_nonCharsetParameterOnContentTypeRetained() throws IOException {
        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            List<EntityPart> fromRequest = converter.bufferMultipartRequest(request);

            MediaType expectedMediaType = new MediaType("text", "plain", Map.of("X-Custom-Param", "hello"));
            assertThat(fromRequest)
                    .singleElement()
                    .satisfies(part -> assertThat(part.getHeaders()).containsEntry(CONTENT_TYPE, List.of(expectedMediaType.toString())));
        }).andRespond(withSuccess());

        MediaType mediaType = new MediaType(
                "text", "plain", Map.of("X-Custom-Param", "hello", CHARSET_PARAMETER, US_ASCII.displayName())
        );
        EntityPart entityPart = EntityPart.withName("plainWithCustomParam")
                .content(PLAIN_CONTENT)
                .mediaType(mediaType)
                .header(CONTENT_TYPE, mediaType.toString()) // CXF doesn't copy the Content-Type header into the headers Map
                .header(CONTENT_LENGTH, String.valueOf(PLAIN_CONTENT.length()))
                .build();

        assertThatCode(
                () -> client.target("/hello")
                        .request()
                        .post(toMultiPartEntity(entityPart))
                        .close())
                .doesNotThrowAnyException();
    }

    @Nested
    @EnableJackson3
    class WithJson {

        @AutoClose
        private final Client jsonClient = ClientBuilder.newClient();

        private final MockRestServer jsonServer = MockRestServer.bindTo(jsonClient).build();

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testBufferExpectedMultipart_repeatedTypedReads() {
            jsonServer.expect(request -> {
                EntityConverter converter = EntityConverter.fromRequestContext(request);
                EntityPart jsonPart = converter.bufferExpectedMultipart(List.of(jsonPart())).getFirst();

                assertThat(jsonPart).isInstanceOf(BufferedEntityPart.class);
                assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
                assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
            }).andRespond(withSuccess());

            assertThatCode(() -> jsonClient.target("/hello").request().get().close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testBufferMultipartRequest_repeatedTypedReads() {
            jsonServer.expect(request -> {
                EntityConverter converter = EntityConverter.fromRequestContext(request);
                EntityPart jsonPart = converter.bufferMultipartRequest(request).getFirst();

                assertThat(jsonPart).isInstanceOf(BufferedEntityPart.class);
                assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
                assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
            }).andRespond(withSuccess());

            assertThatCode(
                    () -> jsonClient.target("/hello")
                            .request()
                            .post(toMultiPartEntity(jsonPart()))
                            .close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testBufferedMultipart_consistentHashCode() {
            jsonServer.expect(request -> {
                EntityConverter converter = EntityConverter.fromRequestContext(request);
                List<EntityPart> fromRequest = converter.bufferMultipartRequest(request);
                List<EntityPart> expectedParts = converter.bufferExpectedMultipart(List.of(jsonPart(), plainPart(), listPart(), imagePart()));

                for (int i = 0; i < fromRequest.size(); i++) {
                    assertThat(fromRequest.get(i)).hasSameHashCodeAs(expectedParts.get(i));
                }
            }).andRespond(withSuccess());

            assertThatCode(
                    () -> jsonClient.target("/hello")
                            .request()
                            .post(toMultiPartEntity(jsonPart(), plainPart(), listPart(), imagePart()))
                            .close())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @SuppressWarnings("DataFlowIssue")
    class ArgumentValidation {

        private void validateArguments(Client client, FilterExceptionAssert filterExceptionAssert, RequestMatcher matcher, String exceptionMessage) {
            MockRestServer validationServer = MockRestServer.bindTo(client).build();

            validationServer.expect(matcher);

            filterExceptionAssert.assertThatThrownBy(() -> client.target("").request().get().close())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(exceptionMessage);
        }

        @Nested
        class VendorSpecific {

            @AutoClose
            private final Client validationClient = ClientBuilder.newClient();

            @JaxRsVendorTest
            void testConvertEntity_type_requestNull(FilterExceptionAssert filterExceptionAssert) {
                RequestMatcher matcher = request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    converter.convertEntity(null, String.class);
                };
                validateArguments(validationClient, filterExceptionAssert, matcher, "'requestContext' must not be null.");
            }

            @JaxRsVendorTest
            void testConvertEntity_type_typeNull(FilterExceptionAssert filterExceptionAssert) {
                RequestMatcher matcher = request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    converter.convertEntity(request, (Class<?>) null);
                };
                validateArguments(validationClient, filterExceptionAssert, matcher, "'type' must not be null.");
            }

            @JaxRsVendorTest
            void testConvertEntity_genericType_requestNull(FilterExceptionAssert filterExceptionAssert) {
                RequestMatcher matcher = request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    converter.convertEntity(null, new GenericType<>() {});
                };
                validateArguments(validationClient, filterExceptionAssert, matcher, "'requestContext' must not be null.");
            }

            @JaxRsVendorTest
            void testConvertEntity_genericType_genericTypeNull(FilterExceptionAssert filterExceptionAssert) {
                RequestMatcher matcher = request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    converter.convertEntity(request, (GenericType<?>) null);
                };
                validateArguments(validationClient, filterExceptionAssert, matcher, "'genericType' must not be null.");
            }
        }

        @Nested
        class VendorUnspecific {

            @AutoClose
            private static final Client VALIDATION_CLIENT = ClientBuilder.newClient();

            @BeforeAll
            static void reset() {
                RuntimeDelegate.setInstance(null);
            }

            @Test
            void testFromRequestContext_converterUnobtainable() {
                MockRestServer validationServer = MockRestServer.bindTo(VALIDATION_CLIENT).build();

                validationServer.expect(request -> {
                    request.setProperty(EntityConverter.class.getName(), "hello");
                    EntityConverter.fromRequestContext(request);
                });

                new DefaultFilterExceptionAssert().assertThatThrownBy(() -> VALIDATION_CLIENT.target("").request().get().close())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Unable to obtain EntityConverter from RequestContext.");
            }

            @Test
            void testFromRequestContext_requestNull() {
                RequestMatcher matcher = _ -> EntityConverter.fromRequestContext(null);
                validateArguments(VALIDATION_CLIENT, new DefaultFilterExceptionAssert(), matcher, "'requestContext' must not be null.");
            }

            @Test
            void testBufferExpectedMultipart_null() {
                RequestMatcher matcher = request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    converter.bufferExpectedMultipart(null);
                };
                validateArguments(VALIDATION_CLIENT, new DefaultFilterExceptionAssert(), matcher, "'expectedParts' must not be null.");
            }

            @Test
            void testBufferExpectedMultipart_empty() {
                MockRestServer validationServer = MockRestServer.bindTo(VALIDATION_CLIENT).build();
                validationServer.expect(request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);

                    List<EntityPart> parts = new ArrayList<>();
                    List<EntityPart> bufferedParts = converter.bufferExpectedMultipart(parts);

                    assertThat(parts)
                            .isSameAs(bufferedParts)
                            .isEmpty();
                }).andRespond(withSuccess());

                assertThatCode(() -> VALIDATION_CLIENT.target("").request().get().close())
                        .doesNotThrowAnyException();
            }

            @Test
            void testBufferMultipartRequest_null() {
                RequestMatcher matcher = request -> {
                    EntityConverter converter = EntityConverter.fromRequestContext(request);
                    converter.bufferMultipartRequest(null);
                };
                validateArguments(VALIDATION_CLIENT, new DefaultFilterExceptionAssert(), matcher, "'requestContext' must not be null.");
            }
        }
    }
}
