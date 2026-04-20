package io.github.solaris.jaxrs.client.test.util.extension.classpath;

import static io.github.solaris.jaxrs.client.test.util.extension.classpath.JacksonFreeTest.CLASS_LOADER_NAME;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
class JacksonFreeClasspathExtension extends AbstractClasspathExtension {
    private static final ClassPath CLASS_PATH = createClassPath(
            CLASS_LOADER_NAME,
            Set.of("resteasy", "cxf", "jackson"),
            null,
            null
    );

    @Override
    ClassPath getClasspath() {
        return CLASS_PATH;
    }

    @Override
    Class<?> getFilterExceptionAssertClass() {
        throw new UnsupportedOperationException("Not supported in @JacksonFreeTest.");
    }
}
