package io.micronaut.vaadin.netty;

import io.micronaut.session.InMemorySession;
import io.micronaut.session.Session;
import io.micronaut.session.SessionStore;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A session store for the tests of the servlet facade, without a context.
 */
final class TestSessionStore implements SessionStore<Session> {

    final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session newSession() {
        return new TestSession(UUID.randomUUID().toString());
    }

    @Override
    public CompletableFuture<Optional<Session>> findSession(String id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(sessions.get(id)));
    }

    @Override
    public CompletableFuture<Boolean> deleteSession(String id) {
        return CompletableFuture.completedFuture(sessions.remove(id) != null);
    }

    @Override
    public CompletableFuture<Session> save(Session session) {
        sessions.put(session.getId(), session);
        return CompletableFuture.completedFuture(session);
    }

    private static final class TestSession extends InMemorySession {
        TestSession(String id) {
            super(id, Duration.ofMinutes(30));
        }
    }
}
