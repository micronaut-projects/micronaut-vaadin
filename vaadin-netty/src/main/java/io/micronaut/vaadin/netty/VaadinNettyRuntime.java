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

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServlet;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.body.AsyncRequestBody;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.session.Session;
import io.micronaut.session.SessionStore;
import io.micronaut.vaadin.VaadinConfigurationProperties;
import io.micronaut.vaadin.startup.VaadinStartup;
import io.micronaut.websocket.WebSocketSession;
import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.cpr.WebSocketProcessorFactory;
import org.atmosphere.websocket.WebSocketProcessor;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Runs the Vaadin servlet on Netty: starts Vaadin with the types known at compile time, then serves the
 * requests that {@link VaadinNettyRoutes} hands it, on a thread that may block.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Context
final class VaadinNettyRuntime {

    static final String SERVLET_NAME = "vaadinServlet";
    private static final Logger LOG = LoggerFactory.getLogger(VaadinNettyRuntime.class);
    private static final String WEBSOCKET_ATTRIBUTE = AtmosphereWebSocket.class.getName();
    private static final String VAADIN_PATH = "/" + Constants.VAADIN_MAPPING.substring(0, Constants.VAADIN_MAPPING.length() - 1);

    private final NettyServletContext servletContext;
    private final NettyVaadinServlet servlet;
    private final SessionStore<Session> sessionStore;
    private final TaskScheduler scheduler;
    private final Executor executor;
    private final String prefix;
    private final AtmosphereFramework atmosphere;

    @SuppressWarnings("unchecked")
    VaadinNettyRuntime(ApplicationContext applicationContext,
                       VaadinConfigurationProperties configuration,
                       VaadinStartup startup,
                       @SuppressWarnings("rawtypes") SessionStore sessionStore,
                       @Named(TaskExecutors.SCHEDULED) TaskScheduler scheduler,
                       @Named(TaskExecutors.BLOCKING) Executor executor) throws ServletException {
        this.sessionStore = sessionStore;
        this.scheduler = scheduler;
        this.executor = executor;
        this.prefix = prefixOf(configuration.getUrlMapping());

        List<String> mappings = new ArrayList<>();
        mappings.add(configuration.getUrlMapping());
        NettyServletRegistration registration = new NettyServletRegistration(SERVLET_NAME, NettyVaadinServlet.class.getName(), mappings);
        if (prefix.isEmpty()) {
            registration.setInitParameter(VaadinServlet.INTERNAL_VAADIN_SERVLET_VITE_DEV_MODE_FRONTEND_PATH, "");
        }
        // push: Atmosphere is initialized here with the asynchronous support of Netty, and the Vaadin servlet reuses it
        registration.setInitParameter(ApplicationConfig.PROPERTY_COMET_SUPPORT, NettyAtmosphereSupport.class.getName());
        this.servletContext = new NettyServletContext(VaadinNettyRuntime.class.getClassLoader(), registration);
        startup.initialize(servletContext);
        JSR356WebsocketInitializer.initAtmosphereForVaadinServlet(registration, servletContext);
        this.atmosphere = (AtmosphereFramework) Objects.requireNonNull(
            servletContext.getAttribute(JSR356WebsocketInitializer.getAttributeName(SERVLET_NAME)), "Atmosphere was not initialized");
        this.servlet = new NettyVaadinServlet(applicationContext);
        servlet.init(registration.toServletConfig(servletContext));
    }

    /**
     * @return The path under which Vaadin serves its views, empty when Vaadin serves the root
     */
    String getPrefix() {
        return prefix;
    }

