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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.MenuAccessControl;
import io.micronaut.context.BeanContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.context.exceptions.NonUniqueBeanException;
import io.micronaut.core.type.Argument;

import java.io.Serial;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Creates the views, layouts and other components of Vaadin with the Micronaut bean context, so they
 * receive dependency injection.
 *
 * <p>A class with a bean definition (a view, a layout, an error view, or any other bean) is resolved
 * from the bean context. Any other class is instantiated by Vaadin, as without Micronaut.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MicronautInstantiator extends DefaultInstantiator {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String INTERCEPTED = "io.micronaut.aop.Intercepted";

    private final transient BeanContext beanContext;

    /**
     * @param service     The Vaadin service
     * @param beanContext The bean context
     */
    public MicronautInstantiator(VaadinService service, BeanContext beanContext) {
        super(service);
        this.beanContext = beanContext;
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        @SuppressWarnings("unchecked")
        ApplicationEventPublisher<ServiceInitEvent> publisher = beanContext.getBean(Argument.of(ApplicationEventPublisher.class, ServiceInitEvent.class));
        // makes ServiceInitEvent observable with @EventListener
        VaadinServiceInitListener publishing = publisher::publishEvent;
        return Stream.concat(
            super.getServiceInitListeners(),
            Stream.concat(Stream.of(publishing), beanContext.getBeansOfType(VaadinServiceInitListener.class).stream())
        );
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        try {
            Optional<T> bean = beanContext.findBean(type);
            if (bean.isPresent()) {
                return bean.get();
            }
        } catch (NonUniqueBeanException e) {
            if (beanContext.containsBean(type) && !type.isInterface()) {
                return beanContext.createBean(type);
            }
            throw e;
        }
        return super.getOrCreate(type);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        if (beanContext.findBeanDefinition(componentClass).isPresent()) {
            return beanContext.createBean(componentClass);
        }
        return super.createComponent(componentClass);
    }

    @Override
    public I18NProvider getI18NProvider() {
        return findUnique(I18NProvider.class).orElseGet(super::getI18NProvider);
    }

    @Override
    public MenuAccessControl getMenuAccessControl() {
        return findUnique(MenuAccessControl.class).orElseGet(super::getMenuAccessControl);
    }

    @Override
    public PageTitleGenerator getPageTitleGenerator() {
        return findUnique(PageTitleGenerator.class).orElseGet(super::getPageTitleGenerator);
    }

    @Override
    public Class<?> getApplicationClass(Class<?> clazz) {
        Class<?> type = super.getApplicationClass(clazz);
        while (type.getSuperclass() != null && isIntercepted(type)) {
            type = type.getSuperclass();
        }
        return type;
    }

    private <T> Optional<T> findUnique(Class<T> type) {
        try {
            return beanContext.findBean(type);
        } catch (NonUniqueBeanException e) {
            return Optional.empty();
        }
    }

    private static boolean isIntercepted(Class<?> type) {
        for (Class<?> i : type.getInterfaces()) {
            if (INTERCEPTED.equals(i.getName())) {
                return true;
            }
        }
        return false;
    }
}
