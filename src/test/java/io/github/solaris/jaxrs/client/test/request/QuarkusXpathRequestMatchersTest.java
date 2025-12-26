package io.github.solaris.jaxrs.client.test.request;

import io.github.solaris.jaxrs.client.test.util.extension.vendor.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class QuarkusXpathRequestMatchersTest extends QuarkusTestFactory {

    @Override
    protected Class<?> getTestClass() {
        return XpathRequestMatchersTest.class;
    }

    @Override
    protected Object getTestInstance() {
        return new XpathRequestMatchersTest();
    }
}
