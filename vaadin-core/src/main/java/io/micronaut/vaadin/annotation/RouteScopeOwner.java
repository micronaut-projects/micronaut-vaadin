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
package io.micronaut.vaadin.annotation;

import com.vaadin.flow.component.HasElement;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the navigation target or layout that owns a {@link RouteScope} bean. Without it, the owner is
 * the navigation target that was current when the bean was created.
 *
 * <p>It is also a qualifier: inject the bean with the same {@code @RouteScopeOwner}.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface RouteScopeOwner {

    /**
     * @return The navigation target or layout that owns the bean
     */
    Class<? extends HasElement> value();
}
