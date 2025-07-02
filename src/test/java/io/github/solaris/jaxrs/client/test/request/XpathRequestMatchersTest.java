package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.JERSEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.jspecify.annotations.NullUnmarked;
import org.xml.sax.SAXParseException;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import io.github.solaris.jaxrs.client.test.util.extension.RunInQuarkus;

@RunInQuarkus
class XpathRequestMatchersTest {
    private static final String EXPLICIT_DEFAULT_NS = """
            <xmlDto xmlns='urn:jax-rs.client.test'>
            </xmlDto>""";

    private static final String CHILD_IN_DIFFERENT_NS = """
            <xmlDto xmlns='urn:jax-rs.client.test' xmlns:other='urn:jax-ws.client.test'>
                <other:greeting>hello</other:greeting>
            </xmlDto>""";

    private static final String ATTRIBUTE_IN_XML_NAMESPACE = """
            <xmlDto>
                <greeting xml:lang='en'>hello</greeting>
                <greeting xml:lang='de'>hallo</greeting>
            </xmlDto>""";

    // Jersey without HK2 throws an NPE when trying to obtain the XML string using the ClientEntityConverter
    @JaxRsVendorTest(skipFor = JERSEY)
    void testExists() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto").exists()).andRespond(withSuccess());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(new XmlDto())).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_explicitDefaultNamespace() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/:xmlDto", Map.of("", "urn:jax-rs.client.test")).exists()).andRespond(withSuccess());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(EXPLICIT_DEFAULT_NS)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_childInDifferentNamespace() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Map<String, String> namespaces = Map.of(
                "", "urn:jax-rs.client.test",
                "other", "urn:jax-ws.client.test"
        );
        server.expect(RequestMatchers.xpath("/:xmlDto/other:greeting", namespaces).exists()).andRespond(withSuccess());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(CHILD_IN_DIFFERENT_NS)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_usingXmlNamespaceAttribute() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Map<String, String> namespaces = Map.of("", "urn:jax-rs.client.test");
        server.expect(RequestMatchers.xpath("/xmlDto/greeting[@xml:lang='en']", namespaces).exists()).andRespond(withSuccess());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(ATTRIBUTE_IN_XML_NAMESPACE)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testExists_doesNot(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlEntity", Map.of()).exists()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(new XmlDto())).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlEntity does not exist");
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testDoesNotExist() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlEntity").doesNotExist()).andRespond(withSuccess());

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(new XmlDto())).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testDoesNotExist_does(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto").doesNotExist()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(new XmlDto())).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto does exist");
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testNodeCount() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/%s", "nodes").nodeCount(1)).andRespond(withSuccess());
        XmlDto xmlDto = new XmlDto();
        xmlDto.nodes = List.of("hello");

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(xmlDto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testNodeCount_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/nodes").nodeCount(2)).andRespond(withSuccess());
        XmlDto xmlDto = new XmlDto();
        xmlDto.nodes = List.of("hello");

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(xmlDto)).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("NodeCount for XPath /xmlDto/nodes expected: <2> but was: <1>");
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testString() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/str").string("hello")).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(xmlDto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testString_coerceNumber() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/num").string("42.0")).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = 42;

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(xmlDto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testString_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/str").string("hello")).andRespond(withSuccess());
        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "goodbye";

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(xmlDto)).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/str expected: <hello> but was: <goodbye>");
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testNumber() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/num").number(1.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = 1;

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(xmlDto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testNumber_coerceString() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/str").number(42.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "42";

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(xmlDto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testNumber_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/num").number(1.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = 2;

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(xmlDto)).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/num expected: <1.0> but was: <2.0>");
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testNumber_notANumber(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/str").number(1.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(xmlDto)).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/str expected: <1.0> but was: <NaN>");
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testBooleanValue() throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/bool").booleanValue(true)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.bool = true;

        assertThatCode(() -> {
            try (client) {
                client.target("/hello").request().post(Entity.xml(xmlDto)).close();
            }
        }).doesNotThrowAnyException();
    }

    @JaxRsVendorTest(skipFor = JERSEY)
    void testBooleanValue_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto/bool").booleanValue(false)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.bool = true;

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml(xmlDto)).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/bool expected: <false> but was: <true>");
    }

    @JaxRsVendorTest
    void testInvalidXml(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(RequestMatchers.xpath("/xmlDto").exists()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> {
                    try (client) {
                        client.target("/hello").request().post(Entity.xml("{\"something\": false}")).close();
                    }
                })
                .isInstanceOf(AssertionError.class)
                .cause()
                .isInstanceOf(SAXParseException.class)
                .satisfies(
                        e -> assertThat(e).hasMessage("Content is not allowed in prolog."),
                        e -> assertThat(((SAXParseException) e).getLineNumber()).isOne(),
                        e -> assertThat(((SAXParseException) e).getColumnNumber()).isOne()
                );
    }

    @JaxRsVendorTest
    void testUnknownNamespace() {
        Map<String, String> namespaces = Map.of(
                "", "urn:jax-rs.client.test",
                "other", "urn:jax-ws.client.test"
        );

        assertThatThrownBy(() -> RequestMatchers.xpath("/xmlDto/greeting:hello", namespaces).exists())
                .isInstanceOf(XPathExpressionException.class)
                .hasMessageEndingWith("Prefix must resolve to a namespace: greeting")
                .cause()
                .isInstanceOf(TransformerException.class)
                .hasMessage("Prefix must resolve to a namespace: greeting");
    }

    @NullUnmarked
    @XmlRootElement
    private static class XmlDto {

        @XmlElement
        private boolean bool;

        @XmlElement
        private String str;

        @XmlElement
        private double num;

        @XmlElementWrapper
        private List<Object> nodes;
    }
}
