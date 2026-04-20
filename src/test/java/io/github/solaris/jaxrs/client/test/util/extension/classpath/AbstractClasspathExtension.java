package io.github.solaris.jaxrs.client.test.util.extension.classpath;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.ReflectionSupport;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;

@NullMarked
abstract class AbstractClasspathExtension implements InvocationInterceptor, ParameterResolver {
    private static final String CLASSES = Path.of("target", "classes").toAbsolutePath() + File.separator;
    private static final String TEST_CLASSES = Path.of("target", "test-classes").toAbsolutePath() + File.separator;

    static final String CLASS_PATH_PROPERTY = "java.class.path";

    static ClassPath createClassPath(
            String loaderName,
            Set<String> excludedJars,
            @Nullable String extraPathElement,
            @Nullable Predicate<String> extraFilter
    ) {
        String classPathString = Stream.of(CLASSES, TEST_CLASSES, extraPathElement, System.getProperty(CLASS_PATH_PROPERTY))
                .filter(Objects::nonNull)
                .collect(joining(File.pathSeparator));

        List<String> filtered = Arrays.stream(classPathString.split(File.pathSeparator))
                .filter(lib -> excludedJars.stream().noneMatch(lib::contains))
                .filter(lib -> !lib.toLowerCase().contains("intellij"))
                .filter(lib -> extraFilter == null || extraFilter.test(lib))
                .toList();

        URL[] classpath = filtered.stream()
                .map(lib -> "file:" + lib)
                .map(URI::create)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new ExceptionInInitializerError(e);
                    }
                })
                .toArray(URL[]::new);

        return new ClassPath(
                new URLClassLoader(loaderName, classpath, ClassLoader.getPlatformClassLoader()),
                String.join(File.pathSeparator, filtered)
        );
    }

    @Override
    public void interceptTestMethod(
            Invocation<@Nullable Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext
    ) throws Throwable {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        String originalClassPathProperty = System.getProperty(CLASS_PATH_PROPERTY);

        try {
            System.setProperty(CLASS_PATH_PROPERTY, getClasspath().classPathProperty());
            Thread.currentThread().setContextClassLoader(getClasspath().classLoader());

            Class<?> testClass = getClasspath().classLoader().loadClass(invocationContext.getExecutable().getDeclaringClass().getName());
            Method testMethod;
            if (invocationContext.getExecutable().getParameterTypes().length == 1) {
                testMethod = ReflectionSupport.findMethod(
                        testClass,
                        invocationContext.getExecutable().getName(),
                        FilterExceptionAssert.class.getName()
                ).orElseThrow();
            } else {
                testMethod = ReflectionSupport.findMethod(testClass, invocationContext.getExecutable().getName()).orElseThrow();
            }

            Object testInstance = ReflectionSupport.newInstance(testClass);

            ReflectionSupport.invokeMethod(testMethod, testInstance, getArgs(testMethod));
        } finally {
            invocation.skip();
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            System.setProperty(CLASS_PATH_PROPERTY, originalClassPathProperty);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return FilterExceptionAssert.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return null;
    }

    private Object[] getArgs(Method testMethod) {
        if (testMethod.getParameterCount() == 1) {
            return new Object[]{ReflectionSupport.newInstance(getFilterExceptionAssertClass())};
        }
        return new Object[0];
    }

    abstract ClassPath getClasspath();

    abstract Class<?> getFilterExceptionAssertClass();

    record ClassPath(ClassLoader classLoader, String classPathProperty) {}
}
