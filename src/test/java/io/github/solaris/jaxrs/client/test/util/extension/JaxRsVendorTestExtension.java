package io.github.solaris.jaxrs.client.test.util.extension;

import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.CXF;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.JERSEY;

import java.lang.reflect.Method;

import jakarta.ws.rs.ext.RuntimeDelegate;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import io.github.solaris.jaxrs.client.test.util.ConfiguredClientSupplier;
import io.github.solaris.jaxrs.client.test.util.ConfiguredClientSupplier.CxfClientSupplier;
import io.github.solaris.jaxrs.client.test.util.ConfiguredClientSupplier.DefaultClientSupplier;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert.ClientEntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert.ProvidersEntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.CxfFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;

class JaxRsVendorTestExtension implements InvocationInterceptor, ParameterResolver {
    private final JaxRsVendor vendor;

    JaxRsVendorTestExtension(JaxRsVendor vendor) {
        this.vendor = vendor;
    }

    @Override
    public void interceptTestTemplateMethod(
            Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            RuntimeDelegate.setInstance(null);
            RestClientBuilderResolver.setInstance(null);
            Thread.currentThread().setContextClassLoader(vendor.getVendorClassLoader());

            invocation.proceed();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return FilterExceptionAssert.class.isAssignableFrom(parameterContext.getParameter().getType())
                || EntityConverterAssert.class.isAssignableFrom(parameterContext.getParameter().getType())
                || ConfiguredClientSupplier.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (FilterExceptionAssert.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            if (vendor == CXF) {
                return new CxfFilterExceptionAssert();
            }
            return new DefaultFilterExceptionAssert();
        } else if (EntityConverterAssert.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            if (vendor == JERSEY) {
                return new ClientEntityConverterAssert();
            }
            return new ProvidersEntityConverterAssert();
        } else if (ConfiguredClientSupplier.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            if (vendor == CXF) {
                return new CxfClientSupplier();
            }
            return new DefaultClientSupplier();
        } else {
            throw new ParameterResolutionException("Unexpected Parameter of type " + parameterContext.getParameter().getType());
        }
    }
}
