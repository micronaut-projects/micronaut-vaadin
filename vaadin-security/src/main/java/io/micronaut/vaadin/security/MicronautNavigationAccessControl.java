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

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import io.micronaut.security.authentication.Authentication;
import org.jspecify.annotations.Nullable;

import java.security.Principal;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Vaadin's access control of views with the authentication of Micronaut Security: the user is the
 * principal of the request, which Micronaut Security authenticates, and the roles are the roles of its
 * {@link Authentication}.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MicronautNavigationAccessControl extends NavigationAccessControl {

    /**
     * @param checkers         The access checkers
     * @param decisionResolver Decides from the results of the checkers
     */
    public MicronautNavigationAccessControl(Collection<NavigationAccessChecker> checkers,
                                            AccessCheckDecisionResolver decisionResolver) {
        super(checkers, decisionResolver);
    }

    @Override
    protected @Nullable Principal getPrincipal(@Nullable VaadinRequest request) {
        return request == null ? null : request.getUserPrincipal();
    }

    @Override
    protected Predicate<String> getRolesChecker(@Nullable VaadinRequest request) {
        Principal principal = getPrincipal(request);
        if (principal instanceof Authentication authentication) {
            Collection<String> roles = authentication.getRoles();
            return roles::contains;
        }
        return role -> request != null && request.isUserInRole(role);
    }
}
