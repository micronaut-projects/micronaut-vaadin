package io.micronaut.vaadin.startup;

import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.startup.app.HomeView;
import io.micronaut.vaadin.startup.app.NotFoundView;
import io.micronaut.vaadin.startup.app.ReportsView;
import io.micronaut.vaadin.startup.app.Shell;
import io.micronaut.vaadin.startup.app.TestInitListener;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class VaadinTypeIndexTest {

    @Inject
    VaadinTypeIndex index;

    @Inject
    BeanContext beanContext;

    @Test
    void viewsAreIndexed() {
        assertEquals(Set.of(HomeView.class, ReportsView.class), index.getRoutes());
    }

    @Test
    void classesImplementingVaadinInterfacesAreIndexed() {
        assertEquals(Set.of(NotFoundView.class), index.getErrorViews());
        assertTrue(index.getTypes().contains(Shell.class));
        // the error view has no annotation, but is a bean: its constructor is injected
        assertNotNull(beanContext.createBean(NotFoundView.class));
    }

    @Test
    void developmentModeAlsoReceivesTheInitListeners() {
        Set<Class<?>> developmentTypes = index.getDevelopmentTypes();
        // every type of the index, and the listeners development mode needs to know of
        assertTrue(developmentTypes.containsAll(index.getTypes()));
        assertTrue(developmentTypes.contains(TestInitListener.class));
        assertFalse(index.getTypes().contains(TestInitListener.class));
    }
}
