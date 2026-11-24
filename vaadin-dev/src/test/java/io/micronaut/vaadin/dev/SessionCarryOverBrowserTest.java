package io.micronaut.vaadin.dev;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Vaadin UI in a browser across a restart of the development runtime.
 */
class SessionCarryOverBrowserTest {

    private static final String COUNTER_VIEW = """
        package example;

        import com.vaadin.flow.component.button.Button;
        import com.vaadin.flow.component.html.Span;
        import com.vaadin.flow.component.orderedlayout.VerticalLayout;
        import com.vaadin.flow.router.Route;

        @Route("")
        public class CounterView extends VerticalLayout {
            private int count;

            public CounterView() {
                Span value = new Span("0");
                value.setId("count");
                Button increment = new Button("Increment", event -> value.setText(String.valueOf(++count)));
                increment.setId("increment");
                add(increment, value);
            }
        }
        """;

    @TempDir
    Path project;

    @Test
    void theUiOfASerializableViewSurvivesARestart() {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)
                // a fixed port, which the development runtime keeps across a restart
                .property("micronaut.server.port", String.valueOf(freePort()))
                .property("vaadin.devmode.session-serialization.enabled", "true")
                .source("example.CounterView", COUNTER_VIEW);
             Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            harness.start();
            Page page = browser.newPage();
            page.onConsoleMessage(message -> System.out.println("BROWSER " + message.type() + " " + message.text()));
            page.navigate("http://localhost:" + harness.context().getBean(EmbeddedServer.class).getPort() + "/");
            page.locator("#increment").click();
            assertThat(page.locator("#count")).hasText("1");

            harness.source("example.Other", "package example; public class Other { }");
            harness.reload();
            assertEquals(2, harness.generation());

            // the same UI, served by the new generation: the count goes on
            page.locator("#increment").click();
            assertThat(page.locator("#count")).hasText("2");
            // the carried sessions hold the classes of the new generation only
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
