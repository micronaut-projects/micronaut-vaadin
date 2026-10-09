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

import jakarta.inject.Scope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scopes a bean to the navigation target that owns it: the instance lives while that target, or the layout named by {@link RouteScopeOwner}, stays in the navigation chain of the UI.
 *
 * <p>The bean can only be resolved while a Vaadin request is being processed, in other words when
 * {@code UI.getCurrent()} is set.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RouteScope {
}
