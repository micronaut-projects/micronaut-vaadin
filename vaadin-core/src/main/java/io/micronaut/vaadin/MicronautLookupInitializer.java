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
package io.micronaut.vaadin;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.server.VaadinContext;
import io.micronaut.context.BeanContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Initializes Vaadin's {@link Lookup} so that beans provide Vaadin's services: a bean that implements a
 * service type, for example {@code com.vaadin.flow.di.ResourceProvider}, replaces Vaadin's own
 * implementation. Services without a bean are found as without Micronaut.
 *
 * <p>The bean context is read from the {@link VaadinContext}, where {@link #setBeanContext} stores it
 * before Vaadin starts.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MicronautLookupInitializer extends LookupInitializer {

    /**
     * Stores the bean context in the Vaadin context, for the lookup to use.
     *
     * @param context     The Vaadin context
     * @param beanContext The bean context
     */
    public static void setBeanContext(VaadinContext context, BeanContext beanContext) {
        context.setAttribute(BeanContextAttribute.class, new BeanContextAttribute(beanContext));
    }

    @Override
    protected Lookup createLookup(VaadinContext context, Map<Class<?>, Collection<Class<?>>> services) {
        BeanContextAttribute attribute = context.getAttribute(BeanContextAttribute.class);
        if (attribute == null) {
            return super.createLookup(context, services);
        }
        BeanContext beanContext = attribute.beanContext();
        return new MicronautLookup(beanContext, services, (service, implementation) -> instantiate(beanContext, service, implementation));
    }

    private Object instantiate(BeanContext beanContext, Class<?> service, Class<?> implementation) {
        // a service implementation that is also a bean is provided by the bean context
        for (Object bean : beanContext.getBeansOfType(service)) {
            if (implementation.isInstance(bean)) {
                return null;
            }
        }
        return instantiate(service, implementation);
    }

    /**
     * The bean context, as stored in the Vaadin context.
     *
     * @param beanContext The bean context
     */
    private record BeanContextAttribute(BeanContext beanContext) {
    }

    /**
     * A lookup that prefers beans to the services Vaadin finds itself.
     */
    private static final class MicronautLookup extends LookupImpl {

        private final BeanContext beanContext;

        MicronautLookup(BeanContext beanContext,
                        Map<Class<?>, Collection<Class<?>>> services,
                        BiFunction<Class<?>, Class<?>, Object> factory) {
            super(services, factory);
            this.beanContext = beanContext;
        }

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            Collection<T> beans = beanContext.getBeansOfType(serviceClass);
            T service = super.lookup(serviceClass);
            if (beans.isEmpty()) {
                return service;
            }
            // a bean replaces Vaadin's default implementation of a service
            if (beans.size() == 1 && (service == null || service.getClass().getName().startsWith("com.vaadin.flow"))) {
                return beans.iterator().next();
            }
            List<Object> found = new ArrayList<>(beans);
            if (service != null) {
                found.add(service);
            }
            throw new IllegalStateException(SEVERAL_IMPLS + serviceClass + SPI + found + ONE_IMPL_REQUIRED);
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            List<T> all = new ArrayList<>(beanContext.getBeansOfType(serviceClass));
            all.addAll(super.lookupAll(serviceClass));
            return all;
        }
    }
}
