package io.micronaut.vaadin.netty;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.netty.app.ThreadView;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaadin holds the lock of a user's session while it runs application code, so that code must never
 * run on a Netty event loop: many users at once would stall the server.
 */
@MicronautTest
@Property(name = "vaadin.production-mode", value = "true")
@Property(name = "vaadin.eager-server-load", value = "true")
class EventLoopSafetyTest {

    private static final int USERS = 50;

    @Inject
    EmbeddedServer server;

    @Test
    void manyUsersAreServedConcurrentlyWithoutTheEventLoop() {
        // each client has no cookies, so each request is a new user with a new session
        URI uri = server.getURI().resolve("/thread");
        List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();
        for (int i = 0; i < USERS; i++) {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            responses.add(client.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).build(),
                HttpResponse.BodyHandlers.ofString()));
        }
        for (CompletableFuture<HttpResponse<String>> future : responses) {
            HttpResponse<String> response = future.join();
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Created on"), response.body());
        }
        assertTrue(ThreadView.EVENT_LOOP_THREADS.isEmpty(), () -> "Vaadin ran on event loops: " + ThreadView.EVENT_LOOP_THREADS);
    }
}
