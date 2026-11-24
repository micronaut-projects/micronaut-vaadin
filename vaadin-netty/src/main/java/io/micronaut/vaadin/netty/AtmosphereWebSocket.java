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

import io.micronaut.websocket.WebSocketSession;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A WebSocket of Atmosphere over a WebSocket session of Micronaut.
 *
 * <p>Atmosphere expects the events of a WebSocket one at a time and in order, as a JSR-356 container
 * delivers them, so they run one after the other on the blocking executor.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class AtmosphereWebSocket extends WebSocket {

    private static final Logger LOG = LoggerFactory.getLogger(AtmosphereWebSocket.class);

    private final WebSocketSession session;
    private final Executor executor;
    private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

    AtmosphereWebSocket(AtmosphereConfig config, WebSocketSession session, Executor executor) {
        super(config);
        this.session = session;
        this.executor = executor;
    }

    /**
     * Runs an event of the WebSocket after the previous ones.
     *
     * @param event The event
     */
    synchronized void runInOrder(Runnable event) {
        tail = tail.thenRunAsync(event, executor).exceptionally(e -> {
            LOG.warn("Vaadin's push failed to handle a WebSocket event", e);
            return null;
        });
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public WebSocket write(String data) {
        if (session.isOpen()) {
            session.sendSync(data);
        }
        return this;
    }

    @Override
    public WebSocket write(byte[] data, int offset, int length) {
        // Vaadin's push messages are text
        return write(new String(Arrays.copyOfRange(data, offset, offset + length), StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        session.close();
    }
}
