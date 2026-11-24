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
package io.micronaut.vaadin.servlet;

import io.micronaut.core.util.PathMatcher;
import io.micronaut.http.HttpMethod;
import io.micronaut.web.router.Router;
import io.micronaut.web.router.resource.StaticResourceResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;
import java.util.List;

/**
 * Lets Vaadin serve the root of the application next to Micronaut: a request goes to Micronaut when a
 * route or a static resource matches it, or its path is excluded with {@code vaadin.exclude-urls}.
 * Every other request is forwarded to the Vaadin servlet.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class VaadinRootFilter extends HttpFilter {

    static final String NAME = "vaadinRootFilter";

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Router router;
    private final transient StaticResourceResolver staticResources;
    private final List<String> excludeUrls;

    VaadinRootFilter(Router router, StaticResourceResolver staticResources, List<String> excludeUrls) {
        this.router = router;
        this.staticResources = staticResources;
        this.excludeUrls = List.copyOf(excludeUrls);
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }
        if (handledByMicronaut(request.getMethod(), path)) {
            chain.doFilter(request, response);
            return;
        }
        RequestDispatcher vaadin = request.getServletContext().getNamedDispatcher(VaadinServletInitializer.SERVLET_NAME);
        vaadin.forward(request, response);
    }

    private boolean handledByMicronaut(String method, String path) {
        for (String pattern : excludeUrls) {
            if (PathMatcher.ANT.matches(pattern, path)) {
                return true;
            }
        }
        if (router.find(HttpMethod.parse(method), path, null).findAny().isPresent()) {
            return true;
        }
        return !"/".equals(path) && staticResources.resolve(path).isPresent();
    }
}
