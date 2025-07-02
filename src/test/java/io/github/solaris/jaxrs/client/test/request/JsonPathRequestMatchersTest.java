package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;

import com.jayway.jsonpath.JsonPathException;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.ConfiguredClientSupplier;
import io.github.solaris.jaxrs.client.test.util.Dto;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import io.github.solaris.jaxrs.client.test.util.extension.RunInQuarkus;

import net.minidev.json.JSONArray;

@RunInQuarkus
class JsonPathRequestMatchersTest {
    private static final String DEFINITE_PATH = "$.something";
    private static final String INDEFINITE_PATH = "$.something[*]";
    private static final String NON_EXISTENT_PATH = "$.somethingElse";

    @JaxRsVendorTest
    void testValue(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_indefinitePath(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(List.of(true));

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_typeConversion(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(42)).andRespond(withSuccess());

        Dto dto = new Dto(42.0);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValue_noMatch(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(false);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("JSON Path \"%s\" expected: <%s> but was: <%s>", DEFINITE_PATH, true, false);
        }
    }

    @JaxRsVendorTest
    void testValue_noMatch_emptyList(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Found no value matching %s at JSON path \"%s\"", true, DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testValue_noMatch_multipleValues(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        List<Boolean> something = List.of(true, false);
        Dto dto = new Dto(something);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Found list of values %s instead of the expected single value %s", toJsonArray(something), true);
        }
    }

    @JaxRsVendorTest
    void testValue_noMatch_incompatibleTypes(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).value(true)).andRespond(withSuccess());

        Map<String, String> something = Map.of("greeting", "hello");
        Dto dto = new Dto(something);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("%s cannot be converted to type %s at JSON path \"%s\"", something, Boolean.class.getTypeName(), DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testExists(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_doesNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Found no value for JSON path \"%s\"", NON_EXISTENT_PATH);
        }
    }

    @JaxRsVendorTest
    void testExists_doesNot_nullValue(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(null);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Found no value for JSON path \"%s\"", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testExists_doesNot_indefinitePath(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).exists()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Found no value for JSON path \"%s\"", INDEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testDoesNotExist(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotExist_indefinitePath(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }


    @JaxRsVendorTest
    void testDoesNotExist_nullValue(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotExist_does_definitePath(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected no value at JSON Path \"%s\" but found true", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testDoesNotExist_does_indefinitePath(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotExist()).andRespond(withSuccess());

        List<?> something = List.of(true, false);
        Dto dto = new Dto(something);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected no value at JSON Path \"%s\" but found %s", INDEFINITE_PATH, toJsonArray(something));
        }
    }

    @JaxRsVendorTest
    void testHasJsonPath(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testHasJsonPath_nullValue(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(null);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testHasJsonPath_indefinitePath(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(List.of(true, false));

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testHasJsonPath_doesNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Found no value for JSON path \"%s\"", NON_EXISTENT_PATH);
        }
    }

    @JaxRsVendorTest
    void testHasJsonPath_doesNot_indefinitePath(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).hasJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("No values for JSON Path \"%s\"", INDEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(NON_EXISTENT_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath_indefinitePath(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath_does(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected no value at JSON Path \"%s\" but found true", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testDoesNotHaveJsonPath_does_indefinitePath(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(INDEFINITE_PATH).doesNotHaveJsonPath()).andRespond(withSuccess());

        List<Boolean> something = List.of(true, false);
        Dto dto = new Dto(something);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected no values at JSON Path \"%s\" but found %s", INDEFINITE_PATH, toJsonArray(something));
        }
    }

    @JaxRsVendorTest
    void testIsString(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isString()).andRespond(withSuccess());

        Dto dto = new Dto("hello");

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsString_isNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isString()).andRespond(withSuccess());

        Dto dto = new Dto(0);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected a string at JSON Path \"%s\" but found 0", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testIsBoolean(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isBoolean()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsBoolean_isNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isBoolean()).andRespond(withSuccess());

        Dto dto = new Dto(Map.of());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected a boolean at JSON Path \"%s\" but found {}", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testIsNumber(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isNumber()).andRespond(withSuccess());

        Dto dto = new Dto(13.37);

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsNumber_isNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isNumber()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected a number at JSON Path \"%s\" but found []", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testIsArray(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isArray()).andRespond(withSuccess());

        Dto dto = new Dto(List.of());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsArray_isNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isArray()).andRespond(withSuccess());

        Dto dto = new Dto(true);

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected an array at JSON Path \"%s\" but found true", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testIsMap(ConfiguredClientSupplier clientSupplier) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isMap()).andRespond(withSuccess());

        Dto dto = new Dto(Map.of());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.json(dto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testIsMap_isNot(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).isMap()).andRespond(withSuccess());

        Dto dto = new Dto("hello");

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json(dto)).close())
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("Expected a map at JSON Path \"%s\" but found 'hello'", DEFINITE_PATH);
        }
    }

    @JaxRsVendorTest
    void testInvalidJson(ConfiguredClientSupplier clientSupplier, FilterExceptionAssert filterExceptionAssert) {
        Client client = clientSupplier.get();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.jsonPath(DEFINITE_PATH).exists()).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.json("<dto></dto>")).close())
                    .isInstanceOf(JsonPathException.class)
                    .hasMessageContaining("This is not a json object according to the JsonProvider:");
        }
    }

    // JSONArray and AbstractCollection have a different toString()
    private static JSONArray toJsonArray(List<?> list) {
        JSONArray array = new JSONArray();
        array.addAll(list);
        return array;
    }
}
