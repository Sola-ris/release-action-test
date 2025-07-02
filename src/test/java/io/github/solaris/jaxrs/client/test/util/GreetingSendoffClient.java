package io.github.solaris.jaxrs.client.test.util;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("")
public interface GreetingSendoffClient extends AutoCloseable {

    @GET
    @Path("/hello")
    Response greeting();

    @GET
    @Path("/goodbye")
    Response sendoff();

    @GET
    @Path("/hello-async")
    CompletionStage<Response> greetAsync();
}
