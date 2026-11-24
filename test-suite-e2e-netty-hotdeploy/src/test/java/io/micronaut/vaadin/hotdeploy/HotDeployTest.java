package io.micronaut.vaadin.hotdeploy;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vite's hot module replacement on Netty: an edit of a stylesheet reaches the open page, through Vite's WebSocket,
 * which the runtime proxies. Vite updates the page in place or reloads it, as Vaadin decides for the kind of file.
 */
@MicronautTest
class HotDeployTest {

    @Inject
    EmbeddedServer server;

    @Test
    void anEditOfAStylesheetReachesTheOpenPage() throws Exception {
        Path stylesheet = Path.of(System.getProperty("vaadin.project.basedir"), "src/main/frontend/hotdeploy.css");
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            // the first start installs the frontend dependencies and starts Vite
            page.setDefaultTimeout(300_000);
            PlaywrightAssertions.setDefaultAssertionTimeout(300_000);
            List<String> console = new CopyOnWriteArrayList<>();
            page.onConsoleMessage(message -> console.add(message.text()));
            List<String> webSockets = new CopyOnWriteArrayList<>();
            page.onWebSocket(webSocket -> webSockets.add(webSocket.url()));
            page.navigate(server.getURI().resolve("/").toString());
            assertThat(page.locator("#styled")).hasCSS("color", "rgb(200, 0, 0)");
            // the browser's WebSocket to Vite goes through the runtime's proxy, on the port of the application: without
            // the proxy, Vite's client falls back to Vite's own port, which only a browser on the same machine reaches
            assertTrue(console.contains("[vite] connected."), "console: " + console);
            String proxy = "ws://localhost:" + server.getPort() + "/VAADIN/";
            assertTrue(webSockets.stream().anyMatch(url -> url.startsWith(proxy)), "WebSockets: " + webSockets);
            assertTrue(webSockets.stream().allMatch(url -> url.startsWith("ws://localhost:" + server.getPort() + "/")), "WebSockets: " + webSockets);

            Files.writeString(stylesheet, Files.readString(stylesheet).replace("rgb(200, 0, 0)", "rgb(0, 0, 200)"));

            assertThat(page.locator("#styled")).hasCSS("color", Pattern.compile("rgb\\(0, 0, 200\\)"));
        }
    }
}
