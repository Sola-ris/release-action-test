package io.github.solaris.jaxrs.client.test.server;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class QuarkusMockRestServerTest extends QuarkusTestFactory {

    @Override
    protected Object getTestInstance() {
        return new MockRestServerTest().new BindMicroProfileRestClientBuilder();
    }
}
