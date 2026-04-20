package io.github.solaris.jaxrs.client.test.util.extension.classpath;

import static io.github.solaris.jaxrs.client.test.util.extension.classpath.JsonPath2Test.CLASS_LOADER_NAME;

import java.nio.file.Path;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;

@NullMarked
class JsonPath2ClasspathExtension extends AbstractClasspathExtension {
    private static final Class<?> DEFAULT_FILTER_EXCEPTION_ASSERT_CLASS;
    private static final ClassPath CLASS_PATH = createClassPath(
            CLASS_LOADER_NAME,
            Set.of("resteasy", "cxf"),
            Path.of("target", "dependency", "json-path-" + System.getProperty("version.json-path-2") + ".jar").toAbsolutePath().toString(),
            lib -> !lib.contains("json-path") || lib.contains(System.getProperty("version.json-path-2"))
    );

    static {
        try {
            DEFAULT_FILTER_EXCEPTION_ASSERT_CLASS = CLASS_PATH.classLoader().loadClass(DefaultFilterExceptionAssert.class.getName());
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    ClassPath getClasspath() {
        return CLASS_PATH;
    }

    @Override
    Class<?> getFilterExceptionAssertClass() {
        return DEFAULT_FILTER_EXCEPTION_ASSERT_CLASS;
    }
}
