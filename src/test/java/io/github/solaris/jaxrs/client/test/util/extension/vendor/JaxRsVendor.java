package io.github.solaris.jaxrs.client.test.util.extension.vendor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.BusFactory;
import org.apache.cxf.microprofile.client.spi.CxfRestClientBuilderResolver;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.inject.injectless.NonInjectionManagerFactory;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;
import org.glassfish.jersey.microprofile.restclient.JerseyRestClientBuilderResolver;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;

import io.github.solaris.jaxrs.client.test.util.Jackson2BusFactory;
import io.github.solaris.jaxrs.client.test.util.Jackson3BusFactory;

public enum JaxRsVendor {
    JERSEY(
            org.glassfish.jersey.internal.RuntimeDelegateImpl.class,
            JerseyClientBuilder.class,
            JerseyRestClientBuilderResolver.class,
            NonInjectionManagerFactory.class,
            null
    ),
    JERSEY_HK2(
            org.glassfish.jersey.internal.RuntimeDelegateImpl.class,
            JerseyClientBuilder.class,
            JerseyRestClientBuilderResolver.class,
            Hk2InjectionManagerFactory.class,
            null
    ),
    RESTEASY(
            ResteasyProviderFactoryImpl.class,
            ResteasyClientBuilderImpl.class,
            org.jboss.resteasy.microprofile.client.BuilderResolver.class
    ),
    CXF(
            org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl.class,
            org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl.class,
            CxfRestClientBuilderResolver.class,
            null,
            Jackson2BusFactory.class
    ),
    CXF_JACKSON3(
            org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl.class,
            org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl.class,
            CxfRestClientBuilderResolver.class,
            null,
            Jackson3BusFactory.class
    ),
    RESTEASY_REACTIVE(
            org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl.class,
            org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl.class,
            io.quarkus.rest.client.reactive.runtime.BuilderResolver.class
    );

    static final List<JaxRsVendor> VENDORS = Stream.of(values())
            .filter(vendor -> Arrays.asList(System.getProperty("vendors.enabled").split(",")).contains(vendor.name()))
            .toList();

    private final Class<? extends RuntimeDelegate> runtimeDelegateClass;
    private final Class<? extends ClientBuilder> clientBuilderClass;
    private final Class<? extends RestClientBuilderResolver> restClientBuilderResolverClass;

    private final Class<? extends InjectionManagerFactory> injectionManagerFactoryClass;
    private final Class<? extends BusFactory> busFactoryClass;

    private final ClassLoader vendorClassLoader;

    JaxRsVendor(
            Class<? extends RuntimeDelegate> runtimeDelegateClass,
            Class<? extends ClientBuilder> clientBuilderClass,
            Class<? extends RestClientBuilderResolver> restClientBuilderResolverClass
    ) {
        this(runtimeDelegateClass, clientBuilderClass, restClientBuilderResolverClass, null, null);
    }

    JaxRsVendor(
            Class<? extends RuntimeDelegate> runtimeDelegateClass,
            Class<? extends ClientBuilder> clientBuilderClass,
            Class<? extends RestClientBuilderResolver> restClientBuilderResolverClass,
            Class<? extends InjectionManagerFactory> injectionManagerFactoryClass,
            Class<? extends BusFactory> busFactoryClass
    ) {
        this.runtimeDelegateClass = runtimeDelegateClass;
        this.clientBuilderClass = clientBuilderClass;
        this.restClientBuilderResolverClass = restClientBuilderResolverClass;
        this.injectionManagerFactoryClass = injectionManagerFactoryClass;
        this.busFactoryClass = busFactoryClass;

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

    Class<? extends BusFactory> getBusFactoryClass() {
        return busFactoryClass;
    }

    ClassLoader getVendorClassLoader() {
        return vendorClassLoader;
    }

    boolean isCxf() {
        return this == CXF || this == CXF_JACKSON3;
    }
}
