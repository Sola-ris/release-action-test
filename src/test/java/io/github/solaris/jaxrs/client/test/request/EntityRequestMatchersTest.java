package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.imagePart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.jsonPart;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.partsBufferMatcher;
import static io.github.solaris.jaxrs.client.test.util.MultiParts.plainPart;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.JERSEY;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.RESTEASY_REACTIVE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_ATOM_XML;
import static jakarta.ws.rs.core.MediaType.APPLICATION_ATOM_XML_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.Dto;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;
import io.github.solaris.jaxrs.client.test.util.MultiParts.PartsBuffer;
import io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendorTest;

class EntityRequestMatchersTest {

    @JaxRsVendorTest
    void testMediaType() {
        assertThatCode(() -> RequestMatchers.entity().mediaType(APPLICATION_JSON_TYPE).match(new MockClientRequestContext(APPLICATION_JSON_TYPE)))
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testMediaType_notSet() {
        assertThatThrownBy(
                () -> RequestMatchers.entity()
                        .mediaType(APPLICATION_ATOM_XML_TYPE)
                        .match(new MockClientRequestContext((MediaType) null)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("MediaType was not set.");
    }

    @JaxRsVendorTest
    void testMediaType_noMatch() {
        assertThatThrownBy(
                () -> RequestMatchers.entity()
                        .mediaType(APPLICATION_JSON_TYPE)
                        .match(new MockClientRequestContext(APPLICATION_OCTET_STREAM_TYPE)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("MediaType expected: <%s> but was: <%s>", APPLICATION_JSON_TYPE, APPLICATION_OCTET_STREAM_TYPE);
    }

    @JaxRsVendorTest
    void testMediaType_string() {
        assertThatCode(() -> RequestMatchers.entity().mediaType(APPLICATION_JSON).match(new MockClientRequestContext(APPLICATION_JSON_TYPE)))
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testMediaType_string_notSet() {
        assertThatThrownBy(
                () -> RequestMatchers.entity()
                        .mediaType(APPLICATION_ATOM_XML)
                        .match(new MockClientRequestContext((MediaType) null)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("MediaType was not set.");
    }

    @JaxRsVendorTest
    void testMediaType_string_noMatch() {
        assertThatThrownBy(
                () -> RequestMatchers.entity()
                        .mediaType(APPLICATION_JSON)
                        .match(new MockClientRequestContext(APPLICATION_OCTET_STREAM_TYPE)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("MediaType expected: <%s> but was: <%s>", APPLICATION_JSON, APPLICATION_OCTET_STREAM);
    }

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void testArgumentValidation(ThrowingCallable callable, String exceptionMessage) {
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(exceptionMessage);
    }

    @SuppressWarnings("DataFlowIssue")
    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
                argumentSet("testMediaType_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().mediaType((MediaType) null), "'mediaType' must not be null."),
                argumentSet("testHeader_string_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().mediaType((String) null), "'mediaType' must not be null."),
                argumentSet("testString_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().string(null), "'expectedString' must not be null."),
                argumentSet("testForm_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().form(null), "'expectedForm' must not be null."),
                argumentSet("testFormContains_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().formContains(null), "'expectedForm' must not be null."),
                argumentSet("testMultipartForm_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().multipartForm(null), "'expectedEntityParts' must not be null."),
                argumentSet("testMultipartFormContains_null",
                        (ThrowingCallable) () -> RequestMatchers.entity().multipartFormContains(null), "'expectedEntityParts' must not be null.")
        );
    }

    @Nested
    class WithClient {

        @AutoClose
        private final Client client = ClientBuilder.newClient();

        private final MockRestServer server = MockRestServer.bindTo(client).build();

        @JaxRsVendorTest
        void testIsEqualTo() {
            Dto dto = new Dto(true);

            server.expect(RequestMatchers.entity().isEqualTo(dto)).andRespond(withSuccess());

            assertThatCode(() -> client.target("/hello").request().post(Entity.entity(dto, TEXT_PLAIN_TYPE)).close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest
        void testIsEqualTo_null() {
            server.expect(RequestMatchers.entity().isEqualTo(null)).andRespond(withSuccess());

            assertThatCode(() -> client.target("").request().get().close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest
        void testIsEqualTo_noMatch() {
            Dto dto = new Dto(true);

            server.expect(RequestMatchers.entity().isEqualTo(dto)).andRespond(withSuccess());

            assertThatCode(() -> client.target("/hello").request().post(Entity.entity(dto, TEXT_PLAIN_TYPE)).close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest
        void testString() {
            Dto dto = new Dto(true);

            server.expect(RequestMatchers.entity().string(dto.toString())).andRespond(withSuccess());

            assertThatCode(() -> client.target("/hello")
                    .request()
                    .post(Entity.entity(dto.toString(), TEXT_PLAIN_TYPE)).close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest
        void testString_noMatch(FilterExceptionAssert filterExceptionAssert) {
            Dto dto = new Dto(true);
            Dto otherDto = new Dto(false);

            server.expect(RequestMatchers.entity().string(dto.toString())).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.entity(otherDto.toString().getBytes(), TEXT_PLAIN_TYPE)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Entity String expected: <%s> but was: <%s>", dto.toString(), otherDto.toString());
        }

        @JaxRsVendorTest
        void testForm() {
            Form form = new Form("greeting", "hello");

            server.expect(RequestMatchers.entity().form(form)).andRespond(withSuccess());

            assertThatCode(() -> client.target("/hello").request(APPLICATION_JSON_TYPE).post(Entity.form(form)).close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest
        void testForm_noMatch(FilterExceptionAssert filterExceptionAssert) {
            Form form = new Form("greeting", "hello");
            Form otherForm = new Form("sendoff", "goodbye");

            server.expect(RequestMatchers.entity().form(form)).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.form(otherForm))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Form expected: <%s> but was: <%s>", form.asMap(), otherForm.asMap());
        }

        @JaxRsVendorTest
        void testFormContains() {
            Form actualForm = new Form()
                    .param("greeting", "hello")
                    .param("greeting", "salutations")
                    .param("sendoff", "goodbye")
                    .param("sendoff", "farewell");

            Form subset = new Form()
                    .param("greeting", "hello")
                    .param("sendoff", "goodbye");

            server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

            assertThatCode(() -> client.target("/hello").request().post(Entity.form(actualForm)).close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest
        void testFormContains_subsetIsLarger(FilterExceptionAssert filterExceptionAssert) {
            Form actualForm = new Form()
                    .param("sendoff", "goodbye")
                    .param("greeting", "hello");

            Form subset = new Form()
                    .param("greeting", "hello")
                    .param("sendoff", "goodbye")
                    .param("question", "how are you?");

            server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.form(actualForm))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected " + subset.asMap() + " to be smaller or the same size as " + actualForm.asMap());
        }

        @JaxRsVendorTest
        void testFormContains_parameterNotInSubset(FilterExceptionAssert filterExceptionAssert) {
            Form actualForm = new Form()
                    .param("sendoff", "goodbye")
                    .param("greeting", "hello");

            Form subset = new Form()
                    .param("question", "how are you?");

            server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.form(actualForm))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected " + actualForm.asMap() + " to contain parameter 'question'");
        }

        @JaxRsVendorTest
        void testFormContains_subsetHasMoreValues(FilterExceptionAssert filterExceptionAssert) {
            Form actualForm = new Form()
                    .param("greeting", "hello");

            Form subset = new Form()
                    .param("greeting", "hello")
                    .param("greeting", "salutations");

            server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.form(actualForm))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected %s to be smaller or the same size as %s",
                            subset.asMap().get("greeting"), actualForm.asMap().get("greeting"));
        }

        @JaxRsVendorTest
        void testFormContains_subsetHasDifferentValue(FilterExceptionAssert filterExceptionAssert) {
            Form actualForm = new Form()
                    .param("greeting", "hello")
                    .param("greeting", "salutations");

            Form subset = new Form()
                    .param("greeting", "good morning");

            server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.form(actualForm))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("FormParam [name=greeting, position=0] expected: <good morning> but was: <hello>");
        }

        @JaxRsVendorTest
        void testFormContains_subsetHasDifferentOrder(FilterExceptionAssert filterExceptionAssert) {
            Form actualForm = new Form()
                    .param("greeting", "hello")
                    .param("greeting", "salutations")
                    .param("greeting", "good morning");

            Form subset = new Form()
                    .param("greeting", "salutations")
                    .param("greeting", "hello");

            server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.form(actualForm))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("FormParam [name=greeting, position=0] expected: <salutations> but was: <hello>");
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testMultipartForm() throws IOException {
            server.expect(RequestMatchers.entity().multipartForm(List.of(plainPart(), imagePart(), jsonPart()))).andRespond(withSuccess());

            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart(), imagePart(), jsonPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testMultipartForm_noMatch(FilterExceptionAssert filterExceptionAssert) throws IOException {
            AtomicReference<PartsBuffer> partsBuffer = new AtomicReference<>();
            server.expect(partsBufferMatcher(List.of(plainPart()), partsBuffer))
                    .andExpect(RequestMatchers.entity().multipartForm(List.of(plainPart()))).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart(), imagePart(), jsonPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Multipart Form expected: <%s> but was: <%s>", partsBuffer.get().expected(), partsBuffer.get().actual());
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testMultipartForm_noMatch_wrongOrder(FilterExceptionAssert filterExceptionAssert) throws IOException {
            AtomicReference<PartsBuffer> partsBuffer = new AtomicReference<>();
            server.expect(partsBufferMatcher(List.of(jsonPart(), imagePart(), plainPart()), partsBuffer))
                    .andExpect(RequestMatchers.entity().multipartForm(List.of(jsonPart(), imagePart(), plainPart()))).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart(), imagePart(), jsonPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Multipart Form expected: <%s> but was: <%s>", partsBuffer.get().expected(), partsBuffer.get().actual());
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testMultipartFormContains() throws IOException {
            server.expect(RequestMatchers.entity().multipartFormContains(List.of(plainPart(), jsonPart()))).andRespond(withSuccess());

            assertThatCode(
                    () -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart(), imagePart(), jsonPart())) {}, MULTIPART_FORM_DATA_TYPE))
                            .close())
                    .doesNotThrowAnyException();
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testMultipartFormContains_subsetIsLarger(FilterExceptionAssert filterExceptionAssert) throws IOException {
            AtomicReference<PartsBuffer> partsBuffer = new AtomicReference<>();
            server.expect(partsBufferMatcher(List.of(jsonPart(), imagePart(), plainPart()), partsBuffer))
                    .andExpect(RequestMatchers.entity().multipartFormContains(List.of(jsonPart(), imagePart(), plainPart())))
                    .andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart(), imagePart())) {}, MULTIPART_FORM_DATA_TYPE)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected %s to be smaller or the same size as %s", partsBuffer.get().expected(), partsBuffer.get().actual());
        }

        @JaxRsVendorTest(skipFor = {JERSEY, RESTEASY_REACTIVE})
        void testMultipartFormContains_noMatch(FilterExceptionAssert filterExceptionAssert) throws IOException {
            AtomicReference<PartsBuffer> partsBuffer = new AtomicReference<>();
            server.expect(partsBufferMatcher(List.of(jsonPart()), partsBuffer))
                    .andExpect(RequestMatchers.entity().multipartFormContains(List.of(jsonPart()))).andRespond(withSuccess());

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                            .request()
                            .post(Entity.entity(new GenericEntity<>(List.of(plainPart(), imagePart())) {}, MULTIPART_FORM_DATA_TYPE)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected %s to contain all of %s", partsBuffer.get().actual(), partsBuffer.get().expected());
        }
    }
}
