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
package io.micronaut.vaadin.dev;

import com.vaadin.base.devserver.hotswap.Hotswapper;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.micronaut.context.BeanContext;
import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.context.reload.ClassChange;
import io.micronaut.context.reload.ClassChangeEvent;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.context.watch.ResourceChange;
import io.micronaut.context.reload.ResourceKind;
import io.micronaut.core.annotation.Internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Hands the changes that Micronaut's development mode applies in place to Flow's hotswap, which clears
 * Flow's caches of the changed classes, and refreshes the UIs that use them:
 * <ul>
 *     <li>an edit of method bodies, which the development runtime redefines in place
 *     ({@link ReloadStrategy#RELOAD});</li>
 *     <li>an edit of the translations under {@code vaadin-i18n}, which needs no restart.</li>
 * </ul>
 * Any other change restarts the application, with a new generation of its classes: Vaadin starts again from
 * the type index of the new generation. Flow's process-wide caches let the classes of the old generation go.

 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Context
@DevelopmentActive
@Requires(classes = Hotswapper.class)
@Internal
final class VaadinDevReloader implements VaadinServiceInitListener {

    /**
     * The translations of Vaadin's default {@code I18NProvider}, under the resources of the application.
     */
    static final String TRANSLATIONS = "vaadin-i18n/**";

    private final Set<VaadinService> services = new CopyOnWriteArraySet<>();

    VaadinDevReloader(BeanContext beanContext) {
        // a new generation: what a draining generation put in Flow's caches since the restart goes
        FlowCaches.release(beanContext.getClassLoader());
        if (beanContext instanceof WatchableBeanContext watchable) {
            watchable.classChanges().watch(this::classesChanged);
            watchable.resources(ResourceKind.CONFIG).include(TRANSLATIONS).watch(this::translationsChanged);
        }
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        if (Hotswapper.getRegistered(service).isEmpty() && Hotswapper.register(service).isEmpty()) {
            // Vaadin runs in production mode
            return;
        }
        services.add(service);
        service.addServiceDestroyListener(destroyed -> services.remove(service));
    }

    /**
     * @return The Vaadin services in development mode
     */
    Set<VaadinService> services() {
        return services;
    }

    /**
     * @param change Classes the development runtime changed
     */
    void classesChanged(ClassChangeEvent change) {
        if (change.strategy() != ReloadStrategy.RELOAD) {
            // a restart: the next generation starts Vaadin again, and Flow's caches let this one go
            FlowCaches.release(change.newLoader());
            return;
        }
        String[] classNames = change.changes().stream().map(ClassChange::className).toArray(String[]::new);
        // Flow resolves the names with the context class loader: that of the running generation, not the
        // development runtime's own, which only sees the libraries
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(change.newLoader());
        try {
            for (VaadinService service : services) {
                Hotswapper.getRegistered(service).ifPresent(hotswapper -> hotswapper.onHotswap(classNames, true));
            }
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * @param change Translation files that changed
     */
    void translationsChanged(ResourceChange change) {
        if (change.initial() || change.isEmpty()) {
            return;
        }
        URI[] modified = uris(change.changed());
        URI[] deleted = uris(change.removed());
        for (VaadinService service : services) {
            Hotswapper.getRegistered(service).ifPresent(hotswapper -> hotswapper.onHotswap(new URI[0], modified, deleted));
        }
    }

    private static URI[] uris(List<Path> paths) {
        return paths.stream().map(Path::toUri).toArray(URI[]::new);
    }
}
