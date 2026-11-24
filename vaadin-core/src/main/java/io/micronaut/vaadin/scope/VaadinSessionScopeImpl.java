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

import com.vaadin.flow.server.VaadinSession;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.inject.BeanIdentifier;
import io.micronaut.vaadin.annotation.VaadinSessionScope;
import jakarta.inject.Singleton;

import java.io.Serial;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Implements {@link VaadinSessionScope}: the beans are stored in the current {@code VaadinSession} and
 * destroyed when it ends.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
@Singleton
final class VaadinSessionScopeImpl extends AbstractVaadinScope<VaadinSessionScope> {

    VaadinSessionScopeImpl() {
        super(VaadinSessionScope.class);
    }

    @Override
    protected @Nullable Map<BeanIdentifier, CreatedBean<?>> getScopeMap(boolean forCreation) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            if (forCreation) {
                throw new IllegalStateException("No VaadinSession is bound to the current thread: a @VaadinSessionScope bean can only be resolved while Vaadin processes a request");
            }
            return null;
        }
        Lock lock = session.getLockInstance();
        lock.lock();
        try {
            SessionBeanStore store = session.getAttribute(SessionBeanStore.class);
            if (store == null) {
                if (!forCreation) {
                    return null;
                }
                store = new SessionBeanStore();
                session.setAttribute(SessionBeanStore.class, store);
                SessionBeanStore created = store;
                session.addSessionDestroyListener(event -> destroy(created));
            }
            return store.beans();
        } finally {
            lock.unlock();
        }
    }

    /**
     * The beans of one session.
     */
    static final class SessionBeanStore extends BeanStore {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
