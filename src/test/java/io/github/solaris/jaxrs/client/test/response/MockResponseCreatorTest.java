package io.github.solaris.jaxrs.client.test.response;

import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.anything;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_ENCODING;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static jakarta.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LANGUAGE;
import static jakarta.ws.rs.core.HttpHeaders.ETAG;
import static jakarta.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static jakarta.ws.rs.core.HttpHeaders.VARY;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.NewCookie.SameSite.NONE;
import static jakarta.ws.rs.core.NewCookie.SameSite.STRICT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import java.time.Instant;
import java.time.Year;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

class MockResponseCreatorTest {

    @JaxRsVendorTest
    void testResponseWithStatus() {
        try (Response response = new MockResponseCreator(OK).createResponse(new MockClientRequestContext())) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
        }
    }

    @JaxRsVendorTest
    void testResponseWithMediaType() {
        try (Response response = new MockResponseCreator(OK).mediaType(APPLICATION_JSON_TYPE).createResponse(new MockClientRequestContext())) {
            assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
        }
    }

    @JaxRsVendorTest
    void testResponseWithHeaders() {
        try (Response response = new MockResponseCreator(OK)
                .header(ACCEPT_ENCODING, "gzip", "deflate", "br")
                .header(ACCEPT, WILDCARD)
                .createResponse(new MockClientRequestContext())) {
            assertThat(response.getHeaders()).satisfies(
                    headers -> assertThat(headers.get(ACCEPT_ENCODING)).containsExactlyInAnyOrder("gzip", "deflate", "br"),
                    headers -> assertThat(headers.get(ACCEPT)).singleElement().isEqualTo(WILDCARD)
            );
        }
    }

    @JaxRsVendorTest
    void testRespondWithEntity() {
        String json = "{\"foo\": true}";
        try (Response response = new MockResponseCreator(OK).entity(json).createResponse(new MockClientRequestContext())) {
            assertThat(response.getEntity()).isEqualTo(json);
        }
    }

    @JaxRsVendorTest
    void testRespondWithCookies() {
        NewCookie sessionCookie = new NewCookie.Builder("session-token")
                .maxAge(-1)
                .comment("top-secret")
                .value("123456")
                .sameSite(STRICT)
                .secure(true)
                .version(42)
                .build();
        NewCookie themeCookie = new NewCookie.Builder("theme")
                // Truncated to seconds to prevent differences in millis after parsing
                .expiry(Date.from(Instant.now().plus(Year.now().length(), DAYS).truncatedTo(SECONDS)))
                .maxAge(Long.valueOf(YEARS.getDuration().getSeconds()).intValue())
                .secure(false)
                .sameSite(NONE)
                .value("dark")
                .build();

        try (Response response = new MockResponseCreator(OK)
                .cookies(sessionCookie, themeCookie)
                .createResponse(new MockClientRequestContext())) {
            assertThat(response.getCookies()).satisfies(
                    cookies -> assertThat(cookies.get("session-token")).isEqualTo(sessionCookie),
                    cookies -> assertThat(cookies.get("theme")).isEqualTo(themeCookie)
            );
        }
    }

    @JaxRsVendorTest
    void testRespondWithLinks() {
        Link nextPage = Link.fromUri("http://local.host?page=3")
                .title("Page 3")
                .rel("next")
                .type(TEXT_HTML)
                .param("greeting", "hello")
                .build();
        Link prevPage = Link.fromUriBuilder(UriBuilder.fromUri("?page={page}"))
                .title("Page 1")
                .rel("prev")
                .type(TEXT_HTML)
                .param("sendoff", "goodbye")
                .build("1");

        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        // Run the Response through a client because otherwise Response::getLinks throws an NPE in CXF if a Link's URI is relative
        server.expect(anything()).andRespond(new MockResponseCreator(OK).links(prevPage, nextPage));

        try (client) {
            assertThat(client.target("").request().get()).satisfies(
                    r -> assertThat(r.getStatusInfo().toEnum()).isEqualTo(OK),
                    r -> assertThat(r.getLinks()).containsExactlyInAnyOrder(nextPage, prevPage)
            );
        }
    }

    @JaxRsVendorTest
    void testRespondWithVariants() {
        List<Variant> variants = Variant.mediaTypes(APPLICATION_JSON_TYPE, APPLICATION_XML_TYPE)
                .languages(ENGLISH, GERMAN, FRENCH)
                .encodings(UTF_8.name(), UTF_16.name())
                .build();

        try (Response response =
                     new MockResponseCreator(OK).variants(variants.toArray(new Variant[0])).createResponse(new MockClientRequestContext())) {
            assertThat(response.getHeaderString(VARY)).contains(ACCEPT, ACCEPT_ENCODING, ACCEPT_LANGUAGE);
        }
    }

    // Other typed headers, URI is covered by MockResponseCreatorsTest#testCreated

    @JaxRsVendorTest
    void testRespondWithCacheControl() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(42);
        cacheControl.setSMaxAge(42);
        cacheControl.setNoCache(true);
        cacheControl.setPrivate(true);
        cacheControl.setNoStore(true);
        cacheControl.setNoTransform(true);
        cacheControl.setMustRevalidate(true);
        cacheControl.setProxyRevalidate(true);

        try (Response response = new MockResponseCreator(OK).header(CACHE_CONTROL, cacheControl).createResponse(new MockClientRequestContext())) {
            assertThat(response.getHeaders())
                    .containsKey(CACHE_CONTROL)
                    .extractingByKey(CACHE_CONTROL)
                    .asInstanceOf(LIST)
                    .singleElement()
                    .isEqualTo(cacheControl);
        }
    }

    @JaxRsVendorTest
    void testRespondWithETag() {
        EntityTag entityTag = new EntityTag(UUID.randomUUID().toString().replace("-", ""), true);

        try (Response response = new MockResponseCreator(OK).header(ETAG, entityTag).createResponse(new MockClientRequestContext())) {
            assertThat(response.getEntityTag()).isEqualTo(entityTag);
        }
    }

    @JaxRsVendorTest
    void testRespondWithLanguage() {
        try (Response response = new MockResponseCreator(OK).header(CONTENT_LANGUAGE, ENGLISH).createResponse(new MockClientRequestContext())) {
            assertThat(response.getLanguage()).isEqualTo(ENGLISH);
        }
    }

    @JaxRsVendorTest
    void testRespondWithLastModified() {
        Date lastModified = new Date();

        try (Response response = new MockResponseCreator(OK).header(LAST_MODIFIED, lastModified).createResponse(new MockClientRequestContext())) {
            assertThat(response.getLastModified()).isEqualTo(lastModified);
        }
    }
}
