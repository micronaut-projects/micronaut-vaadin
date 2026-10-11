/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.vaadin.netty;

import io.micronaut.session.Session;
import io.micronaut.session.SessionStore;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;

/**
 * An HTTP session of the servlet API over a session of Micronaut Session, which keeps it in its store and
 * tracks it with its cookie.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyHttpSession implements HttpSession {

    private final Session session;
    private final SessionStore<Session> sessionStore;
    private final ServletContext servletContext;

    NettyHttpSession(Session session, SessionStore<Session> sessionStore, ServletContext servletContext) {
        this.session = session;
        this.sessionStore = sessionStore;
        this.servletContext = servletContext;
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime().toEpochMilli();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime().toEpochMilli();
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        session.setMaxInactiveInterval(Duration.ofSeconds(interval));
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) session.getMaxInactiveInterval().toSeconds();
    }

    @Override
    public @Nullable Object getAttribute(String name) {
        return session.get(name).orElse(null);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(session.names());
    }

    @Override
    public void setAttribute(String name, @Nullable Object value) {
        if (value == null) {
            session.remove(name);
        } else {
            session.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        session.remove(name);
    }

    @Override
    public void invalidate() {
        sessionStore.deleteSession(session.getId());
    }

    @Override
    public boolean isNew() {
        return session.isNew();
    }
}
