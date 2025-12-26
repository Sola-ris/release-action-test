package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotBlank;
import static io.github.solaris.jaxrs.client.test.internal.ArgumentValidator.validateNotNull;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertTrue;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathNodes;

import jakarta.ws.rs.client.ClientRequestContext;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Factory for {@link RequestMatcher} implementations that use an {@link XPath} expression.
 * <p>Accessed via {@link RequestMatchers#xpath(String, Object...)} or {@link RequestMatchers#xpath(String, Map, Object...)}.</p>
 * <p>
 * Requires an Entity Provider for {@code application/xml} to be present and registered with the JAX-RS client component that executed the request.
 * </p>
 */
public final class XpathRequestMatchers {
    private final String expression;
    private final XPathExpression xPathExpression;
    private final boolean namespaceAware;

    XpathRequestMatchers(String expression, Map<String, String> namespaces, Object... args) throws XPathExpressionException {
        validateNotBlank(expression, "XPath expression must not be null or blank.");
        validateNotNull(namespaces, "'namespaces' must not be null.");
        validateNotNull(args, "'args' must not be null.");
        this.expression = expression.formatted(args);
        this.xPathExpression = compile(this.expression, namespaces);
        this.namespaceAware = !namespaces.isEmpty();
    }

    private static XPathExpression compile(String expression, Map<String, String> namespaces) throws XPathExpressionException {
        SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
        namespaceContext.setBindings(namespaces);
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(namespaceContext);
        return xPath.compile(expression);
    }

    @SuppressWarnings("unchecked")
    private <T extends @Nullable Object> T evaluate(ClientRequestContext requestContext, Class<T> targetType) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        DocumentBuilder builder = factory.newDocumentBuilder();

        EntityConverter converter = EntityConverter.fromRequestContext(requestContext);
        String xmlString = converter.convertEntity(requestContext, String.class);

        InputSource inputSource = new InputSource(new StringReader(xmlString));
        inputSource.setEncoding(UTF_8.name());
        Document document = builder.parse(inputSource);

        // XPathExpression::evaluateExpression only supports javax.xml.xpath.XPathNodes for NODESET
        if (NodeList.class.equals(targetType)) {
            return (T) xPathExpression.evaluate(document, XPathConstants.NODESET);
        }

        return xPathExpression.evaluateExpression(document, targetType);
    }

    /**
     * Assert that a value exists at the given XPath.
     */
    public RequestMatcher exists() {
        return (XpathRequestMatcher) request -> {
            Node node = evaluate(request, Node.class);
            assertTrue("XPath " + expression + " does not exist", node != null);
        };
    }

    /**
     * Assert that no value exists at the given XPath.
     */
    public RequestMatcher doesNotExist() {
        return (XpathRequestMatcher) request -> {
            Node node = evaluate(request, Node.class);
            assertTrue("XPath " + expression + " does exist", node == null);
        };
    }

    /**
     * Evaluate the XPath expression and assert it evaluated to the given amount of nodes.
     *
     * @param expectedCount The expected amount of nodes
     */
    public RequestMatcher nodeCount(int expectedCount) {
        return (XpathRequestMatcher) request -> {
            XPathNodes nodes = evaluate(request, XPathNodes.class);
            int actualCount = nodes == null ? 0 : nodes.size();
            assertEqual("NodeCount for XPath " + expression, expectedCount, actualCount);
        };
    }

    /**
     * Evaluate the XPath expression and compare the value to the given String
     *
     * @param expectedString The expected String value
     */
    public RequestMatcher string(String expectedString) {
        validateNotNull(expectedString, "'expectedString' must not be null.");
        return (XpathRequestMatcher) request -> {
            String actualString = evaluate(request, String.class);
            assertEqual("XPath " + expression, expectedString, actualString);
        };
    }

    /**
     * Evaluate the XPath expression and compare the value to the given Number
     *
     * @param expectedNumber The expected numeric value
     */
    public RequestMatcher number(Double expectedNumber) {
        validateNotNull(expectedNumber, "'expectedNumber' must not be null.");
        return (XpathRequestMatcher) request -> {
            Double actualNumber = evaluate(request, Double.class);
            assertEqual("XPath " + expression, expectedNumber, actualNumber);
        };
    }

    /**
     * Evaluate the XPath expression and compare the value to the given boolean
     *
     * @param expectedBoolean The expected boolean value
     */
    public RequestMatcher booleanValue(boolean expectedBoolean) {
        return (XpathRequestMatcher) request -> {
            Boolean actualBoolean = evaluate(request, Boolean.class);
            assertEqual("XPath " + expression, expectedBoolean, actualBoolean);
        };
    }

    /**
     * Evaluate the XPath expression, convert it into {@code targetType} and assert the resulting value with the supplied assertion
     *
     * @param valueAssertion An arbitrary assertion with which to assert the resulting value
     * @param targetType     The expected type of the resulting value
     * @param <T>            The expected type of the resulting value. Possibly null. Supported values:
     *                       <ul>
     *                          <li>{@link Node} ({@link XPathConstants#NODE})</li>
     *                          <li>{@link NodeList} and {@link XPathNodes} ({@link XPathConstants#NODESET})</li>
     *                          <li>{@link String} ({@link XPathConstants#STRING})</li>
     *                          <li>{@link Double}, {@link Integer} and {@link Long} ({@link XPathConstants#NUMBER})</li>
     *                          <li>{@link Boolean} ({@link XPathConstants#BOOLEAN})</li>
     *                       </ul>
     */
    public <T extends @Nullable Object> RequestMatcher valueSatisfies(ThrowingConsumer<T> valueAssertion, Class<T> targetType) {
        validateNotNull(valueAssertion, "'valueAssertion' must not be null.");
        validateNotNull(targetType, "'targetType' must not be null.");
        return (XpathRequestMatcher) request -> valueAssertion.accept(evaluate(request, targetType));
    }

    @FunctionalInterface
    private interface XpathRequestMatcher extends RequestMatcher {

        @Override
        default void match(ClientRequestContext request) {
            try {
                matchThrowing(request);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        void matchThrowing(ClientRequestContext requestContext) throws Exception;
    }

    /**
     * <p>Copied from org.springframework.util.xml.SimpleNamespaceContext</p>
     * <p>
     * Changes:
     *     <ul>
     *         <li>
     *             Replaced org.springframework.util.Assert.notNull with
     *             {@link io.github.solaris.jaxrs.client.test.internal.ArgumentValidator#validateNotNull validateNotNull}
     *         </li>
     *         <li>Removed @Nullable from getPrefix</li>
     *         <li>
     *             Removed methods:
     *             <ul>
     *                 <li>bindDefaultNamespaceUri</li>
     *                 <li>removeBinding</li>
     *                 <li>clear</li>
     *                 <li>getBoundPrefixes</li>
     *             </ul>
     *         </li>
     *         <li>
     *             Unimplemented methods:
     *             <ul>
     *                 <li>getPrefix</li>
     *                 <li>getPrefixes</li>
     *             </ul>
     *         </li>
     *         <li>
     *             Made private:
     *             <ul>
     *                 <li>setBindings</li>
     *                 <li>bindNamespaceUri</li>
     *             </ul>
     *         </li>
     *     </ul>
     * </p>
     * <p>
     * Simple {@code javax.xml.namespace.NamespaceContext} implementation.
     * Follows the standard {@code NamespaceContext} contract, and is loadable
     * via a {@code java.util.Map} or {@code java.util.Properties} object
     *
     * @author Arjen Poutsma
     * @author Juergen Hoeller
     * @since Spring Framework 3.0
     */
    private static class SimpleNamespaceContext implements NamespaceContext {
        private final Map<String, String> prefixToNamespaceUri = new HashMap<>();
        private final Map<String, Set<String>> namespaceUriToPrefixes = new HashMap<>();
        private String defaultNamespaceUri = "";

        @Override
        public String getNamespaceURI(String prefix) {
            validateNotNull(prefix, "'prefix' must not be null.");
            if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
                return XMLConstants.XML_NS_URI;
            } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                return this.defaultNamespaceUri;
            } else if (this.prefixToNamespaceUri.containsKey(prefix)) {
                return this.prefixToNamespaceUri.get(prefix);
            }
            return "";
        }

        @Override
        public String getPrefix(String namespaceUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceUri) {
            throw new UnsupportedOperationException();
        }

        /**
         * Set the bindings for this namespace context.
         * The supplied map must consist of string key value pairs.
         */
        private void setBindings(Map<String, String> bindings) {
            bindings.forEach(this::bindNamespaceUri);
        }

        /**
         * Bind the given prefix to the given namespace.
         *
         * @param prefix       the namespace prefix
         * @param namespaceUri the namespace URI
         */
        private void bindNamespaceUri(String prefix, String namespaceUri) {
            validateNotNull(prefix, "'prefix' must not be null.");
            validateNotNull(namespaceUri, "'namespaceUri' must not be null.");
            if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                this.defaultNamespaceUri = namespaceUri;
            } else {
                this.prefixToNamespaceUri.put(prefix, namespaceUri);
                Set<String> prefixes = this.namespaceUriToPrefixes.computeIfAbsent(namespaceUri, k -> new LinkedHashSet<>());
                prefixes.add(prefix);
            }
        }
    }
}
