package io.micronaut.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletService;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.NonUniqueBeanException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.startup.app.HomeView;
import io.micronaut.vaadin.startup.app.TestInitListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaadin's instantiator backed by the bean context.
 */
@MicronautTest(startApplication = false)
@Property(name = "spec.name", value = "MicronautInstantiatorTest")
class MicronautInstantiatorTest {

    @Inject
    BeanContext beanContext;

    private MicronautInstantiator instantiator() {
        return new MicronautInstantiator(new VaadinServletService() {
        }, beanContext);
    }

    @Test
    void beansComeFromTheBeanContext() {
        MicronautInstantiator instantiator = instantiator();
        assertSame(beanContext.getBean(Translations.class), instantiator.getOrCreate(Translations.class));
        // a class with several beans is created anew; an interface with several cannot be
        assertNotNull(instantiator.getOrCreate(Animal.class));
        assertThrows(NonUniqueBeanException.class, () -> instantiator.getOrCreate(Named.class));
    }

    @Test
    void viewsThatAreBeansAreCreatedByTheBeanContext() {
        MicronautInstantiator instantiator = instantiator();
        HomeView first = instantiator.createComponent(HomeView.class);
        assertNotSame(first, instantiator.createComponent(HomeView.class));
        assertInstanceOf(Div.class, instantiator.createComponent(Div.class));
    }

    @Test
    void theServicesOfVaadinAreBeansWhenTheApplicationDefinesThem() {
        MicronautInstantiator instantiator = instantiator();
        assertSame(beanContext.getBean(Translations.class), instantiator.getI18NProvider());
        assertEquals(HomeView.class, instantiator.getApplicationClass(HomeView.class));
    }

    @Test
    void theInitListenersIncludeTheBeans() {
        Set<Class<?>> listeners = instantiator().getServiceInitListeners()
            .map(VaadinServiceInitListener::getClass)
            .collect(Collectors.toSet());
        assertTrue(listeners.contains(TestInitListener.class), listeners.toString());
    }

    /**
     * The translations of the application.
     */
    @Singleton
    @Requires(property = "spec.name", value = "MicronautInstantiatorTest")
    static class Translations implements I18NProvider {
        @Override
        public List<Locale> getProvidedLocales() {
            return List.of(Locale.ENGLISH);
        }

        @Override
        public String getTranslation(String key, Locale locale, Object... params) {
            return key;
        }
    }

    interface Named {
    }

    @Singleton
    @Requires(property = "spec.name", value = "MicronautInstantiatorTest")
    static class Animal implements Named {
    }

    @Singleton
    @Requires(property = "spec.name", value = "MicronautInstantiatorTest")
    static class Dog extends Animal {
    }
}
