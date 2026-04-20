package io.github.solaris.jaxrs.client.test.util.extension.classpath;

import static io.github.solaris.jaxrs.client.test.util.extension.classpath.Jackson3Test.CLASS_LOADER_NAME;
import static org.apache.cxf.BusFactory.BUS_FACTORY_PROPERTY_NAME;

import java.lang.reflect.Method;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.CxfFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.Jackson3BusFactory;

@NullMarked
class Jackson3ClasspathExtension extends AbstractClasspathExtension {
    private static final Class<?> CXF_FILTER_EXCEPTION_ASSERT_CLASS;
    private static final ClassPath CLASS_PATH = createClassPath(
            CLASS_LOADER_NAME,
            Set.of("jersey", "resteasy"),
            null,
            lib -> !lib.contains("fasterxml") || lib.contains("annotation")
    );

    static {
        try {
            CXF_FILTER_EXCEPTION_ASSERT_CLASS = CLASS_PATH.classLoader().loadClass(CxfFilterExceptionAssert.class.getName());
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void interceptTestMethod(
            Invocation<@Nullable Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext
    ) throws Throwable {
        try {
            System.setProperty(BUS_FACTORY_PROPERTY_NAME, Jackson3BusFactory.class.getName());
            super.interceptTestMethod(invocation, invocationContext, extensionContext);
        } finally {
            System.clearProperty(BUS_FACTORY_PROPERTY_NAME);
        }
    }

    @Override
    ClassPath getClasspath() {
        return CLASS_PATH;
    }

    @Override
    Class<?> getFilterExceptionAssertClass() {
        return CXF_FILTER_EXCEPTION_ASSERT_CLASS;
    }
}
