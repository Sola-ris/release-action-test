package io.github.solaris.jaxrs.client.test.request;

import static jakarta.ws.rs.HttpMethod.HEAD;
import static jakarta.ws.rs.HttpMethod.PATCH;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;

class RequestMatchersTest {

    @Test
    void testAnything() {
        assertThatCode(() -> RequestMatchers.anything().match(new MockClientRequestContext())).doesNotThrowAnyException();
    }

    @Test
    void testMethod() {
        assertThatCode(() -> RequestMatchers.method(PATCH).match(new MockClientRequestContext(PATCH))).doesNotThrowAnyException();
    }

    @Test
    void testMethod_noMatch() {
        assertThatThrownBy(() -> RequestMatchers.method(HEAD).match(new MockClientRequestContext(PATCH)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Unexpected Method. expected: <%s> but was: <%s>", HEAD, PATCH);
    }

    @Test
    void testRequestTo_string() {
        assertThatCode(() -> RequestMatchers.requestTo("local.host").match(new MockClientRequestContext(URI.create("local.host"))))
                .doesNotThrowAnyException();
    }

    @Test
    void testRequestTo_string_noMatch() {
        assertThatThrownBy(() -> RequestMatchers.requestTo("local.host").match(new MockClientRequestContext(URI.create("remote.host"))))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Unexpected Request. expected: <local.host> but was: <remote.host>");
    }

    @Test
    void testRequestTo_uri() {
        URI uri = URI.create("local.host");
        assertThatCode(() -> RequestMatchers.requestTo(uri).match(new MockClientRequestContext(uri))).doesNotThrowAnyException();
    }

    @Test
    void testRequestTo_uri_noMatch() {
        assertThatThrownBy(() -> RequestMatchers.requestTo(URI.create("local.host")).match(new MockClientRequestContext(URI.create("remote.host"))))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Unexpected Request. expected: <local.host> but was: <remote.host>");
    }

    @Test
    void testQueryParam() {
        URI uri = URI.create("local.host?greeting=hello&greeting=salutations");
        assertThatCode(() -> RequestMatchers.queryParam("greeting", "hello", "salutations").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_noValue() {
        URI uri = URI.create("local.host?greeting");
        assertThatCode(() -> RequestMatchers.queryParam("greeting", "").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_noValueWithEqualsSign() {
        URI uri = URI.create("local.host?greeting=");
        assertThatCode(() -> RequestMatchers.queryParam("greeting", "").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_equalsSignInValue() {
        URI uri = URI.create("local.host?greeting=hello=salutations&sendoff=farewell");
        assertThatCode(() -> RequestMatchers.queryParam("greeting", "hello=salutations").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_paramMissing() {
        URI uri = URI.create("local.host?greeting=hello&greeting=salutations");
        assertThatThrownBy(() -> RequestMatchers.queryParam("sendoff", "goodbye", "farewell").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected QueryParam <sendoff> to exist but was null");
    }

    @Test
    void testQueryParam_paramMissing_noQuerySegment() {
        URI uri = URI.create("local.host");
        assertThatThrownBy(() -> RequestMatchers.queryParam("sendoff", "goodbye").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected QueryParam <sendoff> to exist but was null");
    }

    @Test
    void testQueryParam_paramMissing_emptyQuerySegment() {
        URI uri = URI.create("local.host?");
        assertThatThrownBy(() -> RequestMatchers.queryParam("sendoff", "goodbye").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected QueryParam <sendoff> to exist but was null");
    }

    @Test
    void testQueryParam_countMismatch() {
        URI uri = URI.create("local.host?greeting=hello");
        assertThatThrownBy(() -> RequestMatchers.queryParam("greeting", "hello", "salutations").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected QueryParam <greeting> to have at least <2> values but found [hello]");
    }

    @Test
    void testQueryParam_valueMismatch() {
        URI uri = URI.create("local.host?greeting=hello");
        assertThatThrownBy(() -> RequestMatchers.queryParam("greeting", "salutations").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("QueryParam [name=greeting, position=0] expected: <salutations> but was: <hello>");
    }

    @Test
    void testQueryParam_orderMismatch() {
        URI uri = URI.create("local.host?greeting=hello&greeting=salutations");
        assertThatThrownBy(() -> RequestMatchers.queryParam("greeting", "salutations", "hello").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("QueryParam [name=greeting, position=0] expected: <salutations> but was: <hello>");
    }

    @Test
    void testQueryParamDoesNotExist() {
        URI uri = URI.create("local.host");
        assertThatCode(() -> RequestMatchers.queryParamDoesNotExist("greeting").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParamDoesNotExist_exists() {
        URI uri = URI.create("local.host?greeting=hello");
        assertThatThrownBy(() -> RequestMatchers.queryParamDoesNotExist("greeting").match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected QueryParam <greeting> to not exist, but it exists with values: [hello]");
    }

    @Test
    void testQueryParamCount() {
        URI uri = URI.create("local.host?greeting=hello&sendoff=farewell");
        assertThatCode(() -> RequestMatchers.queryParamCount(2).match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParamCount_noQuerySegment() {
        URI uri = URI.create("local.host");
        assertThatCode(() -> RequestMatchers.queryParamCount(0).match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParamCount_emptyQuerySegment() {
        URI uri = URI.create("local.host?");
        assertThatCode(() -> RequestMatchers.queryParamCount(0).match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParamCount_repeatedQueryParam() {
        URI uri = URI.create("local.host?greeting=hello&greeting=salutations");
        assertThatCode(() -> RequestMatchers.queryParamCount(1).match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParamCount_countMissmatch() {
        URI uri = URI.create("local.host?greeting=hello&sendoff=farewell");
        assertThatThrownBy(() -> RequestMatchers.queryParamCount(1).match(new MockClientRequestContext(uri)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected %s QueryParams but found %s: %s", 1, 2, "[sendoff, greeting]");
    }

    @Test
    void testHeader() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put(ACCEPT, List.of(APPLICATION_JSON, APPLICATION_XML));

        assertThatCode(() -> RequestMatchers.header(ACCEPT, APPLICATION_JSON, APPLICATION_XML).match(new MockClientRequestContext(headers)))
                .doesNotThrowAnyException();
    }

    @Test
    void testHeader_headerMissing() {
        assertThatThrownBy(() -> RequestMatchers.header(ACCEPT, APPLICATION_JSON).match(new MockClientRequestContext(new MultivaluedHashMap<>())))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected header <%s> to exist but was null", ACCEPT);
    }

    @Test
    void testHeader_countMismatch() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(ACCEPT, APPLICATION_JSON);

        assertThatThrownBy(() -> RequestMatchers.header(ACCEPT, APPLICATION_JSON, APPLICATION_XML).match(new MockClientRequestContext(headers)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected header <%s> to have at least <2> values but found %s", ACCEPT, headers.get(ACCEPT));
    }

    @Test
    void testHeader_valueMismatch() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(ACCEPT, APPLICATION_JSON);

        assertThatThrownBy(() -> RequestMatchers.header(ACCEPT, APPLICATION_XML).match(new MockClientRequestContext(headers)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Request header [name=%s, position=0] expected: <%s> but was: <%s>", ACCEPT, APPLICATION_XML, APPLICATION_JSON);
    }

    @Test
    void testHeader_orderMismatch() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put(ACCEPT, List.of(APPLICATION_JSON, APPLICATION_XML));

        assertThatThrownBy(() -> RequestMatchers.header(ACCEPT, APPLICATION_XML, APPLICATION_JSON).match(new MockClientRequestContext(headers)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Request header [name=%s, position=0] expected: <%s> but was: <%s>", ACCEPT, APPLICATION_XML, APPLICATION_JSON);
    }

    @Test
    void testHeaderDoesNotExist() {
        assertThatCode(() -> RequestMatchers.headerDoesNotExist(ACCEPT).match(new MockClientRequestContext(PATCH))).doesNotThrowAnyException();
    }

    @Test
    void testHeaderDoesNotExist_exists() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(ACCEPT, APPLICATION_JSON);

        assertThatThrownBy(() -> RequestMatchers.headerDoesNotExist(ACCEPT).match(new MockClientRequestContext(headers)))
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected header <%s> to not exist, but it exists with values: %s", ACCEPT, headers.get(ACCEPT));
    }
}
