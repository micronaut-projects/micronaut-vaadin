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
package io.micronaut.vaadin.startup;

import com.vaadin.base.devserver.startup.DevModeStartupListener;
import io.micronaut.core.annotation.Internal;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Set;

/**
 * Starts Vaadin's development mode, when {@code com.vaadin:vaadin-dev-server} is on the classpath. Only
 * loaded then, so that the rest of the integration does not depend on it.
 *
 * <p>Vaadin itself skips development mode in production mode.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
final class DevModeStartup {

    static final String DEV_MODE_STARTUP_LISTENER = "com.vaadin.base.devserver.startup.DevModeStartupListener";

    private DevModeStartup() {
    }

    /**
     * @param types          The Vaadin types of the application: the entry points from which Vaadin finds the
     *                       frontend dependencies of the application
     * @param servletContext The servlet context
     * @throws ServletException If development mode fails to start
     */
    static void initialize(Set<Class<?>> types, ServletContext servletContext) throws ServletException {
        DevModeStartupListener listener = new DevModeStartupListener();
        listener.process(types, servletContext);
        // stops the development server when the servlet context is destroyed
        servletContext.addListener(listener);
    }
}
