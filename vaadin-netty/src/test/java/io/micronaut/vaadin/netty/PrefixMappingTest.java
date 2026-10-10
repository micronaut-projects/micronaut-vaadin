package io.micronaut.vaadin.netty;

import io.micronaut.context.annotation.Property;
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
 * Vaadin mapped under a prefix: the rest of the application belongs to Micronaut.
 */
@MicronautTest
@Property(name = "vaadin.production-mode", value = "true")
@Property(name = "vaadin.eager-server-load", value = "true")
@Property(name = "vaadin.url-mapping", value = "/ui/*")
class PrefixMappingTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void vaadinServesItsPrefix() {
        String body = client.toBlocking().retrieve("/ui/");
        assertTrue(body.contains("Hello from Vaadin"), body);
    }

    @Test
    void theRestBelongsToMicronaut() {
        assertEquals("Hello from Micronaut", client.toBlocking().retrieve("/api/hello"));
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> client.toBlocking().retrieve("/missing"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
