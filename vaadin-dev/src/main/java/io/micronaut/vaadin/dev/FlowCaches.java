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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.internal.ReflectionCache;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

/**
 * Releases the classes of retired generations from Flow's process-wide caches. Flow lives in the parent
 * tier of the development runtime, so its static caches would otherwise keep every generation of the
 * application's views reachable.
 *
 * <p>Flow does not clear its map of components by tag: see
 * <a href="https://github.com/vaadin/flow/issues/26324">vaadin/flow#26324</a>.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class FlowCaches {

    private static final Logger LOG = LoggerFactory.getLogger(FlowCaches.class);

    private FlowCaches() {
    }

    /**
     * Clears Flow's reflection caches, which fill again on use, and drops the component classes that the
     * given class loader cannot see from Flow's map of components by tag, which is never cleared.
     *
     * @param current The class loader of the running generation
     */
    static void release(ClassLoader current) {
        ReflectionCache.clearAll();
        clearComponentTracker();
        // the map is unmodifiable, its sets are Flow's own
        for (Set<Class<? extends Component>> components : ComponentUtil.getAllTagMappings().values()) {
            components.removeIf(component -> !visibleFrom(component.getClassLoader(), current));
        }
    }

    /**
     * Forgets where the components were created and attached. Flow's component tracker keeps, for its
     * development tools, a stack trace per component in weak maps: the trace of a collected component, which
     * names the classes of its generation, stays until the map is next used. At a restart, the tracked
     * components are those of the retired generation.
     */
    private static void clearComponentTracker() {
        for (Field field : ComponentTracker.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(null) instanceof Map<?, ?> map) {
                        map.clear();
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOG.debug("Cannot clear the component tracker of Flow: {}", e.toString());
                }
            }
        }
    }

    private static boolean visibleFrom(@Nullable ClassLoader loader, ClassLoader current) {
        if (loader == null) {
            return true;
        }
        for (ClassLoader candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (candidate == loader) {
                return true;
            }
        }
        return false;
    }
}
