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

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.RouteCondition;
import io.micronaut.security.annotation.Secured;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;

/**
 * The WebSocket endpoint of Vaadin's push on Netty when Vaadin is mapped under a prefix, such as
 * {@code /ui/*}: see {@link VaadinPushWebSocket}. Its events arrive in order on the event loop, and
 * {@link AtmosphereWebSocket} runs them in that order on the blocking executor: Vaadin locks the session
 * of the user while it handles a message.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@ServerWebSocket("/{prefix}/VAADIN/push")
@Secured("isAnonymous()") // Vaadin controls the access to its views
final class VaadinPrefixedPushWebSocket {

    private static final String UPGRADE = "#{T(io.micronaut.vaadin.netty.VaadinPushWebSocket).isUpgrade(request)}";

    private final VaadinNettyRuntime runtime;

    VaadinPrefixedPushWebSocket(VaadinNettyRuntime runtime) {
        this.runtime = runtime;
    }

    // only WebSocket upgrades: the long polling of push, a plain GET of the same path, goes to Vaadin's route
    @RouteCondition(UPGRADE)
    @OnOpen
    void open(WebSocketSession session, HttpRequest<?> upgradeRequest) {
        runtime.openPush(session, upgradeRequest);
    }

    @RouteCondition(UPGRADE)
    @OnMessage
    void message(String message, WebSocketSession session) {
        runtime.pushMessage(session, message);
    }

    @OnClose
    void close(WebSocketSession session, CloseReason reason) {
        runtime.closePush(session, reason.getCode());
    }
}
