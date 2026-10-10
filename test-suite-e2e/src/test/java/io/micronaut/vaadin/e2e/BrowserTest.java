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

import java.nio.charset.StandardCharsets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Vaadin in a real browser: the client loads, talks to the server, and receives pushed updates.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserTest {

    @Inject
    EmbeddedServer server;

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
}
