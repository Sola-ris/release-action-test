package io.github.solaris.jaxrs.client.test.util;

import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

public sealed interface ConfiguredClientSupplier extends Supplier<Client> {

    final class DefaultClientSupplier implements ConfiguredClientSupplier {

        @Override
        public Client get() {
            return ClientBuilder.newClient();
        }
    }

    final class CxfClientSupplier implements ConfiguredClientSupplier {
        @Override
        public Client get() {
            Client client = ClientBuilder.newClient();
            client.register(new JacksonJsonProvider());
            return client;
        }
    }
}
