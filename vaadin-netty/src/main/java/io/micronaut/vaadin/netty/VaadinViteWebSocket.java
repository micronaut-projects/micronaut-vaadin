/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.vaadin.netty;

import com.vaadin.base.devserver.ViteHandler;
import com.vaadin.base.devserver.viteproxy.ViteWebsocketConnection;
import com.vaadin.base.devserver.viteproxy.ViteWebsocketEndpoint;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.RouteCondition;
import io.micronaut.security.annotation.Secured;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * The proxy of Vite's WebSocket, through which the browser receives the changes of the frontend sources
 * when Vaadin's development mode runs Vite ({@code vaadin.frontend.hotdeploy}). Vaadin registers its own
 * proxy endpoint with the servlet WebSocket container, which Netty does not have: this endpoint relays each
 * connection to Vite in its place, with Vaadin's own connection to Vite.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@ServerWebSocket(value = "/VAADIN/", subprotocols = "vite-hmr,vite-ping")
@Secured("isAnonymous()") // Vaadin controls the access to its views
@Requires(classes = ViteWebsocketConnection.class)
final class VaadinViteWebSocket {

    private static final String UPGRADE = "#{T(io.micronaut.vaadin.netty.VaadinPushWebSocket).isUpgrade(request)}";
    private static final Logger LOG = LoggerFactory.getLogger(VaadinViteWebSocket.class);

    private final VaadinNettyRuntime runtime;
    // by connection: the attributes of a WebSocket session are those of the HTTP session with Micronaut Session
    private final Map<String, ViteWebsocketConnection> connections = new ConcurrentHashMap<>();

    VaadinViteWebSocket(VaadinNettyRuntime runtime) {
        this.runtime = runtime;
    }

    @RouteCondition(UPGRADE)
    @OnOpen
    void open(WebSocketSession session) {
        ServerEndpointConfig endpoint = runtime.serverContainer().endpoint(ViteWebsocketEndpoint.class);
        if (endpoint == null || !(endpoint.getUserProperties().get(ViteWebsocketEndpoint.VITE_HANDLER) instanceof ViteHandler vite)) {
            // Vite does not run: Vaadin serves its development bundle
            session.close(CloseReason.GOING_AWAY);
            return;
        }
        connections.put(session.getId(), new ViteWebsocketConnection(vite.getPort(), vite.getPathToVaadin(), session.getSubprotocol().orElse(null),
            message -> session.sendAsync(message, MediaType.TEXT_PLAIN_TYPE),
            () -> session.close(CloseReason.NORMAL),
            error -> {
                LOG.debug("The connection to Vite failed", error);
                session.close(CloseReason.INTERNAL_ERROR);
            }));
    }

    @RouteCondition(UPGRADE)
    @OnMessage
    void message(String message, WebSocketSession session) throws ExecutionException, InterruptedException {
        ViteWebsocketConnection connection = connections.get(session.getId());
        if (connection != null) {
            connection.send(message);
        }
    }

    @OnClose
    void close(WebSocketSession session) throws ExecutionException, InterruptedException {
        ViteWebsocketConnection connection = connections.remove(session.getId());
        if (connection != null) {
            connection.close();
        }
    }
}
