package io.github.solaris.jaxrs.client.test.util.extension;

import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.RESTEASY_REACTIVE;
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

import io.github.solaris.jaxrs.client.test.util.ConfiguredClientSupplier;
import io.github.solaris.jaxrs.client.test.util.ConfiguredClientSupplier.DefaultClientSupplier;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;

public abstract class QuarkusTestFactory {

    protected abstract Object getTestInstance();

    @TestFactory
    Stream<DynamicNode> generate() {
        return ReflectionSupport.streamMethods(getTestInstance().getClass(), method -> method.isAnnotationPresent(JaxRsVendorTest.class), TOP_DOWN)
                .map(method -> DynamicTest.dynamicTest(generateMethodName(method), () -> {
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        RuntimeDelegate.setInstance(null);
                        RestClientBuilderResolver.setInstance(null);
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

        return method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(joining(", ")) + ")";
    }

    private static Object[] getArgs(Method method) {
        List<Object> args = new ArrayList<>();
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (parameterType.equals(FilterExceptionAssert.class)) {
                args.add(new DefaultFilterExceptionAssert());
            } else if (parameterType.equals(ConfiguredClientSupplier.class)) {
                args.add(new DefaultClientSupplier());
            } else {
                throw new IllegalArgumentException("Unexpected parameter type " + parameterType);
            }
        }

        return args.toArray();
    }
}
