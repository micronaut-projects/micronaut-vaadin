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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.server.Server;

/**
 * Prepares the Jetty context of Micronaut Servlet for Vaadin. It enables HTTP sessions, which the context is
 * created without and Vaadin keeps its session in, and gives the context the class loader of the
 * application, which Vaadin loads resources and classes with.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
@Requires(classes = {Server.class, ServletContextHandler.class})
final class JettyContextCustomizer implements BeanCreatedEventListener<Server> {

    @Override
    public Server onCreated(BeanCreatedEvent<Server> event) {
        Server server = event.getBean();
        for (ServletContextHandler context : server.getDescendants(ServletContextHandler.class)) {
            if (context.getSessionHandler() == null) {
                context.setSessionHandler(new SessionHandler());
            }
            if (context.getClassLoader() == null) {
                context.setClassLoader(JettyContextCustomizer.class.getClassLoader());
            }
        }
        return server;
    }
}
