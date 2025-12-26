package io.github.solaris.jaxrs.client.test.util.extension.vendor;

import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.RESTEASY_REACTIVE;
import static io.github.solaris.jaxrs.client.test.util.extension.vendor.JaxRsVendor.VENDORS;
import static java.util.stream.Collectors.joining;
import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.ext.RuntimeDelegate;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.support.ReflectionSupport;
import org.opentest4j.TestAbortedException;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.extension.classpath.JacksonFreeTest;

public abstract class QuarkusTestFactory {
    private static final boolean ENABLED = VENDORS.contains(RESTEASY_REACTIVE);

    protected abstract Class<?> getTestClass();

    protected abstract Object getTestInstance();

    @TestFactory
    Stream<DynamicNode> generate() {
        if (!ENABLED) {
            throw new TestAbortedException("Resteasy Reactive is disabled");
        }

        RuntimeDelegate.setInstance(null);
        RestClientBuilderResolver.setInstance(null);

        return ReflectionSupport.streamMethods(getTestClass(), method -> method.isAnnotationPresent(JaxRsVendorTest.class), TOP_DOWN)
                .filter(method -> !method.isAnnotationPresent(JacksonFreeTest.class))
                .map(method -> DynamicTest.dynamicTest(generateMethodName(method), () -> {
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(RESTEASY_REACTIVE.getVendorClassLoader());
                        ReflectionSupport.invokeMethod(method, getTestInstance(), getArgs(method));
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                }));
    }

    private static String generateMethodName(Method method) {
        if (method.getParameterTypes().length == 0) {
            return method.getName() + "()";
        }

        return method.getName() + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(joining(", ", "(", ")"));
    }

    private static Object[] getArgs(Method method) {
        List<Object> args = new ArrayList<>();
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (parameterType.equals(FilterExceptionAssert.class)) {
                args.add(new DefaultFilterExceptionAssert());
            } else {
                throw new IllegalArgumentException("Unexpected parameter type " + parameterType);
            }
        }

        return args.toArray();
    }
}
