package io.micronaut.vaadin.startup;

import com.vaadin.browserless.mocks.MockContext;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.startup.app.HomeView;
import io.micronaut.vaadin.startup.app.NotFoundView;
import io.micronaut.vaadin.startup.app.Shell;
import io.micronaut.vaadin.startup.app.TestInitListener;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class VaadinStartupTest {

    @Inject
    VaadinStartup startup;

    private VaadinServletContext context;

    @BeforeEach
    void start() throws Exception {
        MockContext servletContext = new MockContext();
        startup.initialize(servletContext);
        context = new VaadinServletContext(servletContext);
    }

    @Test
    void registersTheViews() {
        ApplicationRouteRegistry registry = ApplicationRouteRegistry.getInstance(context);
        Set<String> paths = registry.getRegisteredRoutes().stream().map(RouteData::getTemplate).collect(Collectors.toSet());
        assertEquals(Set.of("", "reports"), paths);
        assertEquals(HomeView.class, registry.getNavigationTarget("home").orElseThrow());
    }

    @Test
    void registersTheErrorViews() {
        ApplicationRouteRegistry registry = ApplicationRouteRegistry.getInstance(context);
        assertEquals(NotFoundView.class, registry.getErrorNavigationTarget(new NotFoundException()).orElseThrow().getNavigationTarget());
    }

    @Test
    void registersTheApplicationShell() {
        assertEquals(Shell.class, AppShellRegistry.getInstance(context).getShell());
    }

    @Test
    void theLookupProvidesBeans() {
        Lookup lookup = context.getAttribute(Lookup.class);
        assertTrue(lookup.lookupAll(VaadinServiceInitListener.class).stream().anyMatch(TestInitListener.class::isInstance));
    }
}
