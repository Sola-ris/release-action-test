package io.github.solaris.jaxrs.client.test.util.extension.classpath;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.ReflectionSupport;

class JacksonFreeClasspathExtension implements InvocationInterceptor {
    private static final List<String> EXCLUDED_JARS = List.of("resteasy", "cxf", "jackson");
    private static final String CLASS_PATH_PROPERTY = "java.class.path";

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext
    ) throws Throwable {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        String originalClassPathProperty = System.getProperty(CLASS_PATH_PROPERTY);

        String classes = Path.of("target", "classes").toAbsolutePath() + File.separator;
        String testClasses = Path.of("target", "test-classes").toAbsolutePath() + File.separator;

        String classPathString = classes + File.pathSeparator + testClasses + File.pathSeparator + originalClassPathProperty;

        List<String> filtered = Arrays.stream(classPathString.split(File.pathSeparator))
                .filter(lib -> EXCLUDED_JARS.stream().noneMatch(lib::contains))
                .filter(lib -> !lib.toLowerCase().contains("intellij"))
                .toList();
        URL[] classpath = filtered.stream()
                .map(lib -> "file:" + lib)
                .map(URI::create)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(URL[]::new);
        try {
            System.setProperty(CLASS_PATH_PROPERTY, String.join(File.pathSeparator, filtered));
            URLClassLoader jacksonFreeClassLoader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader());
            Thread.currentThread().setContextClassLoader(jacksonFreeClassLoader);

            String testClassName = invocationContext.getExecutable().getDeclaringClass().getName();
            Class<?> testClass = jacksonFreeClassLoader.loadClass(testClassName);
            Optional<Method> testMethod = ReflectionSupport.findMethod(testClass, invocationContext.getExecutable().getName());
            Object testInstance = ReflectionSupport.newInstance(testClass);

            ReflectionSupport.invokeMethod(testMethod.orElseThrow(), testInstance);
        } finally {
            invocation.skip();
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            System.setProperty(CLASS_PATH_PROPERTY, originalClassPathProperty);
        }
    }
}
