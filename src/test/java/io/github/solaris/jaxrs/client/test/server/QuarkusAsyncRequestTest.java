package io.github.solaris.jaxrs.client.test.server;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class QuarkusAsyncRequestTest extends QuarkusTestFactory {

    @Override
    protected Object getTestInstance() {
        return new AsyncRequestTest().new MicroProfileRestClient();
    }
}