    /**
     * Serves a request with the Vaadin servlet. Blocks while Vaadin reads the body and writes the response.
     * When Vaadin makes the request asynchronous, for push over long polling, the response is sent once it
     * completes, without holding the thread.
     *
     * @param request The request
     * @param body    Its body
     * @return The response
     */
    CompletionStage<HttpResponse<?>> service(HttpRequest<?> request, AsyncRequestBody body) {
        if (isMultipart(request)) {
            // uploads: the parts are read into temporary files, which Vaadin reads through getParts()
            List<NettyPart> parts = readParts(body);
            return service(request, InputStream.nullInputStream(), parts)
                .whenComplete((response, error) -> deleteParts(parts));
        }
        try (CloseableByteBody byteBody = body.takeBody(); InputStream in = byteBody.toInputStream()) {
            return service(request, in, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CompletionStage<HttpResponse<?>> service(HttpRequest<?> request, InputStream in, @Nullable List<NettyPart> parts) {
        String path = request.getPath();
        NettyHttpServletRequest servletRequest = new NettyHttpServletRequest(request, in, servletContext, sessionStore, servletPath(path), pathInfo(path));
        if (parts != null) {
            servletRequest.setParts(parts);
        }
        NettyHttpServletResponse servletResponse = new NettyHttpServletResponse();
        servletRequest.setAsyncContextFactory(() -> new NettyAsyncContext(servletRequest, servletResponse, scheduler, executor));
        try {
            servlet.service(servletRequest, servletResponse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ServletException e) {
            throw new IllegalStateException("Vaadin failed to serve " + path, e);
        }
        NettyAsyncContext asyncContext = servletRequest.asyncContext();
        if (asyncContext == null) {
            return CompletableFuture.completedFuture(servletResponse.toHttpResponse());
        }
        return asyncContext.whenComplete().thenApply(done -> servletResponse.toHttpResponse());
    }

    private static boolean isMultipart(HttpRequest<?> request) {
        return request.getContentType()
            .map(type -> "multipart".equalsIgnoreCase(type.getType()))
            .orElse(false);
    }

    private static List<NettyPart> readParts(AsyncRequestBody body) {
        List<NettyPart> parts = new ArrayList<>();
        try {
            body.parts().forEach(part -> {
                Path file;
                try {
                    file = Files.createTempFile("vaadin-upload-", ".part");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                String contentType = part.contentType().map(MediaType::toString).orElse(null);
                parts.add(new NettyPart(part.name(), part.fileName(), contentType, file));
                return part.transferTo(file);
            }).toCompletableFuture().join();
        } catch (RuntimeException e) {
            deleteParts(parts);
            throw e;
        }
        return parts;
    }

    private static void deleteParts(List<NettyPart> parts) {
        for (NettyPart part : parts) {
            try {
                part.delete();
            } catch (IOException e) {
                LOG.debug("Cannot delete the temporary file of an uploaded part", e);
            }
        }
    }

    /**
     * Opens a WebSocket of Vaadin's push.
     *
     * @param session The WebSocket session
     * @param request The handshake request
     */
    void openPush(WebSocketSession session, HttpRequest<?> request) {
        AtmosphereWebSocket webSocket = new AtmosphereWebSocket(atmosphere.getAtmosphereConfig(), session, executor);
        session.put(WEBSOCKET_ATTRIBUTE, webSocket);
        String path = request.getPath();
        NettyHttpServletRequest servletRequest = new NettyHttpServletRequest(request, InputStream.nullInputStream(), servletContext, sessionStore,
            servletPath(path), pathInfo(path));
        AtmosphereRequest atmosphereRequest = AtmosphereRequestImpl.wrap(servletRequest);
        AtmosphereResponse atmosphereResponse = AtmosphereResponseImpl.newInstance(atmosphere.getAtmosphereConfig(), atmosphereRequest, webSocket);
        webSocket.runInOrder(() -> {
            try {
                processor().open(webSocket, atmosphereRequest, atmosphereResponse);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Hands a message of a WebSocket of Vaadin's push to Atmosphere.
     *
     * @param session The WebSocket session
     * @param message The message
     */
    void pushMessage(WebSocketSession session, String message) {
        session.get(WEBSOCKET_ATTRIBUTE, AtmosphereWebSocket.class)
            .ifPresent(webSocket -> webSocket.runInOrder(() -> processor().invokeWebSocketProtocol(webSocket, message)));
    }

    /**
     * Closes a WebSocket of Vaadin's push.
     *
     * @param session The WebSocket session
     * @param code    The close code
     */
    void closePush(WebSocketSession session, int code) {
        session.get(WEBSOCKET_ATTRIBUTE, AtmosphereWebSocket.class)
            .ifPresent(webSocket -> webSocket.runInOrder(() -> processor().close(webSocket, code)));
    }

    private String servletPath(String path) {
        if (prefix.isEmpty()) {
            return "";
        }
        if (path.equals(prefix) || path.startsWith(prefix + "/")) {
            return prefix;
        }
        // the frontend resources of Vaadin, under /VAADIN whatever the mapping of the views
        return VAADIN_PATH;
    }

    private @Nullable String pathInfo(String path) {
        String servletPath = servletPath(path);
        return path.length() == servletPath.length() ? null : path.substring(servletPath.length());
    }

    private WebSocketProcessor processor() {
        return WebSocketProcessorFactory.getDefault().getWebSocketProcessor(atmosphere);
    }

    @PreDestroy
    void destroy() {
        servlet.destroy();
        servletContext.destroy();
    }

    private static String prefixOf(String mapping) {
        if ("/*".equals(mapping) || "/".equals(mapping)) {
            return "";
        }
        if (!mapping.startsWith("/") || !mapping.endsWith("/*")) {
            throw new IllegalArgumentException("vaadin.url-mapping must be /* or a path ending with /*, such as /ui/*: " + mapping);
        }
        return mapping.substring(0, mapping.length() - 2);
    }
}
