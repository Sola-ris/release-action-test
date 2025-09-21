package io.github.solaris.jaxrs.client.test.util.extension;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class VendorClassLoader extends ClassLoader {
    private final JaxRsVendor vendor;
    private final Map<Class<?>, List<URL>> serviceCache = new HashMap<>(4);

    VendorClassLoader(JaxRsVendor vendor) {
        super(vendor.name(), VendorClassLoader.class.getClassLoader());
        this.vendor = vendor;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return switch (name) {
            case "META-INF/services/jakarta.ws.rs.ext.RuntimeDelegate" -> getService(vendor.getRuntimeDelegateClass());
            case "META-INF/services/jakarta.ws.rs.client.ClientBuilder" -> getService(vendor.getClientBuilderClass());
            case "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver" ->
                    getService(vendor.getRestClientBuilderResolverClass());
            case "META-INF/services/org.glassfish.jersey.internal.inject.InjectionManagerFactory" ->
                    getService(vendor.getInjectionManagerFactoryClass());
            case "META-INF/services/org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable" -> filterGson(name);
            default -> super.getResources(name);
        };
    }

    private Enumeration<URL> getService(Class<?> clazz) throws IOException {
        List<URL> cached = serviceCache.get(clazz);
        if (cached != null) {
            return Collections.enumeration(cached);
        }

        Path servicePath = Files.createTempFile(clazz.getName(), "");
        Files.writeString(servicePath, clazz.getName() + System.lineSeparator());
        servicePath.toFile().deleteOnExit();

        List<URL> urls = List.of(servicePath.toUri().toURL());
        serviceCache.put(clazz, urls);
        return Collections.enumeration(urls);
    }

    private Enumeration<URL> filterGson(String name) throws IOException {
        List<URL> filtered = Collections.list(super.getResources(name))
                .stream()
                .filter(url -> !url.toString().contains("gson"))
                .toList();
        return Collections.enumeration(filtered);
    }
}
