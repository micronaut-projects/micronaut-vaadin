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
import io.micronaut.core.annotation.AllowsReflection;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Makes a class annotated with {@code @Layout} a bean, so that layouts receive dependency injection.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
public final class LayoutAnnotationMapper implements NamedAnnotationMapper {

    @Override
    public String getName() {
        return VaadinTypeNames.LAYOUT;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        return List.of(
            AnnotationValue.builder(Bean.class).build(),
            // Vaadin reads the annotations of a view from its class: a view written in Python gets them on the
            // class that the Python compiler generates for it
            AnnotationValue.builder(AllowsReflection.class).build()
        );
    }
}
