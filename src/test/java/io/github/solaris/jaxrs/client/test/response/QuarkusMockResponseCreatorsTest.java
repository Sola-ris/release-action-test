package io.github.solaris.jaxrs.client.test.response;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class QuarkusMockResponseCreatorsTest extends QuarkusTestFactory {

    @Override
    protected Class<?> getTestClass() {
        return MockResponseCreatorsTest.WithEntity.class;
    }

    @Override
    protected Object getTestInstance() {
        return new MockResponseCreatorsTest().new WithEntity();
    }
}
