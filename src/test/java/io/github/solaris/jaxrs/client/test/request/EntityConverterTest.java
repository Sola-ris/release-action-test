package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.LIST_CONTENT;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.PLAIN_CONTENT;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.jsonPart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.listPart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.plainPart;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.CXF;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.JERSEY;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.RESTEASY_REACTIVE;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.Dto;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

class EntityConverterTest {

    @JaxRsVendorTest
    void testConvertEntity_type(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        String entity = "hello";

        server.expect(converterAssert.typeAsserter(entity, 1)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.text(entity.getBytes()))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testConvertEntity_type_shortCircuit(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        String entity = "hello";

        server.expect(converterAssert.typeAsserter(entity, 0)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.text(entity))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");

        server.expect(converterAssert.genericTypeAsserter(form.asMap(), 1)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.form(form))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType_shortCircuit(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");
        GenericEntity<MultivaluedMap<String, String>> genericMap = new GenericEntity<>(form.asMap()) {};

        server.expect(converterAssert.genericTypeAsserter(form.asMap(), 0)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.entity(genericMap, APPLICATION_FORM_URLENCODED_TYPE))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testUnableToConvertEntity_writingFails(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, Dto.class);
        }).andRespond(withSuccess());

        try (client) {
            converterAssert.assertConversionFailure(() -> client.target("/hello").request().post(Entity.form(form)).close());
        }
    }

    @JaxRsVendorTest
    void testUnableToConvertEntity_readingFails(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, Dto.class);
        }).andRespond(withSuccess());

        try (client) {
            converterAssert.assertConversionFailure(() -> client.target("/hello")
                    .request()
                    .post(Entity.entity(Class.class, TEXT_HTML_TYPE))
                    .close());
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferExpectedMultipart_repeatedReads() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart plainPart = converter.bufferExpectedMultipart(List.of(plainPart())).get(0);

            assertThat(plainPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request().get().close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferExpectedMultipart_repeatedTypedReads() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart jsonPart = converter.bufferExpectedMultipart(List.of(jsonPart())).get(0);

            assertThat(jsonPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
            assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request().get().close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferExpectedMultipart_repeatedGenericReads() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart listPart = converter.bufferExpectedMultipart(List.of(listPart())).get(0);

            assertThat(listPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request().get().close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_repeatedReads() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart plainPart = converter.bufferMultipartRequest(request).get(0);

            assertThat(plainPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
            assertThat(plainPart.getContent().readAllBytes()).isEqualTo(PLAIN_CONTENT.getBytes());
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_repeatedTypedReads() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart jsonPart = converter.bufferMultipartRequest(request).get(0);

            assertThat(jsonPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
            assertThat(jsonPart.getContent(Dto.class)).isEqualTo(new Dto(false));
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(jsonPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_repeatedGenericReads() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart listPart = converter.bufferMultipartRequest(request).get(0);

            assertThat(listPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
            assertThat(listPart.getContent(new GenericType<List<String>>() {})).isEqualTo(LIST_CONTENT);
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(listPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_boundaryRemoved() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            EntityPart plainPart = converter.bufferMultipartRequest(request).get(0);

            assertThat(plainPart).isInstanceOf(BufferedEntityPart.class);
            assertThat(request.getHeaderString(CONTENT_TYPE))
                    .contains(MULTIPART_FORM_DATA)
                    .doesNotContain("boundary");
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest(skipFor = {JERSEY, CXF, RESTEASY_REACTIVE})
    void testBufferMultipartRequest_entityPartsRecreated() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            @SuppressWarnings("unchecked")
            EntityPart entityBefore = ((List<EntityPart>) request.getEntity()).get(0);

            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.bufferMultipartRequest(request);

            @SuppressWarnings("unchecked")
            EntityPart entityAfter = ((List<EntityPart>) request.getEntity()).get(0);

            assertThat(entityBefore).isNotSameAs(entityAfter);
        }).andRespond(withSuccess());

        try (client) {
            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }
    }
}
