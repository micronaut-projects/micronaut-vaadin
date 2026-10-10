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
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.shared.Registration;
import io.micronaut.context.scope.BeanCreationContext;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.inject.BeanIdentifier;
import io.micronaut.vaadin.annotation.RouteScope;
import io.micronaut.vaadin.annotation.RouteScopeOwner;
import jakarta.inject.Singleton;

import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements {@link RouteScope}: the beans are stored on the current {@code UI} and each belongs to a
 * navigation target or layout, its owner. A bean is destroyed when its owner leaves the navigation
 * chain, or when the UI closes.
 *
 * <p>It is also the {@link UIInitListener} that follows the navigation of every UI.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Internal
@Singleton
final class VaadinRouteScope extends AbstractVaadinScope<RouteScope> implements UIInitListener {

    VaadinRouteScope() {
        super(RouteScope.class);
    }

    @Override
    public void uiInit(UIInitEvent event) {
        UI ui = event.getUI();
        RouteBeanStore store = new RouteBeanStore();
        ComponentUtil.setData(ui, RouteBeanStore.class, store);
        Registration beforeEnter = ui.addBeforeEnterListener(store::beforeEnter);
        Registration afterNavigation = ui.addAfterNavigationListener(store::afterNavigation);
        Registration sessionDestroy = ui.getSession().addSessionDestroyListener(e -> destroy(store));
        ui.addDetachListener(e -> {
            if (ui.isClosing()) {
                beforeEnter.remove();
                afterNavigation.remove();
                sessionDestroy.remove();
                ComponentUtil.setData(ui, RouteBeanStore.class, null);
                destroy(store);
            }
        });
    }

    @Override
    protected @Nullable Map<BeanIdentifier, CreatedBean<?>> getScopeMap(boolean forCreation) {
        RouteBeanStore store = currentStore(forCreation);
        return store == null ? null : store.beans();
    }

    @Override
    protected <T> CreatedBean<T> doCreate(BeanCreationContext<T> creationContext) {
        RouteBeanStore store = currentStore(true);
        Class<?> declaredOwner = creationContext.definition().classValue(RouteScopeOwner.class).orElse(null);
        Class<?> owner;
        if (declaredOwner != null) {
            if (!store.isActive(declaredOwner)) {
                throw new IllegalStateException("Cannot create the @RouteScope bean " + creationContext.definition().getBeanType().getName()
                    + ": its owner " + declaredOwner.getName() + " is not in the navigation chain of the current UI");
            }
            owner = declaredOwner;
        } else {
            owner = store.currentTarget;
            if (owner == null) {
                throw new IllegalStateException("Cannot create the @RouteScope bean " + creationContext.definition().getBeanType().getName()
                    + ": the current UI has no navigation target. Name its owner with @RouteScopeOwner");
            }
        }
        CreatedBean<T> created = super.doCreate(creationContext);
        store.owners.put(creationContext.id(), owner);
        return created;
    }

    private static @Nullable RouteBeanStore currentStore(boolean required) {
        UI ui = UI.getCurrent();
        RouteBeanStore store = ui == null ? null : ComponentUtil.getData(ui, RouteBeanStore.class);
        if (store == null && required) {
            if (ui == null) {
                throw new IllegalStateException("No UI is bound to the current thread: a @RouteScope bean can only be resolved while Vaadin processes a request");
            }
            throw new IllegalStateException("The route scope is not available for this UI: the Vaadin service in use is not a MicronautVaadinServletService");
        }
        return store;
    }

    /**
     * The beans of one UI, with the owner of each and the current navigation chain.
     */
    static final class RouteBeanStore extends BeanStore {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient Map<BeanIdentifier, Class<?>> owners = new ConcurrentHashMap<>();
        private transient @Nullable Class<?> currentTarget;
        private transient List<? extends Class<?>> currentLayouts = List.of();

        boolean isActive(Class<?> owner) {
            return owner.equals(currentTarget) || currentLayouts.contains(owner);
        }

        void beforeEnter(BeforeEnterEvent event) {
            currentTarget = event.getNavigationTarget();
            currentLayouts = event.getLayouts();
            Set<Class<?>> active = new HashSet<>(currentLayouts);
            active.add(currentTarget);
            retain(active);
        }

        void afterNavigation(AfterNavigationEvent event) {
            Set<Class<?>> active = new HashSet<>();
            for (HasElement element : event.getActiveChain()) {
                active.add(element.getClass());
            }
            retain(active);
        }

        private void retain(Set<Class<?>> active) {
            owners.entrySet().removeIf(entry -> {
                if (active.contains(entry.getValue())) {
                    return false;
                }
                CreatedBean<?> bean = beans().remove(entry.getKey());
                if (bean != null) {
                    bean.close();
                }
                return true;
            });
        }
    }
}
