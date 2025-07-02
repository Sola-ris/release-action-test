package io.github.solaris.jaxrs.client.test.internal;

import java.lang.ref.Cleaner;

import jakarta.ws.rs.client.Client;

/**
 * Centralized {@link Cleaner} instance for deferring {@link Client} closings.
 */
public final class ClientCleaner {
    private static final Cleaner CLEANER = Cleaner.create();

    private ClientCleaner() {}

    public static void register(Object instance, Client client) {
        CLEANER.register(instance, closeClient(client));
    }

    private static Runnable closeClient(Client client) {
        return client::close;
    }
}
