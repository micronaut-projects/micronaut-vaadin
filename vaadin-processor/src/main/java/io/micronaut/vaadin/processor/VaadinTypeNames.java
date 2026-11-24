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
package io.micronaut.vaadin.processor;

import io.micronaut.core.annotation.Internal;

import java.util.List;

/**
 * Names of the Vaadin types that the processor turns into beans. The processor does not depend on
 * Vaadin, so the types are referenced by name.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
final class VaadinTypeNames {

    static final String ROUTE = "com.vaadin.flow.router.Route";
    static final String ROUTE_ALIAS = "com.vaadin.flow.router.RouteAlias";
    static final String LAYOUT = "com.vaadin.flow.router.Layout";

    /**
     * Interfaces whose concrete implementations Vaadin instantiates: error views, the application
     * shell and web component exporters.
     */
    static final List<String> INSTANTIATED_INTERFACES = List.of(
        "com.vaadin.flow.router.HasErrorParameter",
        "com.vaadin.flow.component.page.AppShellConfigurator",
        "com.vaadin.flow.component.WebComponentExporter",
        "com.vaadin.flow.component.WebComponentExporterFactory"
    );

    private VaadinTypeNames() {
    }
}
