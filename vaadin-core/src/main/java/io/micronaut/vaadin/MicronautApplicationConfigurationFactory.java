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

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * Creates Vaadin's application-wide configuration with the initialization parameters of the application
 * configuration, under the {@code vaadin} prefix, as {@link MicronautVaadinServlet} does for each
 * servlet. Vaadin reads it before any servlet starts, for example to decide on development mode.
 *
 * <p>As a bean, it replaces Vaadin's {@link ApplicationConfigurationFactory} through the lookup.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
public class MicronautApplicationConfigurationFactory extends DefaultApplicationConfigurationFactory {

    private final transient Environment environment;

    /**
     * @param environment The application environment
     */
    public MicronautApplicationConfigurationFactory(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected ApplicationConfigurationImpl doCreate(VaadinContext context, Map<String, String> properties) {
        properties.putAll(VaadinInitParameters.from(environment));
        return super.doCreate(context, properties);
    }
}
