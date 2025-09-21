package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.w3c.dom.Node.ELEMENT_NODE;
import static org.w3c.dom.Node.TEXT_NODE;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    @AutoClose
    private final Client client = ClientBuilder.newClient();

    private final MockRestServer server = MockRestServer.bindTo(client).build();

    @JaxRsVendorTest
    void testExists() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto").exists()).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(new XmlDto())).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_explicitDefaultNamespace() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/:xmlDto", Map.of("", "urn:jax-rs.client.test")).exists()).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(EXPLICIT_DEFAULT_NS)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_childInDifferentNamespace() throws XPathExpressionException {
        Map<String, String> namespaces = Map.of(
                "", "urn:jax-rs.client.test",
                "other", "urn:jax-ws.client.test"
        );
        server.expect(RequestMatchers.xpath("/:xmlDto/other:greeting", namespaces).exists()).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(CHILD_IN_DIFFERENT_NS)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_usingXmlNamespaceAttribute() throws XPathExpressionException {
        Map<String, String> namespaces = Map.of("", "urn:jax-rs.client.test");
        server.expect(RequestMatchers.xpath("/xmlDto/greeting[@xml:lang='en']", namespaces).exists()).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(ATTRIBUTE_IN_XML_NAMESPACE)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testExists_doesNot(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlEntity", Map.of()).exists()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(new XmlDto())).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlEntity does not exist");
    }

    @JaxRsVendorTest
    void testDoesNotExist() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlEntity").doesNotExist()).andRespond(withSuccess());

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(new XmlDto())).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testDoesNotExist_does(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto").doesNotExist()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(new XmlDto())).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto does exist");
    }

    @JaxRsVendorTest
    void testNodeCount() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/%s", "nodes").nodeCount(1)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.nodes = List.of("hello");

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testNodeCount_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/nodes").nodeCount(2)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.nodes = List.of("hello");

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("NodeCount for XPath /xmlDto/nodes expected: <2> but was: <1>");
    }

    @JaxRsVendorTest
    void testString() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str").string("hello")).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testString_coerceNumber() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/num").string("42.0")).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = 42.0;

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testString_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str").string("hello")).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "goodbye";

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/str expected: <hello> but was: <goodbye>");
    }

    @JaxRsVendorTest
    void testNumber() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/num").number(1.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = 1;

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testNumber_coerceString() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str").number(42.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "42";

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testNumber_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/num").number(1.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = 2;

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/num expected: <1.0> but was: <2.0>");
    }

    @JaxRsVendorTest
    void testNumber_notANumber(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str").number(1.0)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/str expected: <1.0> but was: <NaN>");
    }

    @JaxRsVendorTest
    void testBooleanValue() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/bool").booleanValue(true)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.bool = true;

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testBooleanValue_noMatch(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/bool").booleanValue(false)).andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.bool = true;

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("XPath /xmlDto/bool expected: <false> but was: <true>");
    }

    @JaxRsVendorTest
    void testValueSatisfies() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str").valueSatisfies(node -> assertThat(node)
                                .isNotNull()
                                .satisfies(
                                        n -> assertThat(n.getNodeType()).isEqualTo(ELEMENT_NODE),
                                        n -> assertThat(n.hasChildNodes()).isTrue(),
                                        n -> assertThat(n.getChildNodes().getLength()).isEqualTo(1),
                                        n -> assertThat(n.getFirstChild()).satisfies(
                                                cn -> assertThat(cn.getNodeType()).isEqualTo(TEXT_NODE),
                                                cn -> assertThat(cn.getTextContent()).isEqualTo("hello")
                                        )
                                ),
                        Node.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValueSatisfies_doesNot(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str")
                        .valueSatisfies(s -> assertThat(s).isNotNull().contains("bye"), String.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll("Expecting actual:", "\"hello\"", "to contain:", "\"bye\"");
    }

    @JaxRsVendorTest
    @SuppressWarnings("DataFlowIssue")
    void testValueSatisfies_null() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str").valueSatisfies(node -> assertThat(node).isNull(), Node.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValueSatisfies_nodeList() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/nodes/node").valueSatisfies(nodeList -> assertThat(nodeList).isNotNull()
                                .satisfies(
                                        nl -> assertThat(nl.getLength()).isEqualTo(2),
                                        nl -> assertThat(nl.item(0).getTextContent()).isEqualTo("hello"),
                                        nl -> assertThat(nl.item(1).getTextContent()).isEqualTo("goodbye")
                                ),
                        NodeList.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.nodes = List.of("hello", "goodbye");

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValueSatisfies_int() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/num").valueSatisfies(integer -> assertThat(integer)
                                .isNotNull()
                                .isEqualTo(Integer.MIN_VALUE),
                        Integer.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = Integer.MIN_VALUE;

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValueSatisfies_long() throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/num").valueSatisfies(integer -> assertThat(integer)
                                .isNotNull()
                                .isEqualTo(Long.MAX_VALUE),
                        Long.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.num = Long.MAX_VALUE;

        assertThatCode(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close()).doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testValueSatisfies_unexpectedTargetType(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto/str")
                        .valueSatisfies(xmlDto -> {}, XmlDto.class))
                .andRespond(withSuccess());

        XmlDto xmlDto = new XmlDto();
        xmlDto.str = "hello";

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml(xmlDto)).close())
                .isInstanceOf(AssertionError.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("UnSupported Return Type : %s", XmlDto.class);
    }

    @JaxRsVendorTest
    void testInvalidXml(FilterExceptionAssert filterExceptionAssert) throws XPathExpressionException {
        server.expect(RequestMatchers.xpath("/xmlDto").exists()).andRespond(withSuccess());

        filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello").request().post(Entity.xml("{\"something\": false}")).close())
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

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void testArgumentValidation(ThrowingCallable callable, String exceptionMessage) {
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(exceptionMessage);
    }

    @SuppressWarnings({"DataFlowIssue", "ResultOfMethodCallIgnored"})
    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
                argumentSet("testExpression_null",
                        (ThrowingCallable) () -> RequestMatchers.xpath(null), "XPath expression must not be null or blank."),
                argumentSet("testExpression_blank",
                        (ThrowingCallable) () -> RequestMatchers.xpath(" \t\n"), "XPath expression must not be null or blank."),
                argumentSet("testString_null",
                        (ThrowingCallable) () -> RequestMatchers.xpath("/xmlDto/str").string(null), "'expectedString' must not be null."),
                argumentSet("testNumber_null",
                        (ThrowingCallable) () -> RequestMatchers.xpath("/xmlDto/str").number(null), "'expectedNumber' must not be null."),
                argumentSet("testValueSatisfies_valueAssertionNull",
                        (ThrowingCallable) () -> RequestMatchers.xpath("/xmlDto/str").valueSatisfies(null, null),
                        "'valueAssertion' must not be null."),
                argumentSet("testValueSatisfies_targetTypeNull",
                        (ThrowingCallable) () -> RequestMatchers.xpath("/xmlDto/str").valueSatisfies(__ -> {}, null),
                        "'targetType' must not be null.")
        );
    }

    @NullUnmarked
    @XmlRootElement
    private static class XmlDto {

        @XmlElement
        private boolean bool;

        @XmlElement
        private String str;

        @XmlElement
        private Number num;

        @XmlElementWrapper
        @XmlElement(name = "node")
        private List<Object> nodes;
    }
}
