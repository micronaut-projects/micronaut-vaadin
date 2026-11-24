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

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.InitParameters;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.value.PropertyResolver;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads Vaadin's initialization parameters from the application configuration, under the {@code vaadin}
 * prefix: {@code vaadin.production-mode} sets {@code productionMode}, and {@code vaadin.productionMode}
 * works too.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
public final class VaadinInitParameters {

    private static final List<String> NAMES = initParameterNames();

    private VaadinInitParameters() {
    }

    /**
     * @param propertyResolver The application configuration
     * @return The Vaadin initialization parameters that the configuration sets, with the defaults of the integration
     */
    public static Map<String, String> from(PropertyResolver propertyResolver) {
        Map<String, String> parameters = new LinkedHashMap<>();
        for (String name : NAMES) {
            configuredValue(propertyResolver, name).ifPresent(value -> parameters.put(name, value));
        }
        // Development mode finds the frontend dependencies of the application from the types it is given, which
        // here are those of the compile-time index: the views, layouts and other entry points of the application,
        // not the components of the add-ons on the class path. It follows the classes they use, as a production
        // build does, rather than expect every class annotated with @JsModule or @NpmPackage
        parameters.putIfAbsent(InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE, "true");
        return parameters;
    }

    private static Optional<String> configuredValue(PropertyResolver propertyResolver, String initParameter) {
        String property = VaadinConfigurationProperties.PREFIX + "." + initParameter;
        Optional<String> value = propertyResolver.getProperty(hyphenate(property), String.class);
        return value.isPresent() ? value : propertyResolver.getProperty(property, String.class);
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
        // where development mode finds the frontend sources and writes generated files
        names.add(FrontendUtils.PROJECT_BASEDIR);
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
}
