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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.micronaut.context.BeanContext;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Vaadin types of the application, known at compile time. The Micronaut Vaadin processor makes every
 * view, layout, error view, application shell and web component exporter a bean, so their bean
 * definitions replace the classpath scanning of a servlet container.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
public class VaadinTypeIndex {

    private static final Class<?>[] INSTANTIATED_TYPES = {
        HasErrorParameter.class,
        AppShellConfigurator.class,
        WebComponentExporter.class,
        WebComponentExporterFactory.class
    };

    private final BeanContext beanContext;

    /**
     * @param beanContext The bean context
     */
    public VaadinTypeIndex(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * @return The views: the classes annotated with {@code @Route}
     */
    public Set<Class<? extends Component>> getRoutes() {
        return componentsWith(Route.class);
    }

    /**
     * @return The automatic layouts: the classes annotated with {@code @Layout}
     */
    public Set<Class<? extends Component>> getLayouts() {
        return componentsWith(Layout.class);
    }

    /**
     * @return The error views: the classes implementing {@code HasErrorParameter}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Set<Class<? extends HasErrorParameter<?>>> getErrorViews() {
        Set result = typesOf(HasErrorParameter.class);
        return result;
    }

    /**
     * @return Every Vaadin type of the application
     */
    public Set<Class<?>> getTypes() {
        Set<Class<?>> types = new LinkedHashSet<>();
        types.addAll(getRoutes());
        types.addAll(getLayouts());
        for (Class<?> type : INSTANTIATED_TYPES) {
            types.addAll(typesOf(type));
        }
        return Collections.unmodifiableSet(types);
    }

    /**
     * The types from which Vaadin's development mode finds the frontend dependencies of the application:
     * every Vaadin type, and the {@code UIInitListener} and {@code VaadinServiceInitListener} beans.
     *
     * @return The entry points of the application for development mode
     */
    public Set<Class<?>> getDevelopmentTypes() {
        Set<Class<?>> types = new LinkedHashSet<>(getTypes());
        types.addAll(typesOf(UIInitListener.class));
        types.addAll(typesOf(VaadinServiceInitListener.class));
        return Collections.unmodifiableSet(types);
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends Component>> componentsWith(Class<? extends java.lang.annotation.Annotation> annotation) {
        Set<Class<? extends Component>> types = new LinkedHashSet<>();
        for (BeanDefinition<?> definition : beanContext.getBeanDefinitions(Qualifiers.byStereotype(annotation))) {
            Class<?> type = definition.getBeanType();
            if (Component.class.isAssignableFrom(type)) {
                types.add((Class<? extends Component>) type);
            }
        }
        return Collections.unmodifiableSet(types);
    }

    private Set<Class<?>> typesOf(Class<?> type) {
        Set<Class<?>> types = new LinkedHashSet<>();
        for (BeanDefinition<?> definition : beanContext.getBeanDefinitions(type)) {
            types.add(definition.getBeanType());
        }
        return Collections.unmodifiableSet(types);
    }
}
