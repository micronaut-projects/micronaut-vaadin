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
package io.micronaut.vaadin.graal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.startup.ClassLoaderAwareServletContainerInitializer;
import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.util.List;

/**
 * Registers the classes that Vaadin uses through reflection in a native image. Vaadin reads the
 * annotations, constructors, fields and methods of components and of their events at runtime, so every
 * subclass of {@link Component} and {@link ComponentEvent} that the native image reaches is registered,
 * in the application and in Vaadin's own jars, as the Spring integration of Vaadin does at build time.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
public final class VaadinFeature implements Feature {

    private static final List<Class<?>> REFLECTIVE_SUPERTYPES = List.of(
        Component.class,
        ComponentEvent.class,
        HasErrorParameter.class,
        AppShellConfigurator.class,
        WebComponentExporter.class,
        VaadinServiceInitListener.class
    );

    /**
     * Classes of Vaadin that Flow serializes to JSON, or reads, by reflection: the same as the Spring
     * integration of Vaadin registers.
     */
    private static final List<String> REFLECTIVE_CLASSES = List.of(
        "com.vaadin.flow.shared.ui.Dependency",
        "com.vaadin.flow.server.DefaultErrorHandler",
        "com.vaadin.flow.server.InitParameters",
        "com.vaadin.flow.server.menu.MenuData",
        "com.vaadin.flow.server.menu.AvailableViewInfo",
        "com.vaadin.flow.server.menu.AvailableViewInfo$DetailDeserializer",
        "com.vaadin.flow.server.menu.AvailableViewInfo$DetailSerializer",
        "com.vaadin.flow.server.menu.RouteParamType",
        "com.vaadin.flow.component.messages.MessageListItem",
        "tools.jackson.databind.ser.std.ToStringSerializer"
    );

    private static final String COMPONENT_PACKAGE = "com.vaadin.flow.component";

    private static final String ATMOSPHERE_PACKAGE = "org.atmosphere";
    private static final String ATMOSPHERE_FRAMEWORK = ATMOSPHERE_PACKAGE + ".cpr.AtmosphereFramework";

    @Override
    public String getDescription() {
        return "Registers the classes that Vaadin uses through reflection";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (Class<?> type : LookupInitializer.getDefaultImplementations()) {
            registerAll(type);
        }
        for (Class<?> supertype : REFLECTIVE_SUPERTYPES) {
            access.registerSubtypeReachabilityHandler((duringAnalysis, type) -> registerAll(type), supertype);
        }
        for (String className : REFLECTIVE_CLASSES) {
            Class<?> type = access.findClassByName(className);
            if (type != null) {
                registerAll(type);
            }
        }
        // the translations of components, and the configurations of charts and maps, go to the browser as JSON
        JarClasses.forEachOnClassPath(access, COMPONENT_PACKAGE, VaadinFeature::isSerializedComponentModel,
            VaadinFeature::registerAll);
        // the startup reads the @HandlesTypes of Vaadin's initializers to give each one its types
        access.registerSubtypeReachabilityHandler((duringAnalysis, type) -> RuntimeReflection.register(type),
            ClassLoaderAwareServletContainerInitializer.class);
        // Atmosphere, which carries push, creates its broadcasters, interceptors and factories by name
        JarClasses.forEach(access, ATMOSPHERE_FRAMEWORK, ATMOSPHERE_PACKAGE, name -> true, VaadinFeature::registerConstructors);
    }

    /**
     * The translations of components are in classes named after {@code I18n}, or {@code I18N} for the upload,
     * with their inner classes.
     */
    private static boolean isSerializedComponentModel(String className) {
        return className.matches(".*I18[nN](\\$.*)?")
            || className.startsWith(COMPONENT_PACKAGE + ".charts.model.")
            || className.startsWith(COMPONENT_PACKAGE + ".map.configuration.");
    }

    private static void registerConstructors(Class<?> type) {
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.register(type.getDeclaredConstructors());
        } catch (LinkageError e) {
            // a class for a container or a library that is not on the class path
        }
    }

    private static void registerAll(Class<?> type) {
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.registerAllDeclaredConstructors(type);
            RuntimeReflection.registerAllDeclaredMethods(type);
            RuntimeReflection.registerAllDeclaredFields(type);
            RuntimeReflection.register(type.getDeclaredConstructors());
            RuntimeReflection.register(type.getDeclaredMethods());
            RuntimeReflection.register(type.getDeclaredFields());
        } catch (LinkageError e) {
            // a class that refers to a library that is not on the class path
        }
    }
}
