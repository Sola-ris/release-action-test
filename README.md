# JAX-RS Client Test

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.sola-ris/jax-rs-client-test?color=%234c1)](https://central.sonatype.com/artifact/io.github.sola-ris/jax-rs-client-test)
[![javadoc](https://javadoc.io/badge2/io.github.sola-ris/jax-rs-client-test/javadoc.svg)](https://javadoc.io/doc/io.github.sola-ris/jax-rs-client-test)
[![codecov](https://codecov.io/github/sola-ris/release-action-test/graph/badge.svg?token=G71V79NFGU)](https://codecov.io/github/sola-ris/release-action-test)

A library for testing classes that use a JAX-RS Client without starting a full server or relying on mocking libraries.

Heavily inspired by Spring's [Client testing infrastructure](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-client.html).

* [Compatibility](#compatibility)
* [Usage Guide](#usage-guide)
    * [Basic Usage](#basic-usage)
    * [Repeated requests](#repeated-requests)
    * [Request ordering](#request-ordering)
    * [Mixing stubs and real requests](#mixing-stubs-and-real-responses)
    * [Request matchers](#request-matchers)
        * [Matching the request entity](#matching-the-request-entity)
        * [Matching `multipart/form-data` (`EntityPart`)](#matching-multipartform-data-entitypart)
* [Tested implementations](#tested-implementations)
* [Limitations](#limitations)

## Compatibility

* JDK 17 or higher
* JAX-RS (Jakarta RESTful Web Services) 3.1

## Usage Guide

### Basic Usage

The idea is to declare the expected requests and provide "mock" or "stub" responses so the code can be tested in isolation,
i.e. without starting a server.

The following example shows how to do so with a `Client`:

[@formatter:off]: #
```java
Client client = ClientBuilder.newClient();

MockRestServer server = MockRestServer.bindTo(client).build();
server.expect(RequestMatchers.requestTo("/users/42"))
        .andRespond(MockResponseCreators.withNotFound());

// Test code that uses the Client

server.verify();
```
[@formatter:on]: #

In the example, the `MockRestServer` (the entrypoint into the library) configures the `Client` with a `ClientRequestFilter` that matches the incoming
request against the set-up expectations and returns the "stub" responses. In this case, we expect a call to `/users/42` and want to return
a response with status code `404`. Additional expected requests and stub responses can be defined as needed. At the end of the test, `server.verify()`
can be used to verify that all the expected requests were indeed executed.

### Repeated requests

Unless specified otherwise, ech expected request is expected to be executed exactly once. The `expect` method of the `MockRestServer` provides an
overload that accepts an `ExpectedCount` argument that specifies a range count (e.g. once, between, min, max, etc.) for the given request expectation.

The following example shows how to do so with a `WebTarget`:

[@formatter:off]: #
```java
WebTarget target = ClientBuilder.newClient().target("");

MockRestServer server = MockRestServer.bindTo(target).build();
server.expect(ExpectedCount.max(3), RequestMatchers.requestTo("/hello"))
        .andRespond(MockResponseCreators.withSuccess());
server.expect(ExpectedCount.between(1, 2), RequestMatchers.requestTo("/goodbye"))
        .andRespond(MockResponseCreators.withSuccess());

// Use the bound WebTarget e.g. by passing it to the class under test

server.verify();
```
[@formatter:on]: #

### Request ordering

By default, only the first invocation of each expected request is expected to occur in order of declaration.
This behavior can be changed by passing the desired `RequestOrder` to `withRequestOrder` when building the server.

```java
ClientBuilder clientBuilder = ClientBuilder.newBuilder()

MockRestServer server = MockRestServer.bindTo(clientBuilder)
        .withRequestOrder(RequestOrder.UNORDERED)
        .build();
```

The following options are available:

* ORDERED
    * Expect the first invocation of each request in order of declaration. Subsequent requests may occur in any order. This is the default.
* UNORDERED
    * Expect all invocations in any order.
* STRICT
    * Expect the minimum amount of expected requests to occur in order of declaration. Subsequent requests may occur in any order.

### Mixing stubs and real responses

In some tests it may be necessary to mock only some of the requests and call an actual remote service or others.
This can be done through an `ExecutingResponseCreator`:

[@formatter:off]: #
```java
Client client = ClientBuilder.newClient();

MockRestServer server = MockRestServer.bindTo(client).build();

server.expect(RequestMatchers.requestTo("/profile/42"))
        .andRespond(new ExecutingResponseCreator()); // <1>

Client customClient = ClientBuilder.newBuilder().register(new MyAuthFilter()).build(); // <2>

server.expect(RequestMatchers.requestTo("/users/42")) // <3>
        .andExpect(RequestMatchers.method("GET"))
        .andRespond(new ExecutingResponseCreator(customClient));

server.expect(RequestMatchers.requestTo("/users/42")) // <4>
        .andExpect(RequestMatchers.method("DELETE"))
        .andRespond(MockResponseCreators.withSuccess());

// Test code that uses the Client

customClient.close(); // <5>

server.verify();
```
[@formatter:on]: #

1. Respond to `/profile/42` by calling the actual service with a default Client
2. Create a custom Client with e.g. custom authentication
3. Respond to `GET /users/42` by calling the actual service through `customClient`
4. Respond to `DELETE /users/42` with a stub
5. Clients passed to an `ExecutingResponseCreator` must be closed by the caller

### Request matchers

JAX-RS Client Test comes with a number of built-in `RequestMacher` implementations, all accessed via factory methods in `RequestMatchers`.

* RequestMatchers
    * Matchers for basic request attributes like the URI or HTTP method, access to all other RequestMatcher implementations
* EntityRequestMatchers
    * Matchers related to the request entity / body
* JsonPathRequestMatchers
    * Matchers that evaluate a JsonPath expression against the request body
* XpathRequestMatchers
    * Matchers that evaluate an XPath expression against the request body

Custom request matchers can be implemented as a lambda and can access the current `ClientRequestContext`.
All implementations must throw an `AssertionError` if the incoming request does not match and return if it does.

For example:

[@formatter:off]: #
```java
RequestMatcher myMatcher = request -> {
    if (Locale.ENGLISH.equals(request.getLanguage())) {
        throw new AssertionError("Expected a language other than ENGLISH.")
    }
};
```
[@formatter:on]: #

#### Matching the request entity

When implementing a `RequestMatcher`, the entity can be accessed via `ClientRequestContext#getEntity`.
If the matcher requires a different representation of the entity, e.g. a JSON String instead of POJO, it can be converted via an `EntityConverter`,
which can be obtained by calling `EntityConverter#fromRequestContext`. The `EntityConverter` has the same capabilities as the client that made the
request, so if the client has a provider that can handle POJO -> JSON String conversion, the `EntityConverter` can do it as well.

For example:

[@formatter:off]: #
```java
RequestMatcher myMatcher = request -> {
    EntityConverter converter = EntityConverter.fromRequestContext(request);
    
    // Assuming the MediaType is application/json
    String jsonString = converter.convertEntity(request, String.class);
    
    // Assertions on the string
};
```
[@formatter:on]: #

#### Matching `multipart/form-data` (`EntityPart`)

`EntityPart` has certain limitations that require it to be handled separately from other request entities:

* It is `InputStream` based, meaning its contents can only be accessed once
* Implementations are not required to override `equals`, preventing comparison with another `EntityPart`

The `EntityConverter` provides two methods to work around this:

* `bufferExpectedMultipart`, which takes an arbitrary `List<EntityPart>` and buffers it
* `bufferMultipartRequest`, which takes the `List<EntityPart>` from the current `ClientRequestContext`,
  buffers it and sets the request up for further processing or repeated buffering in another `RequestMatcher`

For example:

[@formatter:off]: #
```java
EntityPart myEntityPart = EntityPart.withName("username")
        .mediaType(MediaType.TEXT_PLAIN_TYPE)
        .content("admin")
        .build();

RequestMatcher myMultipartMatcher = request -> {
    EntityConverter converter = EntityConverter.fromRequestContext(request);
    EntityPart myBufferedPart = converter.bufferExpectedMultipart(List.of(myEntityPart)).get(0);
    // MediaType multipart/form-data and entity of type List<EntityPart> is assumed
    EntityPart bufferedRequestPart = converter.bufferMultipartRequest(request).get(0);

    // Assuming AssertJ is present

    // Reading the content here does not prevent further reads
    // in another matcher or during request execution
    assertThat(myBufferedPart.getContent(String.class))
            .isEqualTo(bufferedRequestPart.getContent(String.class))

    // Buffered EntityParts implement equals()
    assertThat(myBufferedPart)
            .isEqualTo(bufferedRequestPart)
};

// Set up the MockRestServer and execute the request
```
[@formatter:on]: #

## Tested implementations

* [Jersey 3.1.x](https://github.com/eclipse-ee4j/jersey/tree/3.1)
* [RESTEasy](https://github.com/resteasy/resteasy)
* [Apache CXF](https://github.com/apache/cxf)
* [RESTEasy Reactive / Quarkus REST](https://github.com/quarkusio/quarkus)

## Limitations

The library relies on a
[ClientRequestFilter](https://jakarta.ee/specifications/restful-ws/3.1/apidocs/jakarta.ws.rs/jakarta/ws/rs/client/clientrequestfilter)
that calls
[ClientRequestContext#abortWith](https://jakarta.ee/specifications/restful-ws/3.1/apidocs/jakarta.ws.rs/jakarta/ws/rs/client/clientrequestcontext#abortWith(jakarta.ws.rs.core.Response))
in order to create its mock response, making it impossible to test classes that use a client that relies on the same behavior.
