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
import io.micronaut.http.body.AsyncRequestBody;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.session.Session;
import io.micronaut.session.SessionStore;
import io.micronaut.vaadin.VaadinConfigurationProperties;
import io.micronaut.vaadin.startup.VaadinStartup;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

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
    private static final String VAADIN_PATH = "/" + Constants.VAADIN_MAPPING.substring(0, Constants.VAADIN_MAPPING.length() - 1);

    private final NettyServletContext servletContext;
    private final NettyVaadinServlet servlet;
    private final SessionStore<Session> sessionStore;
    private final String prefix;

    @SuppressWarnings("unchecked")
    VaadinNettyRuntime(ApplicationContext applicationContext,
                       VaadinConfigurationProperties configuration,
                       VaadinStartup startup,
                       @SuppressWarnings("rawtypes") SessionStore sessionStore) throws ServletException {
        this.sessionStore = sessionStore;
        this.prefix = prefixOf(configuration.getUrlMapping());

        List<String> mappings = new ArrayList<>();
        mappings.add(configuration.getUrlMapping());
        NettyServletRegistration registration = new NettyServletRegistration(SERVLET_NAME, NettyVaadinServlet.class.getName(), mappings);
        if (prefix.isEmpty()) {
            registration.setInitParameter(VaadinServlet.INTERNAL_VAADIN_SERVLET_VITE_DEV_MODE_FRONTEND_PATH, "");
        }
        this.servletContext = new NettyServletContext(VaadinNettyRuntime.class.getClassLoader(), registration);
        startup.initialize(servletContext);
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
     *
     * @param request The request
     * @param body    Its body
     * @return The response
     */
    HttpResponse<?> service(HttpRequest<?> request, AsyncRequestBody body) {
        String path = request.getPath();
        String servletPath;
        @Nullable String pathInfo;
        if (prefix.isEmpty()) {
            servletPath = "";
            pathInfo = path;
        } else if (path.equals(prefix) || path.startsWith(prefix + "/")) {
            servletPath = prefix;
            pathInfo = path.length() == prefix.length() ? null : path.substring(prefix.length());
        } else {
            // the frontend resources of Vaadin, under /VAADIN whatever the mapping of the views
            servletPath = VAADIN_PATH;
            pathInfo = path.substring(VAADIN_PATH.length());
        }
        try (CloseableByteBody byteBody = body.takeBody(); InputStream in = byteBody.toInputStream()) {
            NettyHttpServletRequest servletRequest = new NettyHttpServletRequest(request, in, servletContext, sessionStore, servletPath, pathInfo);
            NettyHttpServletResponse servletResponse = new NettyHttpServletResponse();
            servlet.service(servletRequest, servletResponse);
            return servletResponse.toHttpResponse();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ServletException e) {
            throw new IllegalStateException("Vaadin failed to serve " + path, e);
        }
    }

    @PreDestroy
    void destroy() {
        servlet.destroy();
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
