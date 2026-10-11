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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Retain;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The serialized HTTP sessions that one generation hands to the next. It holds bytes only, and survives the
 * restart, as a retained bean of the development runtime.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
@Retain
@DevelopmentActive
@Requires(property = VaadinSessionCarrier.ENABLED, value = StringUtils.TRUE)
@Internal
final class CarriedSessions {

    private volatile List<SerializedSession> sessions = List.of();

    /**
     * @param captured The sessions of the stopping generation
     */
    void hold(List<SerializedSession> captured) {
        sessions = List.copyOf(captured);
    }

    /**
     * @return The sessions of the previous generation, which are no longer held
     */
    List<SerializedSession> take() {
        List<SerializedSession> taken = sessions;
        sessions = List.of();
        return taken;
    }

    /**
     * A session of a previous generation.
     *
     * @param id            The id of the session, which the browser's cookie names
     * @param creationTime  When the session was created
     * @param maxInactive   How long the session lives without a request
     * @param attributes    The serialized attributes of the session
     */
    record SerializedSession(String id, Instant creationTime, Duration maxInactive, byte[] attributes) {
    }
}
