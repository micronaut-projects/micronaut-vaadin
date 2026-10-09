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

import io.micronaut.context.annotation.Bean;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

/**
 * Makes the concrete classes that Vaadin instantiates through its {@code Instantiator} into beans:
 * error views, the application shell and web component exporters. Views and layouts are handled by
 * {@link RouteAnnotationMapper} and {@link LayoutAnnotationMapper}.
 *
 * <p>The bean definitions double as the index of Vaadin types that replaces the classpath scanning of
 * a servlet container.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
public final class VaadinTypeVisitor implements TypeElementVisitor<Object, Object> {

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (element.isAbstract() || element.isInterface() || element.isEnum() || element.isRecord()
            || element.hasStereotype(Bean.class)) {
            return;
        }
        for (String type : VaadinTypeNames.INSTANTIATED_INTERFACES) {
            if (element.isAssignable(type)) {
                // a class needs an annotation to become a bean on its own: the visitor creates its definition instead
                element.addAssociatedBean(element);
                return;
            }
        }
    }
}
