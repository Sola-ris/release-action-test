package io.github.solaris.jaxrs.client.test.util.extension;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class VendorClassLoader extends ClassLoader {
    private final JaxRsVendor vendor;

    private Path runtimeDelegatePath;
    private Path clientBuilderPath;
    private Path restClientBuilderResolverPath;
    private Path injectionManagerFactoryPath;

    VendorClassLoader(JaxRsVendor vendor) {
        super(vendor.name(), VendorClassLoader.class.getClassLoader());
        this.vendor = vendor;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return switch (name) {
            case "META-INF/services/jakarta.ws.rs.ext.RuntimeDelegate" -> getRuntimeDelegateService();
            case "META-INF/services/jakarta.ws.rs.client.ClientBuilder" -> getClientBuilderService();
            case "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver" -> getRestClientBuilderResolverService();
            case "META-INF/services/org.glassfish.jersey.internal.inject.InjectionManagerFactory" -> getInjectionManagerFactoryService();
            default -> super.getResources(name);
        };
    }

    private Enumeration<URL> getRuntimeDelegateService() throws IOException {
        if (runtimeDelegatePath != null) {
            return Collections.enumeration(List.of(runtimeDelegatePath.toUri().toURL()));
        }

        runtimeDelegatePath = Files.createTempFile(vendor.getRuntimeDelegateClass().getName(), "");
        Files.writeString(runtimeDelegatePath, vendor.getRuntimeDelegateClass().getName() + System.lineSeparator());

        runtimeDelegatePath.toFile().deleteOnExit();

        return Collections.enumeration(List.of(runtimeDelegatePath.toUri().toURL()));
    }

    private Enumeration<URL> getClientBuilderService() throws IOException {
        if (clientBuilderPath != null) {
            return Collections.enumeration(List.of(clientBuilderPath.toUri().toURL()));
        }

        clientBuilderPath = Files.createTempFile(vendor.getClientBuilderClass().getName(), "");
        Files.writeString(clientBuilderPath, vendor.getClientBuilderClass().getName() + System.lineSeparator());

        clientBuilderPath.toFile().deleteOnExit();

        return Collections.enumeration(List.of(clientBuilderPath.toUri().toURL()));
    }

    private Enumeration<URL> getRestClientBuilderResolverService() throws IOException {
        if (restClientBuilderResolverPath != null) {
            return Collections.enumeration(List.of(restClientBuilderResolverPath.toUri().toURL()));
        }

        restClientBuilderResolverPath = Files.createTempFile(vendor.getRestClientBuilderResolverClass().getName(), "");
        Files.writeString(restClientBuilderResolverPath, vendor.getRestClientBuilderResolverClass().getName() + System.lineSeparator());

        restClientBuilderResolverPath.toFile().deleteOnExit();

        return Collections.enumeration(List.of(restClientBuilderResolverPath.toUri().toURL()));
    }

    private Enumeration<URL> getInjectionManagerFactoryService() throws IOException {
        if (injectionManagerFactoryPath != null) {
            return Collections.enumeration(List.of(injectionManagerFactoryPath.toUri().toURL()));
        }

        injectionManagerFactoryPath = Files.createTempFile(vendor.getInjectionManagerFactoryClass().getName(), "");
        Files.writeString(injectionManagerFactoryPath, vendor.getInjectionManagerFactoryClass().getName() + System.lineSeparator());

        injectionManagerFactoryPath.toFile().deleteOnExit();

        return Collections.enumeration(List.of(injectionManagerFactoryPath.toUri().toURL()));
    }
}
