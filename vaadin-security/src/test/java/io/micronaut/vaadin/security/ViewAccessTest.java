package io.micronaut.vaadin.security;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaadin checks the access to its views with the users of Micronaut Security, here authenticated with
 * HTTP Basic, while Micronaut Security keeps protecting the controllers.
 */
@MicronautTest
@Property(name = "vaadin.production-mode", value = "true")
@Property(name = "vaadin.eager-server-load", value = "true")
@Property(name = "vaadin.security.login-view", value = "/login")
class ViewAccessTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void anonymousUsersSeeAnonymousViews() {
        assertTrue(page(HttpRequest.GET("/")).contains("Welcome"));
    }

    @Test
    void anonymousUsersAreSentToTheLoginView() {
        String page = page(HttpRequest.GET("/admin"));
        assertFalse(page.contains("Admin area"), page);
        // the client is redirected to the login view
        assertTrue(page.contains("[\"/login\",\"_self\""), page);
    }

    @Test
    void usersWithTheRoleSeeTheView() {
        assertTrue(page(HttpRequest.GET("/admin").basicAuth("admin", "secret")).contains("Admin area"));
    }

    @Test
    void usersWithoutTheRoleAreDenied() {
        String page = page(HttpRequest.GET("/admin").basicAuth("user", "secret"));
        assertFalse(page.contains("Admin area"), page);
        // in production mode, Vaadin answers a denied view as a view that does not exist
        assertTrue(page.startsWith("NOT_FOUND") && page.contains("Could not navigate"), page);
    }

    @Test
    void viewsKnowTheAuthenticatedUser() {
        assertTrue(page(HttpRequest.GET("/profile").basicAuth("user", "secret")).contains("Hello user"));
    }

    @Test
    void micronautSecurityStillProtectsTheControllers() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
            () -> client.toBlocking().retrieve(HttpRequest.GET("/api/secret")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
        assertEquals("secret", client.toBlocking().retrieve(HttpRequest.GET("/api/secret").basicAuth("user", "secret")));
    }

    /**
     * The page Vaadin serves, whatever its status.
     */
    private String page(HttpRequest<?> request) {
        try {
            return client.toBlocking().retrieve(request);
        } catch (HttpClientResponseException e) {
            return e.getStatus() + " " + e.getResponse().getBody(String.class).orElse("");
        }
    }
}
