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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A filter of the Vaadin servlet. A bean of this type filters every request that Vaadin handles,
 * including those forwarded to it when Vaadin serves the root of the application, and no other request.
 *
 * <p>It is not a {@code jakarta.servlet.Filter}: Micronaut Servlet registers the beans of that type for
 * every request.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@FunctionalInterface
public interface VaadinServletFilter {

    /**
     * Filters a request of Vaadin.
     *
     * @param request  The request
     * @param response The response
     * @param chain    The rest of the chain, ending with the Vaadin servlet
     * @throws IOException      If the request or the response fail
     * @throws ServletException If the request cannot be handled
     */
    void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;
}
