package io.micronaut.vaadin.netty;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The servlet WebSocket container, where Vaadin registers the proxy of Vite's WebSocket.
 */
class NettyServerContainerTest {

    private final NettyServerContainer container = new NettyServerContainer();
    private final URI uri = URI.create("ws://localhost/socket");

    @Test
    void endpointsAreFoundByClass() throws DeploymentException {
        assertNull(container.endpoint(Echo.class));
        ServerEndpointConfig echo = echo();
        container.addEndpoint(echo);
        assertSame(echo, container.endpoint(Echo.class));
        assertNull(container.endpoint(Other.class));
    }

    @Test
    void theDefaultsAreKept() {
        container.setAsyncSendTimeout(1);
        container.setDefaultMaxSessionIdleTimeout(2);
        container.setDefaultMaxBinaryMessageBufferSize(3);
        container.setDefaultMaxTextMessageBufferSize(4);
        assertEquals(1, container.getDefaultAsyncSendTimeout());
        assertEquals(2, container.getDefaultMaxSessionIdleTimeout());
        assertEquals(3, container.getDefaultMaxBinaryMessageBufferSize());
        assertEquals(4, container.getDefaultMaxTextMessageBufferSize());
        assertTrue(container.getInstalledExtensions().isEmpty());
    }

    @Test
    void onlyProgrammaticServerEndpointsAreSupported() {
        ServerEndpointConfig echo = echo();
        ClientEndpointConfig client = ClientEndpointConfig.Builder.create().build();
        Echo endpoint = new Echo();
        Object instance = new Object();
        Map<String, String> parameters = Map.of();
        assertThrows(DeploymentException.class, () -> container.addEndpoint(Object.class));
        assertThrows(DeploymentException.class, () -> container.upgradeHttpToWebSocket(instance, instance, echo, parameters));
        assertThrows(DeploymentException.class, () -> container.connectToServer(instance, uri));
        assertThrows(DeploymentException.class, () -> container.connectToServer(Object.class, uri));
        assertThrows(DeploymentException.class, () -> container.connectToServer(endpoint, client, uri));
        assertThrows(DeploymentException.class, () -> container.connectToServer(Echo.class, client, uri));
    }

    private static ServerEndpointConfig echo() {
        // without a configurator, the builder looks for the one of a WebSocket container
        return ServerEndpointConfig.Builder.create(Echo.class, "/echo")
            .configurator(new ServerEndpointConfig.Configurator())
            .build();
    }

    static class Echo extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            // not opened
        }
    }

    static class Other extends Echo {
    }
}
