package io.micronaut.vaadin.netty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.TimeUnit;

/**
 * Opens WebSockets to the server under test.
 */
final class WebSockets {

    private WebSockets() {
    }

    /**
     * @param uri The URI of the endpoint
     * @return Whether the server upgraded the connection to a WebSocket
     */
    static boolean upgrades(URI uri) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            WebSocket webSocket = client.newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                })
                .get(10, TimeUnit.SECONDS);
            webSocket.abort();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
