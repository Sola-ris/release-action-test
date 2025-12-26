package io.github.solaris.jaxrs.client.test.request;

import io.github.solaris.jaxrs.client.test.util.extension.vendor.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class QuarkusJsonPathRequestMatchersTest extends QuarkusTestFactory {

    @Override
    protected Class<?> getTestClass() {
        return JsonPathRequestMatchersTest.class;
    }

    @Override
    protected Object getTestInstance() {
        return new JsonPathRequestMatchersTest();
    }
}
