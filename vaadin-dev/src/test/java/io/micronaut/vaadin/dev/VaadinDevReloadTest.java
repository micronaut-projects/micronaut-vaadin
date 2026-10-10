package io.micronaut.vaadin.dev;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaadin across the generations of Micronaut's development mode, on Netty.
 */
class VaadinDevReloadTest {

    @TempDir
    Path project;

    @Test
    void aRestartRegistersTheRoutesOfTheNewGeneration() {
        try (ReloadHarness harness = harness().source("example.HelloView", view("HelloView", "hello", "Hello"))) {
            // the test holds no object of the first generation past its checks
            assertTrue(routeExists(harness.start(), "hello"));
            assertFalse(routeExists(harness.context(), "goodbye"));

            harness.source("example.GoodbyeView", view("GoodbyeView", "goodbye", "Goodbye"));
            ApplicationContext second = harness.reload();

            assertEquals(2, harness.generation());
            assertTrue(routeExists(second, "hello"));
            assertTrue(routeExists(second, "goodbye"));
            // Flow's caches keyed by class must not keep the classes of a retired generation
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    @Test
    void anEditOfTheTranslationsReachesTheRunningGeneration() {
        try (ReloadHarness harness = harness()
            .source("example.HelloView", view("HelloView", "hello", "Hello"))
            .resource("vaadin-i18n/translations.properties", "greeting=Hello\n")) {
            ApplicationContext context = harness.start();
            assertEquals("Hello", translation(context, "greeting"));

            harness.resource("vaadin-i18n/translations.properties", "greeting=Bonjour\n");
            harness.reload();

            assertEquals(1, harness.generation(), "a translation needs no restart");
            assertEquals("Bonjour", translation(harness.context(), "greeting"));
        }
    }

    @Test
    void aRestartReloadsThePageQuietly() {
        try (ReloadHarness harness = harness().source("example.HelloView", view("HelloView", "hello", "Hello"))) {
            ApplicationContext context = harness.start();
            VaadinService service = service(context);
            SystemMessagesInfo info = new SystemMessagesInfo(Locale.ROOT, null, service);
            assertFalse(service.getSystemMessages(Locale.ROOT, null).isSessionExpiredNotificationEnabled());
            assertFalse(service.getSystemMessagesProvider().getSystemMessages(info).isSessionExpiredNotificationEnabled());
        }
    }

    @Test
    void aBodyEditReachesFlowsHotswapInPlace() {
        RecordingHotswapper.REDEFINED.clear();
        try (ReloadHarness harness = harness()
            .manifest("strategy", "auto")
            .source("example.HelloView", view("HelloView", "hello", "Hello"))) {
            harness.start();
            assertEquals(ReloadStrategy.AUTO, harness.runtime().strategy(), "byte-buddy-agent attaches to the test JVM");

            harness.source("example.HelloView", view("HelloView", "hello", "Bonjour"));
            harness.reload();

            assertEquals(1, harness.generation(), "a body edit is applied in place");
            assertTrue(RecordingHotswapper.REDEFINED.contains("example.HelloView"), "Flow's hotswap: " + RecordingHotswapper.REDEFINED);
        }
    }

    private ReloadHarness harness() {
        return ReloadHarness.inDirectory(project)
            .property("micronaut.server.port", "-1");
    }

    private static VaadinService service(ApplicationContext context) {
        return context.getBean(VaadinDevReloader.class).services().iterator().next();
    }

    private static boolean routeExists(ApplicationContext context, String route) {
        return ApplicationRouteRegistry.getInstance(service(context).getContext()).getNavigationTarget(route).isPresent();
    }

    private static String translation(ApplicationContext context, String key) {
        I18NProvider provider = service(context).getInstantiator().getI18NProvider();
        return provider.getTranslation(key, Locale.ROOT);
    }

    private static String view(String className, String route, String text) {
        return """
            package example;

            @com.vaadin.flow.router.Route("%s")
            public class %s extends com.vaadin.flow.component.html.Span {
                public %s() {
                    super("%s");
                }
            }
            """.formatted(route, className, className, text);
    }
}
