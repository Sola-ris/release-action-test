package io.github.solaris.jaxrs.client.test.request;

import static jakarta.ws.rs.HttpMethod.HEAD;
import static jakarta.ws.rs.HttpMethod.PATCH;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
    void testQueryPram_testExists() {
        URI uri = URI.create("local.host?greeting=hello");
        assertThatCode(() -> RequestMatchers.queryParam("greeting").match(new MockClientRequestContext(uri)))
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
    void testQueryParam_equalsSignInValue_encoded() {
        String encodedEqualsSign = URLEncoder.encode("=", UTF_8);
        URI uri = URI.create("local.host?greeting=hello" + encodedEqualsSign + "salutations&sendoff=farewell");
        assertThatCode(() -> RequestMatchers.queryParam("greeting", "hello=salutations").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_equalsSignInName_encoded() {
        String encodedEqualsSign = URLEncoder.encode("=", UTF_8);
        URI uri = URI.create("local.host?gree" + encodedEqualsSign + "ting=hello&sendoff=farewell");
        assertThatCode(() -> RequestMatchers.queryParam("gree=ting", "hello").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_ampersandInName_encoded() {
        String ampersandEncoded = URLEncoder.encode("&", UTF_8);
        URI uri = URI.create("local.host?gree" + ampersandEncoded + "ting=hello");
        assertThatCode(() -> RequestMatchers.queryParam("gree&ting", "hello").match(new MockClientRequestContext(uri)))
                .doesNotThrowAnyException();
    }

    @Test
    void testQueryParam_ampersandInValue_encoded() {
        String ampersandEncoded = URLEncoder.encode("&", UTF_8);
        URI uri = URI.create("local.host?greeting=hel" + ampersandEncoded + "lo");
        assertThatCode(() -> RequestMatchers.queryParam("greeting", "hel&lo").match(new MockClientRequestContext(uri)))
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
    void testQueryParamCount_blankQuery_encoded() {
        URI uri = URI.create("local.host&" + URLEncoder.encode(" \t", UTF_8));
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
    void testQueryParamCount_countMismatch() {
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
    void testHeader_testExists() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put(ACCEPT, List.of(APPLICATION_JSON, APPLICATION_XML));

        assertThatCode(() -> RequestMatchers.header(ACCEPT).match(new MockClientRequestContext(headers)))
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
                argumentSet("testHttpMethod_null",
                        (ThrowingCallable) () -> RequestMatchers.method(null), "'httpMethod' must not be null."),
                argumentSet("testRequestTo_uriNull",
                        (ThrowingCallable) () -> RequestMatchers.requestTo((String) null), "'uri' must not be null."),
                argumentSet("testRequestTo_stringNull",
                        (ThrowingCallable) () -> RequestMatchers.requestTo((URI) null), "'uri' must not be null."),
                argumentSet("testQueryParam_nameNull",
                        (ThrowingCallable) () -> RequestMatchers.queryParam(null), "'name' must not be null."),
                argumentSet("testQueryParam_valuesNull",
                        (ThrowingCallable) () -> RequestMatchers.queryParam("greeting", (String[]) null), "'expectedValues' must not be null."),
                argumentSet("testQueryParamDoesNotExist_null",
                        (ThrowingCallable) () -> RequestMatchers.queryParamDoesNotExist(null), "'name' must not be null."),
                argumentSet("testHeader_nameNull",
                        (ThrowingCallable) () -> RequestMatchers.header(null), "'name' must not be null."),
                argumentSet("testHeader_valuesNull",
                        (ThrowingCallable) () -> RequestMatchers.header(ACCEPT, (String[]) null), "'expectedValues' must not be null."),
                argumentSet("testHeaderDoesNotExist_null",
                        (ThrowingCallable) () -> RequestMatchers.headerDoesNotExist(null), "'name' must not be null.")
        );
    }
}
