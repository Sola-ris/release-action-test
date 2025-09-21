package io.github.solaris.jaxrs.client.test.util.extension;

import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.CXF;
import static io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendor.JERSEY;

import jakarta.ws.rs.ext.RuntimeDelegate;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert.ClientEntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert.ProvidersEntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.CxfFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.CxfMicroProfileFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;

class JaxRsVendorTestExtension implements ParameterResolver, TestInstancePreConstructCallback, TestInstancePreDestroyCallback {
    private static final Namespace NAMESPACE = Namespace.create(JaxRsVendorTestExtension.class);

    private final JaxRsVendor vendor;

    JaxRsVendorTestExtension(JaxRsVendor vendor) {
        this.vendor = vendor;
    }

    @Override
    public void preConstructTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext context) {
        context.getStore(NAMESPACE).put(ClassLoader.class, Thread.currentThread().getContextClassLoader());

        RuntimeDelegate.setInstance(null);
        RestClientBuilderResolver.setInstance(null);

        Thread.currentThread().setContextClassLoader(vendor.getVendorClassLoader());
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
        Thread.currentThread().setContextClassLoader(context.getStore(NAMESPACE).get(ClassLoader.class, ClassLoader.class));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return FilterExceptionAssert.class.isAssignableFrom(parameterContext.getParameter().getType())
                || EntityConverterAssert.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (FilterExceptionAssert.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            if (vendor == CXF) {
                if (extensionContext.getRequiredTestClass().getName().contains("MicroProfile")) {
                    return new CxfMicroProfileFilterExceptionAssert();
                }
                return new CxfFilterExceptionAssert();
            }
            return new DefaultFilterExceptionAssert();
        } else if (EntityConverterAssert.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            if (vendor == JERSEY) {
                return new ClientEntityConverterAssert();
            }
            return new ProvidersEntityConverterAssert();
        } else {
            throw new ParameterResolutionException("Unexpected Parameter of type " + parameterContext.getParameter().getType());
        }
    }
}
