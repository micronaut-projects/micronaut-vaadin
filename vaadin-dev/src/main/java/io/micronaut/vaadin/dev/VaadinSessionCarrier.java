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
package io.micronaut.vaadin.dev;

import com.vaadin.flow.server.VaadinSession;
import io.micronaut.context.BeanContext;
import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.reload.ClassChangeEvent;
import io.micronaut.context.reload.ReloadStrategy;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.session.InMemorySession;
import io.micronaut.session.InMemorySessionStore;
import io.micronaut.session.Session;
import io.micronaut.session.event.AbstractSessionEvent;
import io.micronaut.session.event.SessionCreatedEvent;
import io.micronaut.session.event.SessionDeletedEvent;
import io.micronaut.session.event.SessionDestroyedEvent;
import io.micronaut.session.event.SessionExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carries the HTTP sessions of the Netty runtime, Vaadin's sessions with them, over a restart of the
 * development runtime, when Vaadin's own development mode session serialization is enabled with
 * {@code vaadin.devmode.session-serialization.enabled}:
 * <ul>
 *     <li>the stopping generation serializes the attributes of every session, the {@code VaadinSession}
 *     under its lock;</li>
 *     <li>the next generation reads them with its own classes, and stores the sessions under their ids, so
 *     that the browser keeps its session, and Vaadin its UIs.</li>
 * </ul>
 * A session whose state is not serializable, such as a view that holds a bean, is not carried over: the
 * browser then reloads the page.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Context
@DevelopmentActive
@Requires(property = VaadinSessionCarrier.ENABLED, value = StringUtils.TRUE)
@Requires(beans = InMemorySessionStore.class)
@Internal
final class VaadinSessionCarrier implements ApplicationEventListener<AbstractSessionEvent> {

    /**
     * Vaadin's development mode session serialization, which the carrier follows.
     */
    static final String ENABLED = "vaadin.devmode.session-serialization.enabled";

    private static final Logger LOG = LoggerFactory.getLogger(VaadinSessionCarrier.class);

    private final InMemorySessionStore sessionStore;
    private final CarriedSessions carried;
    private final ClassLoader classLoader;
    private final Set<String> sessionIds = ConcurrentHashMap.newKeySet();

    VaadinSessionCarrier(BeanContext beanContext, InMemorySessionStore sessionStore, CarriedSessions carried) {
        this.sessionStore = sessionStore;
        this.carried = carried;
        this.classLoader = beanContext.getClassLoader();
        restore();
        if (beanContext instanceof WatchableBeanContext watchable) {
            watchable.classChanges().watch(this::classesChanged);
        }
    }

    @Override
    public void onApplicationEvent(AbstractSessionEvent event) {
        String id = event.getSource().getId();
        if (event instanceof SessionCreatedEvent) {
            sessionIds.add(id);
        } else if (event instanceof SessionDeletedEvent || event instanceof SessionExpiredEvent || event instanceof SessionDestroyedEvent) {
            sessionIds.remove(id);
        }
    }

    /**
     * @param change Classes the development runtime changed
     */
    void classesChanged(ClassChangeEvent change) {
        if (change.strategy() == ReloadStrategy.RESTART) {
            // the event reaches the stopping generation: its sessions go to the next one
            carried.hold(capture());
        }
    }

    private List<CarriedSessions.SerializedSession> capture() {
        List<CarriedSessions.SerializedSession> captured = new ArrayList<>();
        for (String id : sessionIds) {
            Optional<InMemorySession> session = sessionStore.findSession(id).join();
            if (session.isEmpty() || session.get().isExpired()) {
                continue;
            }
            try {
                captured.add(new CarriedSessions.SerializedSession(id, session.get().getCreationTime(),
                    session.get().getMaxInactiveInterval(), serialize(id, session.get())));
            } catch (IOException e) {
                LOG.info("The session {} is not carried over the restart: {}", id, e.toString());
            }
        }
        return captured;
    }

    /**
     * Serializes the attributes of a session one by one, so that an attribute that is not serializable is
     * left behind alone.
     */
    private static byte[] serialize(String id, Session session) throws IOException {
        Map<String, byte[]> attributes = new LinkedHashMap<>();
        for (String name : session.names()) {
            Object value = session.get(name).orElse(null);
            if (value == null) {
                continue;
            }
            try {
                attributes.put(name, serialize(value));
            } catch (IOException e) {
                LOG.info("The attribute {} of the session {} is not carried over the restart, as it is not serializable: {}", name, id, e.toString());
            }
        }
        return serialize(attributes);
    }

    private static byte[] serialize(Object value) throws IOException {
        VaadinSession vaadinSession = value instanceof VaadinSession vs ? vs : null;
        if (vaadinSession != null) {
            vaadinSession.lock();
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(value);
            }
            return bytes.toByteArray();
        } finally {
            if (vaadinSession != null) {
                vaadinSession.unlock();
            }
        }
    }

    private void restore() {
        for (CarriedSessions.SerializedSession serialized : carried.take()) {
            try {
                Map<?, ?> attributes = (Map<?, ?>) deserialize(serialized.attributes());
                CarriedSession session = new CarriedSession(serialized.id(), serialized.creationTime(), serialized.maxInactive());
                for (Map.Entry<?, ?> attribute : attributes.entrySet()) {
                    session.put((String) attribute.getKey(), deserialize((byte[]) attribute.getValue()));
                }
                sessionStore.save(session).join();
                sessionIds.add(session.getId());
            } catch (IOException | ClassNotFoundException | RuntimeException e) {
                LOG.info("The session {} is not carried over the restart, as the new classes cannot read it: {}", serialized.id(), e.toString());
            }
        }
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new GenerationObjectInputStream(new ByteArrayInputStream(bytes), classLoader)) {
            return in.readObject();
        }
    }

    /**
     * Reads objects with the classes of the running generation.
     */
    private static final class GenerationObjectInputStream extends ObjectInputStream {

        private final ClassLoader classLoader;

        GenerationObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), false, classLoader);
            } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }
    }

    /**
     * A session restored under its id.
     */
    private static final class CarriedSession extends InMemorySession {

        CarriedSession(String id, Instant creationTime, Duration maxInactive) {
            super(id, creationTime, maxInactive);
            setNew(false);
        }
    }
}
