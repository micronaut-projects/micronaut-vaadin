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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Singleton;

import java.util.Optional;

/**
 * The user of the current Vaadin request, for views and the beans they use: who is signed in, with which
 * roles, and how to sign out.
 *
 * <pre>{@code
 * public AdminView(AuthenticationContext authentication) {
 *     add(new Span("Hello " + authentication.getName().orElse("")),
 *         new Button("Log out", event -> authentication.logout()));
 * }
 * }</pre>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
public class AuthenticationContext {

    private final VaadinSecurityConfiguration configuration;

    /**
     * @param configuration The configuration of the access control
     */
    public AuthenticationContext(VaadinSecurityConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return The authentication of the user of the current request, if the user is signed in
     */
    public Optional<Authentication> getAuthentication() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request != null && request.getUserPrincipal() instanceof Authentication authentication) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    /**
     * @return Whether the user of the current request is signed in
     */
    public boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    /**
     * @return The name of the signed in user
     */
    public Optional<String> getName() {
        return getAuthentication().map(Authentication::getName);
    }

    /**
     * @param role A role
     * @return Whether the signed in user has the role
     */
    public boolean hasRole(String role) {
        return getAuthentication().map(authentication -> authentication.getRoles().contains(role)).orElse(false);
    }

    /**
     * Signs the user out: the browser goes to the logout endpoint of Micronaut Security,
     * {@code vaadin.security.logout-path}, which ends the authentication and redirects.
     */
    public void logout() {
        UI.getCurrentOrThrow().getPage().setLocation(configuration.getLogoutPath());
    }
}
