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

import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.AnnotationValidator;
import com.vaadin.flow.server.startup.ClassLoaderAwareServletContainerInitializer;
import com.vaadin.flow.server.startup.ErrorNavigationTargetInitializer;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;
import com.vaadin.flow.server.startup.RouteRegistryInitializer;
import com.vaadin.flow.server.startup.VaadinAppShellInitializer;
import com.vaadin.flow.server.startup.WebComponentConfigurationRegistryInitializer;
import com.vaadin.flow.server.startup.WebComponentExporterAwareValidator;
import io.micronaut.context.BeanContext;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.vaadin.MicronautLookupInitializer;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Starts Vaadin in a servlet context. It runs Vaadin's servlet container initializers with the types of
 * the {@link VaadinTypeIndex}, in place of the classpath scanning that a servlet container does for
 * their {@code @HandlesTypes}.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
public class VaadinStartup {

    /**
     * Implementations of Vaadin's lookup services that live in Vaadin's own jars and are not registered
     * with the Java service loader. They are used when present.
     */
    private static final List<String> LIBRARY_SERVICES = List.of(
        "com.vaadin.base.devserver.DevModeHandlerManagerImpl",
        "com.vaadin.base.devserver.BrowserLiveReloadAccessorImpl"
    );

    private final BeanContext beanContext;
    private final VaadinTypeIndex typeIndex;

    /**
     * @param beanContext The bean context
     * @param typeIndex   The Vaadin types of the application
     */
    public VaadinStartup(BeanContext beanContext, VaadinTypeIndex typeIndex) {
        this.beanContext = beanContext;
        this.typeIndex = typeIndex;
    }

    /**
     * Initializes Vaadin in the given servlet context: its lookup, route registry, error views,
     * application shell and web components, and its development mode when
     * {@code com.vaadin:vaadin-dev-server} is on the classpath.
     *
     * @param servletContext The servlet context
     * @throws ServletException If Vaadin fails to initialize
     */
    public void initialize(ServletContext servletContext) throws ServletException {
        MicronautLookupInitializer.setBeanContext(new VaadinServletContext(servletContext), beanContext);

        Set<Class<?>> lookupTypes = new LinkedHashSet<>();
        lookupTypes.add(LookupInitializer.class);
        lookupTypes.add(MicronautLookupInitializer.class);
        ClassLoader classLoader = getClass().getClassLoader();
        for (String name : LIBRARY_SERVICES) {
            ClassUtils.forName(name, classLoader).ifPresent(lookupTypes::add);
        }
        new LookupServletContainerInitializer().process(lookupTypes, servletContext);

        Set<Class<?>> types = typeIndex.getTypes();
        List<ClassLoaderAwareServletContainerInitializer> initializers = List.of(
            new RouteRegistryInitializer(),
            new ErrorNavigationTargetInitializer(),
            new VaadinAppShellInitializer(),
            new WebComponentConfigurationRegistryInitializer(),
            new AnnotationValidator(),
            new WebComponentExporterAwareValidator()
        );
        for (ClassLoaderAwareServletContainerInitializer initializer : initializers) {
            // the lookup is in place, so the initializers run straight away rather than through onStartup,
            // which also expects the servlet context to have a class loader
            initializer.process(handledTypes(initializer, types), servletContext);
        }

        if (ClassUtils.isPresent(DevModeStartup.DEV_MODE_STARTUP_LISTENER, classLoader)) {
            DevModeStartup.initialize(typeIndex.getDevelopmentTypes(), servletContext);
        }
    }

    /**
     * Selects the types an initializer handles, as a servlet container does with {@code @HandlesTypes}:
     * the classes annotated with one of the annotations, or extending or implementing one of the types.
     */
    @SuppressWarnings("unchecked")
    private static Set<Class<?>> handledTypes(ClassLoaderAwareServletContainerInitializer initializer, Set<Class<?>> types) {
        HandlesTypes handlesTypes = initializer.getClass().getAnnotation(HandlesTypes.class);
        Set<Class<?>> handled = new LinkedHashSet<>();
        if (handlesTypes == null) {
            return handled;
        }
        for (Class<?> type : types) {
            for (Class<?> handledType : handlesTypes.value()) {
                boolean matches = handledType.isAnnotation()
                    ? type.isAnnotationPresent((Class<? extends Annotation>) handledType)
                    : handledType.isAssignableFrom(type) && !handledType.equals(type);
                if (matches) {
                    handled.add(type);
                    break;
                }
            }
        }
        return handled;
    }
}
