package io.micronaut.vaadin.e2e.security;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Signing in with Vaadin's login form, posted to Micronaut Security, and signing out.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginTest {

    @Inject
    EmbeddedServer server;

    private Playwright playwright;
    private Browser browser;

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

    @Test
    void signInAndOut() {
        Page page = browser.newContext().newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                System.out.println("BROWSER " + message.text());
            }
        });

        page.navigate(url("/admin"));
        // anonymous: taken to the login view
        assertThat(page).hasURL(url("/login"));

        page.locator("vaadin-login-form input[name=username]").fill("admin");
        page.locator("vaadin-login-form input[name=password]").fill("secret");
        page.locator("vaadin-login-form vaadin-button[slot=submit]").click();

        assertThat(page.locator("#greeting")).hasText("Admin area for admin");

        page.locator("#logout").click();
        assertThat(page.locator("#home")).hasText("Welcome");

        page.navigate(url("/admin"));
        assertThat(page).hasURL(url("/login"));
    }

    @Test
    void wrongCredentialsShowAnError() {
        Page page = browser.newContext().newPage();
        page.navigate(url("/login"));
        page.locator("vaadin-login-form input[name=username]").fill("admin");
        page.locator("vaadin-login-form input[name=password]").fill("wrong");
        page.locator("vaadin-login-form vaadin-button[slot=submit]").click();
        assertThat(page).hasURL(url("/login?error"));
        assertThat(page.getByText("Incorrect username or password")).isVisible();
    }

    private String url(String path) {
        return server.getURI().resolve(path).toString();
    }
}
