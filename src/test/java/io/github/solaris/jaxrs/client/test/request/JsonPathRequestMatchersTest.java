package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.jayway.jsonpath.JsonPathException;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.Dto;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.extension.classpath.JacksonFreeTest;
import io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendorTest;
import io.github.solaris.jaxrs.client.test.util.extension.vendor.RunInQuarkus;

import net.minidev.json.JSONArray;

@RunInQuarkus
class JsonPathRequestMatchersTest {
    private static final String DEFINITE_PATH = "$.something";
    private static final String INDEFINITE_PATH = "$.something[*]";
    private static final String NON_EXISTENT_PATH = "$.somethingElse";

    @AutoClose
    private final Client client = ClientBuilder.newClient();

    private final MockRestServer server = MockRestServer.bindTo(client).build();

    @JaxRsVendorTest
    void testValue() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_indefinitePath() {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(List.of(true));

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_typeConversion() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(42)).andRespond(withSuccess());

        Dto dto = new Dto(42.0);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_null() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(null)).andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_record() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(new Dto("hello"))).andRespond(withSuccess());

        Dto dto = new Dto(new Dto("hello"));

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_noMatch(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(false);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("JSON Path \"%s\" expected: <%s> but was: <%s>", DEFINITE_PATH, true, false);
    }

    @JaxRsVendorTest
    void testValue_noMatch_emptyList(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class);
    }

    @JaxRsVendorTest
    void testValue_noMatch_multipleValues(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        List<Boolean> something = List.of(true, false);
        Dto dto = new Dto(something);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Found list of values %s instead of the expected single value %s", toJsonArray(something), true);
    }

    @JaxRsVendorTest
    void testValue_noMatch_incompatibleTypes(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Map<String, String> something = Map.of("greeting", "hello");
        Dto dto = new Dto(something);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("%s cannot be converted to type %s at JSON path \"%s\"", something, Boolean.class.getTypeName(), DEFINITE_PATH);
    }

    @JacksonFreeTest
    void testValue_record_jacksonUnavailable() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(new Dto("hello"))).andRespond(withSuccess());

        Dto dto = new Dto(new Dto("hello"));

        assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(ProcessingException.class)
                .cause()
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to load Jackson.");
    }

    @JaxRsVendorTest
    void testExists() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_doesNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Found no value for JSON path \"%s\"", NON_EXISTENT_PATH);
    }

    @JaxRsVendorTest
    void testExists_doesNot_nullValue(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(null);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Found no value for JSON path \"%s\"", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testExists_doesNot_indefinitePath(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Found no value for JSON path \"%s\"", INDEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testDoesNotExist() {
        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotExist_indefinitePath() {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotExist_nullValue() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotExist_does_definitePath(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected no value at JSON Path \"%s\" but found true", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testDoesNotExist_does_indefinitePath(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        List<?> something = List.of(true, false);
        Dto dto = new Dto(something);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected no value at JSON Path \"%s\" but found %s", INDEFINITE_PATH, toJsonArray(something));
    }

    @JaxRsVendorTest
    void testHasJsonPath() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testHasJsonPath_nullValue() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testHasJsonPath_indefinitePath() {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(List.of(true, false));

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testHasJsonPath_doesNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Found no value for JSON path \"%s\"", NON_EXISTENT_PATH);
    }

    @JaxRsVendorTest
    void testHasJsonPath_doesNot_indefinitePath(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("No values for JSON Path \"%s\"", INDEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath() {
        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath_indefinitePath() {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath_does(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected no value at JSON Path \"%s\" but found true", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath_does_indefinitePath(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        List<Boolean> something = List.of(true, false);
        Dto dto = new Dto(something);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected no values at JSON Path \"%s\" but found %s", INDEFINITE_PATH, toJsonArray(something));
    }

    @JaxRsVendorTest
    void testIsString() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isString()).andRespond(withSuccess());

        Dto dto = new Dto("hello");

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsString_isNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isString()).andRespond(withSuccess());

        Dto dto = new Dto(0);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected a string at JSON Path \"%s\" but found 0", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testIsBoolean() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isBoolean()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsBoolean_isNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isBoolean()).andRespond(withSuccess());

        Dto dto = new Dto(Map.of());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected a boolean at JSON Path \"%s\" but found {}", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testIsNumber() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isNumber()).andRespond(withSuccess());

        Dto dto = new Dto(13.37);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsNumber_isNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isNumber()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected a number at JSON Path \"%s\" but found []", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testIsArray() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isArray()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsArray_isNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isArray()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected an array at JSON Path \"%s\" but found true", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testIsMap() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isMap()).andRespond(withSuccess());

        Dto dto = new Dto(Map.of());

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsMap_isNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isMap()).andRespond(withSuccess());

        Dto dto = new Dto("hello");

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected a map at JSON Path \"%s\" but found 'hello'", DEFINITE_PATH);
    }

    @JaxRsVendorTest
    void testsValueSatisfies() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(value -> assertThat(value)
                                .isNotNull()
                                .extracting(Dto::something, STRING)
                                .contains("ell"),
                        Dto.class))
                .andRespond(withSuccess());

        Dto dto = new Dto(new Dto("hello"));

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testsValueSatisfies_null() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(value -> assertThat(value).isNull(), Dto.class))
                .andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testsValueSatisfies_doesNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(value -> assertThat(value)
                                .isNotNull()
                                .contains("bye"),
                        String.class))
                .andRespond(withSuccess());

        Dto dto = new Dto("hello");

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll("Expecting actual:", "\"hello\"", "to contain:", "\"bye\"");
    }

    @JaxRsVendorTest
    void testsValueSatisfies_incompatibleType(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(_ -> {}, Dto.class)).andRespond(withSuccess());

        Dto dto = new Dto("hello");

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Failed to evaluate JSON path \"%s\" with type %s", DEFINITE_PATH, Dto.class.toString());
    }

    @JaxRsVendorTest
    void testsValueSatisfies_exceptionInMatcher(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(_ -> throwIoException(), Dto.class))
                .andRespond(withSuccess());

        Dto dto = new Dto(new Dto("hello"));

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessage("I/O Error");
    }

    @JacksonFreeTest
    void testsValueSatisfies_record_jacksonUnavailable() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(_ -> {}, Dto.class)).andRespond(withSuccess());

        Dto dto = new Dto(new Dto("hello"));

        assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(ProcessingException.class)
                .cause()
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to load Jackson.");
    }

    @JaxRsVendorTest
    void testsValueSatisfies_genericType() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(value -> assertThat(value)
                                .isNotNull()
                                .hasSize(2)
                                .containsExactly(
                                        new Dto("hello"),
                                        new Dto("goodbye")
                                ),
                        new GenericType<List<Dto>>() {}))
                .andRespond(withSuccess());

        Dto dto = new Dto(List.of(new Dto("hello"), new Dto("goodbye")));

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testsValueSatisfies_genericType_null() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(value -> assertThat(value).isNull(), new GenericType<List<Dto>>() {}))
                .andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testsValueSatisfies_genericType_doesNot(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(value -> assertThat(value)
                                .isNotNull()
                                .hasSize(2)
                                .containsExactly(
                                        new Dto("greetings"),
                                        new Dto("farewell")
                                ),
                        new GenericType<List<Dto>>() {}))
                .andRespond(withSuccess());

        List<Dto> something = List.of(new Dto("hello"), new Dto("goodbye"));
        Dto dto = new Dto(something);

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll(
                        "Expecting actual:",
                        something.toString(),
                        "to contain exactly (and in same order):",
                        "but some elements were not found:",
                        something.toString(),
                        "and others were not expected:",
                        "[Dto[something=hello], Dto[something=goodbye]]"
                );
    }

    @JaxRsVendorTest
    void testsValueSatisfies_genericType_incompatibleType(FilterExceptionAssert filterExceptionAssert) {
        GenericType<Map<String, Dto>> type = new GenericType<>() {};

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(_ -> {}, type))
                .andRespond(withSuccess());

        Dto dto = new Dto("hello");

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Failed to evaluate JSON path \"%s\" with type %s", DEFINITE_PATH, type);
    }

    @JaxRsVendorTest
    void testsValueSatisfies_genericType_exceptionInMatcher(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(_ -> throwIoException(), new GenericType<List<Dto>>() {}))
                .andRespond(withSuccess());

        Dto dto = new Dto(List.of(new Dto("hello"), new Dto("goodbye")));

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessage("I/O Error");
    }

    @JacksonFreeTest
    void testsValueSatisfies_genericType_record_jacksonUnavailable() {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).valueSatisfies(_ -> {}, new GenericType<List<Dto>>() {}))
                .andRespond(withSuccess());

        Dto dto = new Dto(new Dto("hello"));

        assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                .isInstanceOf(ProcessingException.class)
                .cause()
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to load Jackson.");
    }

    @JaxRsVendorTest
    void testInvalidJson(FilterExceptionAssert filterExceptionAssert) {
        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).exists()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json("<dto></dto>")).close())
                .isInstanceOf(JsonPathException.class)
                .hasMessageContaining("This is not a json object according to the JsonProvider:");
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
                argumentSet("testExpression_null",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(null), "JsonPath expression must not be null or blank."),
                argumentSet("testExpression_blank",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(" \t\n"), "JsonPath expression must not be null or blank."),
                argumentSet("testArgs_null",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(DEFINITE_PATH, (Object[]) null), "'args' must not be null."),
                argumentSet("testValueSatisfies_consumerNull",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(DEFINITE_PATH)
                                .valueSatisfies(null, String.class), "'valueAssertion' must not be null."),
                argumentSet("testValueSatisfies_targetTypeNull",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(DEFINITE_PATH)
                                .valueSatisfies(_ -> {}, (Class<?>) null), "'targetType' must not be null."),
                argumentSet("testValueSatisfies_genericType_consumerNull",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(DEFINITE_PATH)
                                .valueSatisfies(null, new GenericType<>() {}), "'valueAssertion' must not be null."),
                argumentSet("testValueSatisfies_genericType_targetTypeNull",
                        (ThrowingCallable) () -> RequestMatchers.jsonPath(DEFINITE_PATH)
                                .valueSatisfies(_ -> {}, (GenericType<?>) null), "'targetType' must not be null.")
        );
    }

    // JSONArray and AbstractCollection have a different toString()
    private static JSONArray toJsonArray(List<?> list) {
        JSONArray array = new JSONArray();
        array.addAll(list);
        return array;
    }

    private static void throwIoException() throws IOException {
        throw new IOException("I/O Error");
    }
}
