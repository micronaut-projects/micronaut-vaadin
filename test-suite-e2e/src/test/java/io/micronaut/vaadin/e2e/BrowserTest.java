package io.micronaut.vaadin.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FilePayload;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Vaadin in a real browser: the client loads, talks to the server, and receives pushed updates.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserTest {

    @Inject
    EmbeddedServer server;

    private static final String ATMOSPHERE_TRANSPORT = "X-Atmosphere-Transport";

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeAll
    void startBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    void stopBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        page = browser.newPage();
        // report what went wrong in the browser with the test output
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                System.out.println("BROWSER " + message.text());
            }
        });
        page.onResponse(response -> {
            if (response.status() >= 400) {
                System.out.println("BROWSER " + response.status() + " " + response.url());
            }
        });
        page.onRequestFailed(request -> System.out.println("BROWSER failed " + request.url() + " " + request.failure()));
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    private String url(String path) {
        return server.getURI().resolve(path).toString();
    }

    @Test
    void theViewRendersWithItsInjectedService() {
        page.navigate(url("/"));
        assertThat(page.locator("#greeting")).hasText("Hello from Vaadin");
    }

    @Test
    void clicksRoundTripToTheServer() {
        page.navigate(url("/"));
        page.locator("#count-button").click();
        assertThat(page.locator("#count")).hasText("1");
        page.locator("#count-button").click();
        assertThat(page.locator("#count")).hasText("2");
    }

    @Test
    void filesAreUploaded() {
        page.navigate(url("/upload"));
        page.locator("#upload input[type=file]").setInputFiles(
            new FilePayload("hello.txt", "text/plain", "Hello from a file".getBytes(StandardCharsets.UTF_8)));
        assertThat(page.locator("#result")).hasText("hello.txt: Hello from a file");
    }

    @Test
    void backgroundUpdatesArePushed() {
        page.navigate(url("/push"));
        page.locator("#start").click();
        assertThat(page.locator("#status")).hasText("pushed");
    }

    @Test
    void backgroundUpdatesArePushedOverLongPollingWithoutWebSockets() {
        // without WebSocket in the browser, Atmosphere falls back to long polling
        page.addInitScript("delete window.WebSocket");
        List<String> transports = new CopyOnWriteArrayList<>();
        page.onRequest(request -> {
            String transport = queryParameter(request.url(), ATMOSPHERE_TRANSPORT);
            if (transport != null) {
                transports.add(transport);
            }
        });
        // click once a poll waits for updates, past Atmosphere's handshake: an update pushed during the
        // handshake is a race between Vaadin and Atmosphere's client, whatever the server
        page.waitForRequest(request -> "long-polling".equals(queryParameter(request.url(), ATMOSPHERE_TRANSPORT))
                && !"0".equals(queryParameter(request.url(), "X-Atmosphere-tracking-id")),
            () -> page.navigate(url("/push")));
        page.locator("#start").click();
        assertThat(page.locator("#status")).hasText("pushed");
        assertFalse(transports.contains("websocket"), "transports: " + transports);
    }

    @Test
    void routeScopedBeansSurviveAReloadOfTheirWindow() {
        page.navigate(url("/wizard"));
        String draft = page.locator("#draft").textContent();
        page.reload();
        assertThat(page.locator("#draft")).hasText(draft);
        // leaving the route ends the scope of its beans
        page.navigate(url("/"));
        assertThat(page.locator("#greeting")).hasText("Hello from Vaadin");
        page.navigate(url("/wizard"));
        assertThat(page.locator("#draft")).not().hasText(draft);
    }

    private static @Nullable String queryParameter(String url, String name) {
        Matcher matcher = Pattern.compile("[?&]" + Pattern.quote(name) + "=([^&]*)").matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
