package io.micronaut.vaadin.devmode;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaadin in development mode: vaadin-dev-server is on the classpath and production mode is off. The
 * application uses only Vaadin's components, so Vaadin serves its pre-built development bundle and needs
 * no Node.js.
 */
@MicronautTest
@Property(name = "vaadin.eager-server-load", value = "true")
class DevModeTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void vaadinServesTheViewWithTheDevelopmentBundle() {
        String page = client.toBlocking().retrieve("/");
        assertTrue(page.contains("Hello from Vaadin"), page);
        assertTrue(page.contains("\"flow/dev-bundle\""), "The page does not use the development bundle: " + page);
        assertTrue(page.contains("\"devToolsEnabled\":true"), "The development tools are not enabled: " + page);
    }
}
