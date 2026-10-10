package io.micronaut.vaadin.netty;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaadin mapped to the root of the application, next to a controller. The production frontend bundle is
 * replaced by a stand-in index.html, and the first view is rendered into it on the server.
 */
@MicronautTest
@Property(name = "vaadin.production-mode", value = "true")
@Property(name = "vaadin.eager-server-load", value = "true")
@Property(name = "vaadin.exclude-urls", value = "/excluded/**")
class RootMappingTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmbeddedServer server;

    @Test
    void vaadinRendersTheViewWithItsInjectedService() {
        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET("/"), String.class);
        assertEquals(HttpStatus.OK, response.status());
        assertTrue(response.body().contains("Hello from Vaadin"), response.body());
    }

    @Test
    void vaadinKeepsItsStateInTheHttpSession() throws Exception {
        // a client without cookies, so the session is always new
        java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient().send(
            java.net.http.HttpRequest.newBuilder(server.getURI().resolve("/")).build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
        assertTrue(response.headers().allValues("Set-Cookie").stream().anyMatch(cookie -> cookie.startsWith("SESSION=")),
            () -> "No session cookie: " + response.headers().map());
    }

    @Test
    void controllersKeepTheirRoutes() {
        assertEquals("Hello from Micronaut", client.toBlocking().retrieve("/api/hello"));
    }

    @Test
    void unknownPathsGoToVaadin() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().retrieve("/missing"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        String body = e.getResponse().getBody(String.class).orElse("");
        assertTrue(body.contains("Could not navigate"), body);
    }

    @Test
    void excludedPathsStayWithMicronaut() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().retrieve("/excluded/page"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
