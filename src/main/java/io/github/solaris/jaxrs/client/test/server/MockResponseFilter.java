package io.github.solaris.jaxrs.client.test.server;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Providers;

import org.jspecify.annotations.Nullable;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.request.ClientEntityConverter;
import io.github.solaris.jaxrs.client.test.request.EntityConverter;
import io.github.solaris.jaxrs.client.test.request.ProvidersEntityConverter;

/**
 * <p>Filter that redirects the current request to a {@link RequestExpectationManager} bound via {@link MockRestServer}.</p>
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>
 * Not intend to for public use, but must be declared {@code public} so JAX-RS implementations can instantiate it.
 * <strong>It may change without warning!</strong>
 * </p>
 */
public final class MockResponseFilter implements ClientRequestFilter {
    private static final MethodType GET_PROVIDERS_TYPE = MethodType.methodType(Providers.class);
    private static final Set<Class<? extends ClientRequestContext>> SKIP_CLASSES = new HashSet<>();
    private static final Map<Class<? extends ClientRequestContext>, MethodHandle> HANDLE_CACHE = new HashMap<>();

    @Context
    private @Nullable Providers providers;

    public MockResponseFilter() {}

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        EntityConverter converter = getProvidersEntityConverter(requestContext).orElseGet(ClientEntityConverter::new);
        requestContext.setProperty(EntityConverter.class.getName(), converter);

        Object property = requestContext.getConfiguration().getProperty(RequestExpectationManager.class.getName());
        if (property instanceof RequestExpectationManager expectationManager) {
            requestContext.abortWith(
                    expectationManager.validateRequest(requestContext)
            );
        } else {
            String foundType = property == null ? "null" : "a " + property.getClass().getName();
            throw new IllegalStateException("Tried to access the RequestExpectationManager but found " + foundType + " instead.");
        }
    }

    private Optional<EntityConverter> getProvidersEntityConverter(ClientRequestContext requestContext) {
        if (providers != null) {
            return Optional.of(new ProvidersEntityConverter(providers));
        }
        return getProvidersFromHandle(requestContext).map(ProvidersEntityConverter::new);
    }

    private static Optional<Providers> getProvidersFromHandle(ClientRequestContext requestContext) {
        if (SKIP_CLASSES.contains(requestContext.getClass())) {
            return Optional.empty();
        }

        try {
            MethodHandle handle = HANDLE_CACHE.get(requestContext.getClass());
            if (handle == null) {
                handle = MethodHandles.publicLookup().findVirtual(requestContext.getClass(), "getProviders", GET_PROVIDERS_TYPE);
                HANDLE_CACHE.put(requestContext.getClass(), handle);
            }
            return Optional.ofNullable((Providers) handle.invoke(requestContext));
        } catch (Throwable e) {
            SKIP_CLASSES.add(requestContext.getClass());
            return Optional.empty();
        }
    }
}
