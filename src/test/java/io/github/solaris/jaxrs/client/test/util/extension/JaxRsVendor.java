package io.github.solaris.jaxrs.client.test.util.extension;

import java.util.List;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.microprofile.client.spi.CxfRestClientBuilderResolver;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.inject.injectless.NonInjectionManagerFactory;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;
import org.glassfish.jersey.microprofile.restclient.JerseyRestClientBuilderResolver;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;

public enum JaxRsVendor {
    JERSEY(
            org.glassfish.jersey.internal.RuntimeDelegateImpl.class,
            JerseyClientBuilder.class,
            JerseyRestClientBuilderResolver.class,
            NonInjectionManagerFactory.class
    ),
    JERSEY_HK2(
            org.glassfish.jersey.internal.RuntimeDelegateImpl.class,
            JerseyClientBuilder.class,
            JerseyRestClientBuilderResolver.class,
            Hk2InjectionManagerFactory.class
    ),
    RESTEASY(
            ResteasyProviderFactoryImpl.class,
            ResteasyClientBuilderImpl.class,
            org.jboss.resteasy.microprofile.client.BuilderResolver.class
    ),
    CXF(
            org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl.class,
            org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl.class,
            CxfRestClientBuilderResolver.class
    ),
    RESTEASY_REACTIVE(
            org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl.class,
            org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl.class,
            io.quarkus.rest.client.reactive.runtime.BuilderResolver.class
    );

    public static final List<JaxRsVendor> VENDORS = List.of(JaxRsVendor.values());

    private final Class<? extends RuntimeDelegate> runtimeDelegateClass;
    private final Class<? extends ClientBuilder> clientBuilderClass;
    private final Class<? extends RestClientBuilderResolver> restClientBuilderResolverClass;
    private final Class<? extends InjectionManagerFactory> injectionManagerFactoryClass;

    private final ClassLoader vendorClassLoader;

    JaxRsVendor(
            Class<? extends RuntimeDelegate> runtimeDelegateClass,
            Class<? extends ClientBuilder> clientBuilderClass,
            Class<? extends RestClientBuilderResolver> restClientBuilderResolverClass
    ) {
        this(runtimeDelegateClass, clientBuilderClass, restClientBuilderResolverClass, null);
    }

    JaxRsVendor(
            Class<? extends RuntimeDelegate> runtimeDelegateClass,
            Class<? extends ClientBuilder> clientBuilderClass,
            Class<? extends RestClientBuilderResolver> restClientBuilderResolverClass,
            Class<? extends InjectionManagerFactory> injectionManagerFactoryClass
    ) {
        this.runtimeDelegateClass = runtimeDelegateClass;
        this.clientBuilderClass = clientBuilderClass;
        this.restClientBuilderResolverClass = restClientBuilderResolverClass;
        this.injectionManagerFactoryClass = injectionManagerFactoryClass;

        this.vendorClassLoader = new VendorClassLoader(this);
    }

    Class<? extends ClientBuilder> getClientBuilderClass() {
        return clientBuilderClass;
    }

    Class<? extends RuntimeDelegate> getRuntimeDelegateClass() {
        return runtimeDelegateClass;
    }

    Class<? extends RestClientBuilderResolver> getRestClientBuilderResolverClass() {
        return restClientBuilderResolverClass;
    }

    Class<? extends InjectionManagerFactory> getInjectionManagerFactoryClass() {
        return injectionManagerFactoryClass;
    }

    ClassLoader getVendorClassLoader() {
        return vendorClassLoader;
    }
}
