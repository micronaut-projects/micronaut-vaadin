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
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import io.micronaut.context.scope.BeanCreationContext;
import io.micronaut.context.scope.CreatedBean;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanIdentifier;
import io.micronaut.vaadin.annotation.RouteScope;
import io.micronaut.vaadin.annotation.RouteScopeOwner;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements {@link RouteScope}: each bean belongs to a navigation target or layout, its owner, and is
 * destroyed when its owner leaves the navigation chain.
 *
 * <p>The beans of a browser window are kept in the {@code VaadinSession}, under the name of the window, as
 * the Spring integration of Vaadin does: when the page is reloaded, the new UI of the window takes the
 * beans of the old one over, as long as their owners stay in its navigation chain. The beans of a closed
 * window go with the session. A UI without a window name keeps its beans to itself, and they are destroyed
 * when it closes.</p>
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
        Navigation navigation = new Navigation();
        ComponentUtil.setData(ui, Navigation.class, navigation);
        // the listeners refer to the scope through its handle: Vaadin serializes them with the session
        ScopeHandle handle = handle();
        Registration beforeEnter = ui.addBeforeEnterListener(e -> navigation.beforeEnter(e, store(handle, ui)));
        Registration afterNavigation = ui.addAfterNavigationListener(e -> navigation.afterNavigation(e, store(handle, ui)));
        ui.addDetachListener(e -> {
            if (ui.isClosing()) {
                beforeEnter.remove();
                afterNavigation.remove();
                ComponentUtil.setData(ui, Navigation.class, null);
                close(handle, ui);
            }
        });
    }

    @Override
    protected @Nullable Map<BeanIdentifier, CreatedBean<?>> getScopeMap(boolean forCreation) {
        UI ui = currentUI(forCreation);
        if (ui == null) {
            return null;
        }
        RouteBeanStore store = forCreation ? store(handle(), ui) : existingStore(ui);
        return store == null ? null : store.beans();
    }

    @Override
    protected <T> CreatedBean<T> doCreate(BeanCreationContext<T> creationContext) {
        UI ui = currentUI(true);
        Navigation navigation = ComponentUtil.getData(ui, Navigation.class);
        if (navigation == null) {
            throw new IllegalStateException("The route scope is not available for this UI: the Vaadin service in use is not a MicronautVaadinServletService");
        }
        Class<?> declaredOwner = creationContext.definition().classValue(RouteScopeOwner.class).orElse(null);
        Class<?> owner;
        if (declaredOwner != null) {
            if (!navigation.isActive(declaredOwner)) {
                throw new IllegalStateException("Cannot create the @RouteScope bean " + creationContext.definition().getBeanType().getName()
                    + ": its owner " + declaredOwner.getName() + " is not in the navigation chain of the current UI");
            }
            owner = declaredOwner;
        } else {
            owner = navigation.currentTarget;
            if (owner == null) {
                throw new IllegalStateException("Cannot create the @RouteScope bean " + creationContext.definition().getBeanType().getName()
                    + ": the current UI has no navigation target. Name its owner with @RouteScopeOwner");
            }
        }
        CreatedBean<T> created = super.doCreate(creationContext);
        store(handle(), ui).owners.put(creationContext.id(), owner);
        return created;
    }

    /**
     * @return The store of the window of the UI, created if needed, now held by the UI
     */
    private static RouteBeanStore store(ScopeHandle handle, UI ui) {
        RouteStores stores = stores(handle, ui.getSession());
        String key = key(ui);
        RouteBeanStore store = stores.byKey().get(key);
        if (store == null) {
            // the store of the UI from before its window name was known
            store = stores.byKey().remove(uiKey(ui));
            if (store == null) {
                store = new RouteBeanStore();
            }
            stores.byKey().put(key, store);
        }
        // a reload: the new UI of the window takes the beans over
        store.ui = ui;
        return store;
    }

    private static @Nullable RouteBeanStore existingStore(UI ui) {
        RouteStores stores = ui.getSession().getAttribute(RouteStores.class);
        return stores == null ? null : stores.byKey().get(key(ui));
    }

    private static RouteStores stores(ScopeHandle handle, VaadinSession session) {
        RouteStores stores = session.getAttribute(RouteStores.class);
        if (stores == null) {
            RouteStores created = new RouteStores();
            session.setAttribute(RouteStores.class, created);
            session.addSessionDestroyListener(e -> created.byKey().values().forEach(handle::destroy));
            stores = created;
        }
        return stores;
    }

    /**
     * Releases the beans of a closing UI. Vaadin closes the UI of a page that unloads, before the UI that
     * reloads the page exists: the beans of a named window are therefore kept for its next UI, which drops
     * those whose owners it does not navigate to, and go with the session otherwise. The beans of a UI
     * without a window name are destroyed.
     */
    private static void close(ScopeHandle handle, UI ui) {
        VaadinSession session = ui.getSession();
        RouteStores stores = session == null ? null : session.getAttribute(RouteStores.class);
        if (stores == null) {
            return;
        }
        String key = key(ui);
        RouteBeanStore store = stores.byKey().get(key);
        if (store == null || store.ui != ui) {
            // a newer UI of the window holds the beans
            return;
        }
        store.ui = null;
        if (key.equals(uiKey(ui))) {
            stores.byKey().remove(key);
            handle.destroy(store);
        }
    }

    private static String key(UI ui) {
        ExtendedClientDetails details = ui.getInternals().getExtendedClientDetails();
        String windowName = details == null ? null : details.getWindowName();
        return windowName == null ? uiKey(ui) : "window:" + windowName;
    }

    private static String uiKey(UI ui) {
        return "ui:" + ui.getUIId();
    }

    private static @Nullable UI currentUI(boolean required) {
        UI ui = UI.getCurrent();
        if (ui == null && required) {
            throw new IllegalStateException("No UI is bound to the current thread: a @RouteScope bean can only be resolved while Vaadin processes a request");
        }
        return ui;
    }

    /**
     * The navigation chain of one UI.
     */
    static final class Navigation implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private transient @Nullable Class<?> currentTarget;
        private transient List<? extends Class<?>> currentLayouts = List.of();

        boolean isActive(Class<?> owner) {
            return owner.equals(currentTarget) || currentLayouts.contains(owner);
        }

        void beforeEnter(BeforeEnterEvent event, RouteBeanStore store) {
            currentTarget = event.getNavigationTarget();
            currentLayouts = event.getLayouts();
            Set<Class<?>> active = new HashSet<>(currentLayouts);
            active.add(currentTarget);
            store.retain(active);
        }

        void afterNavigation(AfterNavigationEvent event, RouteBeanStore store) {
            Set<Class<?>> active = new HashSet<>();
            for (HasElement element : event.getActiveChain()) {
                active.add(element.getClass());
            }
            store.retain(active);
        }
    }

    /**
     * The stores of the windows of a session, by window name, or by UI for a UI without one.
     */
    static final class RouteStores implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private transient @Nullable Map<String, RouteBeanStore> byKey;

        synchronized Map<String, RouteBeanStore> byKey() {
            if (byKey == null) {
                byKey = new ConcurrentHashMap<>();
            }
            return byKey;
        }
    }

    /**
     * The beans of one window, with the owner of each, and the UI that holds them.
     */
    static final class RouteBeanStore extends BeanStore {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient Map<BeanIdentifier, Class<?>> owners = new ConcurrentHashMap<>();
        private transient @Nullable UI ui;

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
