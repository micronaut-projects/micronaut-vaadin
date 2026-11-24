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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The registration of the Vaadin servlet on Netty, which also serves as its {@link ServletConfig}.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyServletRegistration implements ServletRegistration {

    private final String name;
    private final String className;
    private final List<String> mappings;
    private final Map<String, String> initParameters = new LinkedHashMap<>();

    NettyServletRegistration(String name, String className, List<String> mappings) {
        this.name = name;
        this.className = className;
        this.mappings = List.copyOf(mappings);
    }

    /**
     * @param servletContext The servlet context
     * @return The configuration of the servlet
     */
    ServletConfig toServletConfig(ServletContext servletContext) {
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return name;
            }

            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }

            @Override
            public @Nullable String getInitParameter(String name) {
                return initParameters.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(initParameters.keySet());
            }
        };
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        throw new UnsupportedOperationException("The mappings of the Vaadin servlet come from vaadin.url-mapping");
    }

    @Override
    public Collection<String> getMappings() {
        return mappings;
    }

    @Override
    public @Nullable String getRunAsRole() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return initParameters.putIfAbsent(name, value) == null;
    }

    @Override
    public @Nullable String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        initParameters.forEach(this::setInitParameter);
        return Set.of();
    }

    @Override
    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParameters);
    }
}
