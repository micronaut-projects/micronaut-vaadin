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

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The servlet WebSocket container of the Netty runtime, where Vaadin registers its programmatic endpoints,
 * such as the proxy of Vite's WebSocket in development mode. Netty has no servlet WebSocket support: the
 * runtime serves the endpoints it knows with Micronaut WebSockets, from the configurations registered here.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyServerContainer implements ServerContainer {

    private final List<ServerEndpointConfig> endpoints = new CopyOnWriteArrayList<>();
    private volatile long asyncSendTimeout;
    private volatile long maxSessionIdleTimeout;
    private volatile int maxBinaryMessageBufferSize;
    private volatile int maxTextMessageBufferSize;

    /**
     * @param endpointClass The class of an endpoint
     * @return The configuration under which the endpoint was registered
     */
    @Nullable ServerEndpointConfig endpoint(Class<? extends Endpoint> endpointClass) {
        for (ServerEndpointConfig endpoint : endpoints) {
            if (endpoint.getEndpointClass() == endpointClass) {
                return endpoint;
            }
        }
        return null;
    }

    @Override
    public void addEndpoint(ServerEndpointConfig serverConfig) {
        endpoints.add(serverConfig);
    }

    @Override
    public void addEndpoint(Class<?> endpointClass) throws DeploymentException {
        throw new DeploymentException("Annotated WebSocket endpoints are not supported by Vaadin's Netty runtime: " + endpointClass.getName());
    }

    @Override
    public void upgradeHttpToWebSocket(Object httpServletRequest, Object httpServletResponse, ServerEndpointConfig sec, Map<String, String> pathParameters) throws DeploymentException {
        throw unsupported();
    }

    @Override
    public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException {
        throw unsupported();
    }

    @Override
    public Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException {
        throw unsupported();
    }

    @Override
    public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path) throws DeploymentException {
        throw unsupported();
    }

    @Override
    public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path) throws DeploymentException {
        throw unsupported();
    }

    @Override
    public long getDefaultAsyncSendTimeout() {
        return asyncSendTimeout;
    }

    @Override
    public void setAsyncSendTimeout(long timeoutmillis) {
        asyncSendTimeout = timeoutmillis;
    }

    @Override
    public long getDefaultMaxSessionIdleTimeout() {
        return maxSessionIdleTimeout;
    }

    @Override
    public void setDefaultMaxSessionIdleTimeout(long timeout) {
        maxSessionIdleTimeout = timeout;
    }

    @Override
    public int getDefaultMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    @Override
    public void setDefaultMaxBinaryMessageBufferSize(int max) {
        maxBinaryMessageBufferSize = max;
    }

    @Override
    public int getDefaultMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    @Override
    public void setDefaultMaxTextMessageBufferSize(int max) {
        maxTextMessageBufferSize = max;
    }

    @Override
    public Set<Extension> getInstalledExtensions() {
        return Set.of();
    }

    private static DeploymentException unsupported() {
        return new DeploymentException("Not supported by Vaadin's Netty runtime, which serves WebSockets with Micronaut");
    }
}
