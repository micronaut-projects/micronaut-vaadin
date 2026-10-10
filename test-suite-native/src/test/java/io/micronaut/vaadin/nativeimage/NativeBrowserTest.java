package io.micronaut.vaadin.nativeimage;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FilePayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application as a native executable, in production mode, driven by a browser.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NativeBrowserTest {

    private Process application;
    private int port;
    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeAll
    void start() throws Exception {
        Path executable = Path.of(System.getProperty("native.executable"));
        assertTrue(Files.isExecutable(executable), "No native executable at " + executable);
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        application = new ProcessBuilder(executable.toString(), "-Dmicronaut.server.port=" + port)
            .inheritIO()
            .start();
        Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
        while (!listening()) {
            assertTrue(application.isAlive(), "The native executable exited with " + (application.isAlive() ? "" : application.exitValue()));
            assertTrue(Instant.now().isBefore(deadline), "The native executable did not start");
            Thread.sleep(100);
        }
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    void stop() {
        if (browser != null) {
            browser.close();
            playwright.close();
        }
        if (application != null) {
            application.destroy();
        }
    }

    @BeforeEach
    void openPage() {
        page = browser.newPage();
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
    }

    @AfterEach
    void closePage() {
        page.close();
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
    }

    @Test
    void backgroundUpdatesArePushed() {
        page.navigate(url("/push"));
        page.locator("#start").click();
        assertThat(page.locator("#status")).hasText("pushed");
    }

    @Test
    void filesAreUploaded() {
        page.navigate(url("/upload"));
        page.locator("#upload input[type=file]").setInputFiles(
            new FilePayload("hello.txt", "text/plain", "Hello from a file".getBytes(StandardCharsets.UTF_8)));
        assertThat(page.locator("#result")).hasText("hello.txt: Hello from a file");
    }

    private boolean listening() {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
