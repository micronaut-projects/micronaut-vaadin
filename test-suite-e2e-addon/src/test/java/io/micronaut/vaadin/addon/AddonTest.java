package io.micronaut.vaadin.addon;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The frontend module of an add-on reaches the browser in development mode, although the add-on's component
 * is not in the compile-time index of the application.
 */
@MicronautTest
class AddonTest {

    @Inject
    EmbeddedServer server;

    @Test
    void theFrontendModuleOfAnAddOnIsLoaded() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            // a frontend module outside the pre-built development bundle makes Vaadin build one with Node.js
            page.setDefaultTimeout(300_000);
            PlaywrightAssertions.setDefaultAssertionTimeout(300_000);
            page.onConsoleMessage(message -> System.out.println("BROWSER " + message.type() + " " + message.text()));
            page.navigate(server.getURI().resolve("/").toString());
            assertThat(page.locator("#addon")).hasText("Hello from the add-on");
        }
    }
}
