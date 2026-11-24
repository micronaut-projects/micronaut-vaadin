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
package io.micronaut.vaadin;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.naming.NameUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * The Vaadin servlet of Micronaut. It creates a {@link MicronautVaadinServletService} and reads Vaadin's
 * initialization parameters from the application configuration under the {@code vaadin} prefix: for
 * example {@code vaadin.production-mode} sets {@code productionMode}.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MicronautVaadinServlet extends VaadinServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<String> INIT_PARAMETERS = initParameterNames();

    private final transient ApplicationContext applicationContext;
    private final boolean rootMapping;

    /**
     * @param applicationContext The application context
     */
    public MicronautVaadinServlet(ApplicationContext applicationContext) {
        this(applicationContext, false);
    }

    /**
     * @param applicationContext The application context
     * @param rootMapping        Whether Vaadin serves the root of the application, receiving its requests by forwarding
     */
    public MicronautVaadinServlet(ApplicationContext applicationContext, boolean rootMapping) {
        this.applicationContext = applicationContext;
        this.rootMapping = rootMapping;
    }

    /**
     * @return The application context
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(rootMapping && request.getPathInfo() == null ? new RootMappedRequest(request) : request, response);
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException {
        MicronautVaadinServletService service = new MicronautVaadinServletService(this, deploymentConfiguration, applicationContext);
        service.init();
        return service;
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(Properties initParameters) {
        Properties properties = new Properties();
        properties.putAll(initParameters);
        for (String name : INIT_PARAMETERS) {
            configuredValue(name).ifPresent(value -> properties.put(name, value));
        }
        return super.createDeploymentConfiguration(properties);
    }

    private Optional<String> configuredValue(String initParameter) {
        String property = VaadinConfigurationProperties.PREFIX + "." + initParameter;
        Optional<String> value = applicationContext.getProperty(hyphenate(property), String.class);
        return value.isPresent() ? value : applicationContext.getProperty(property, String.class);
    }

    private static String hyphenate(String property) {
        List<String> segments = new ArrayList<>();
        for (String segment : property.split("\\.")) {
            segments.add(NameUtils.hyphenate(segment));
        }
        return String.join(".", segments);
    }

    private static List<String> initParameterNames() {
        List<String> names = new ArrayList<>();
        for (Field field : InitParameters.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && field.getType() == String.class && !field.isSynthetic()) {
                try {
                    names.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read the Vaadin initialization parameter " + field.getName(), e);
                }
            }
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * A request forwarded to a root-mapped Vaadin servlet: Vaadin expects the path of the request as its path info.
     */
    private static final class RootMappedRequest extends HttpServletRequestWrapper {

        RootMappedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getServletPath() {
            return "";
        }

        @Override
        public String getPathInfo() {
            String path = getRequestURI().substring(getContextPath().length());
            return path.isEmpty() ? "/" : path;
        }
    }
}
