module io.github.solaris.jaxrs.client.test {
    requires transitive jakarta.ws.rs;
    requires transitive org.jspecify;
    requires transitive java.xml;
    requires json.path;

    exports io.github.solaris.jaxrs.client.test.request;
    exports io.github.solaris.jaxrs.client.test.response;
    exports io.github.solaris.jaxrs.client.test.server;

    // Required so vendors can @Context inject the Providers in MockResponseFilter
    opens io.github.solaris.jaxrs.client.test.server;
}
