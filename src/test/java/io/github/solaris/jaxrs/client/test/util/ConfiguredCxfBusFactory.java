package io.github.solaris.jaxrs.client.test.util;

import static org.apache.cxf.jaxrs.provider.ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION;

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

public class ConfiguredCxfBusFactory extends CXFBusFactory {

    @Override
    public Bus createBus() {
        Bus bus = super.createBus();
        bus.setProperty(SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
        bus.setProperty("org.apache.cxf.jaxrs.bus.providers", List.of(JacksonJsonProvider.class));
        return bus;
    }
}
