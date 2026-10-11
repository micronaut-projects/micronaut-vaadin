package io.micronaut.vaadin.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalUriTest {

    @Test
    void keepsThePathAndQuery() {
        assertEquals("/profile", LocalUri.of("/profile", null));
        assertEquals("/search?q=a%20b", LocalUri.of("/search", "q=a%20b"));
        assertEquals("/caf%C3%A9", LocalUri.of("/caf%C3%A9", null));
    }

    @Test
    void fallsBackToTheRootForAnythingButALocalPath() {
        assertEquals("/", LocalUri.of("//attacker.example/x", null));
        assertEquals("/", LocalUri.of("http://attacker.example/x", null));
        assertEquals("/", LocalUri.of("relative", null));
        assertEquals("/", LocalUri.of("/bad path", null));
    }
}
