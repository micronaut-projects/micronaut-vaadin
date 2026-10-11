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
package io.micronaut.vaadin.security;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.session.Session;
import io.micronaut.session.SessionStore;
import io.micronaut.session.http.HttpSessionFilter;
import io.micronaut.session.http.HttpSessionIdResolver;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

/**
 * Finds the Micronaut session of a Vaadin request on a servlet container, as Micronaut Session's filter does
 * for Micronaut's own routes, so that authentication kept in the session, with
 * {@code micronaut.security.authentication=session}, reaches Vaadin's requests too.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
@Requires(classes = {SessionStore.class, VaadinAuthenticationFilter.class})
@Requires(beans = SessionStore.class)
final class VaadinSessionResolver {

    private final SessionStore<Session> sessionStore;
    private final List<HttpSessionIdResolver> idResolvers;

    VaadinSessionResolver(SessionStore<Session> sessionStore, List<HttpSessionIdResolver> idResolvers) {
        this.sessionStore = sessionStore;
        this.idResolvers = idResolvers;
    }

    /**
     * Adds the session of the request, when it has one that has not expired, to its attributes.
     *
     * @param request The request, as the authentication fetchers see it
     */
    void attachSession(MutableHttpRequest<?> request) {
        for (HttpSessionIdResolver resolver : idResolvers) {
            for (String id : resolver.resolveIds(request)) {
                Optional<Session> session = sessionStore.findSession(id).join();
                if (session.isPresent() && !session.get().isExpired()) {
                    request.setAttribute(HttpSessionFilter.SESSION_ATTRIBUTE, session.get());
                    return;
                }
            }
        }
    }
}
