package io.micronaut.vaadin.servlet;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Push runs over a JSR-356 WebSocket endpoint that Atmosphere registers with the container.
 */
@MicronautTest
@Property(name = "vaadin.production-mode", value = "true")
class PushTest {

    @Inject
    EmbeddedServer server;

    @Test
    void thePushEndpointAcceptsWebSockets() throws Exception {
        URI uri = URI.create("ws://localhost:" + server.getPort()
            + "/VAADIN/push?v-r=push&v-uiId=0&v-pushId=probe&X-Atmosphere-Transport=websocket&X-Atmosphere-tracking-id=0");
        WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(uri, new WebSocket.Listener() { })
            .get(30, TimeUnit.SECONDS);
        // the upgrade succeeded: the container hands the connection to Atmosphere
        assertNotNull(webSocket);
        webSocket.abort();
    }
}
