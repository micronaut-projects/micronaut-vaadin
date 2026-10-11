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
package io.micronaut.vaadin.scope;

import io.micronaut.context.scope.AbstractConcurrentCustomScope;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanIdentifier;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class of the Vaadin scopes. The beans of a scope live in the {@code UI} or {@code VaadinSession}
 * they belong to, so the scope itself holds no state.
 *
 * @param <A> The scope annotation
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
abstract class AbstractVaadinScope<A extends Annotation> extends AbstractConcurrentCustomScope<A> {

    private final ScopeHandle handle = new ScopeHandle(this);

    AbstractVaadinScope(Class<A> annotationType) {
        super(annotationType, true);
    }

    @Override
    public final boolean isRunning() {
        return true;
    }

    @Override
    public final void close() {
        // the beans are destroyed with the UI or session that holds them
    }

    /**
     * @return A serializable reference to this scope, for the listeners that the scope adds to UIs and
     * sessions, which Vaadin serializes with them
     */
    final ScopeHandle handle() {
        return handle;
    }

    /**
     * Destroys the beans of a store.
     *
     * @param store The store
     */
    final void destroy(BeanStore store) {
        destroyScope(store.beans());
    }

    /**
     * A reference to a scope that survives the serialization of a session without serializing the scope: once
     * read back, it refers to no scope, and the beans of the stores, which are not serialized either, are gone.
     */
    static final class ScopeHandle implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient @Nullable AbstractVaadinScope<?> scope;

        ScopeHandle(AbstractVaadinScope<?> scope) {
            this.scope = scope;
        }

        /**
         * Destroys the beans of a store, unless the handle was read back from a serialized session.
         *
         * @param store The store
         */
        void destroy(BeanStore store) {
            if (scope != null) {
                scope.destroy(store);
            }
        }
    }

    /**
     * The beans of one UI or session. Stored in a session, so it is serializable; the beans themselves
     * are not carried over a serialization of the session.
     */
    static class BeanStore implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private transient Map<BeanIdentifier, CreatedBean<?>> beans;

        final synchronized Map<BeanIdentifier, CreatedBean<?>> beans() {
            if (beans == null) {
                beans = new ConcurrentHashMap<>();
            }
            return beans;
        }
    }
}
