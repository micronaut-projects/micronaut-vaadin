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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.inject.BeanIdentifier;
import io.micronaut.vaadin.annotation.UIScope;
import jakarta.inject.Singleton;

import java.io.Serial;
import java.util.Map;

/**
 * Implements {@link UIScope}: the beans are stored on the current {@code UI} and destroyed when it
 * closes or its session ends.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
@Singleton
final class VaadinUIScope extends AbstractVaadinScope<UIScope> {

    VaadinUIScope() {
        super(UIScope.class);
    }

    @Override
    protected @Nullable Map<BeanIdentifier, CreatedBean<?>> getScopeMap(boolean forCreation) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            if (forCreation) {
                throw new IllegalStateException("No UI is bound to the current thread: a @UIScope bean can only be resolved while Vaadin processes a request");
            }
            return null;
        }
        UIBeanStore store = ComponentUtil.getData(ui, UIBeanStore.class);
        if (store == null) {
            if (!forCreation) {
                return null;
            }
            store = new UIBeanStore();
            ComponentUtil.setData(ui, UIBeanStore.class, store);
            UIBeanStore created = store;
            Registration sessionDestroy = ui.getSession().addSessionDestroyListener(event -> destroy(ui, created));
            ui.addDetachListener(event -> {
                if (ui.isClosing()) {
                    sessionDestroy.remove();
                    destroy(ui, created);
                }
            });
        }
        return store.beans();
    }

    private void destroy(UI ui, UIBeanStore store) {
        ComponentUtil.setData(ui, UIBeanStore.class, null);
        destroy(store);
    }

    /**
     * The beans of one UI.
     */
    static final class UIBeanStore extends BeanStore {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
