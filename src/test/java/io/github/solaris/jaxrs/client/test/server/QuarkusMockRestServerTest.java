package io.github.solaris.jaxrs.client.test.server;

import io.github.solaris.jaxrs.client.test.util.extension.vendor.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class QuarkusMockRestServerTest extends QuarkusTestFactory {

    @Override
    protected Class<?> getTestClass() {
        return MockRestServerTest.BindMicroProfileRestClientBuilder.class;
    }

    @Override
    protected Object getTestInstance() {
        return new MockRestServerTest().new BindMicroProfileRestClientBuilder();
    }
}
