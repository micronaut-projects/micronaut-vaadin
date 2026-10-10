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

import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.DefaultAccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * The access control of Vaadin views: by default, the views are checked against their annotations,
 * {@code @AnonymousAllowed}, {@code @PermitAll}, {@code @RolesAllowed} and {@code @DenyAll}, and a view
 * without one is denied. Define a {@link NavigationAccessControl} bean to replace it, or
 * {@link NavigationAccessChecker} beans to add checks.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Factory
@Requires(property = VaadinSecurityConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
public class VaadinSecurityFactory {

    /**
     * @return Checks the access annotations of a view
     */
    @Singleton
    @Requires(missingBeans = AccessAnnotationChecker.class)
    AccessAnnotationChecker accessAnnotationChecker() {
        return new AccessAnnotationChecker();
    }

    /**
     * @param accessAnnotationChecker Checks the access annotations of a view
     * @return Checks the access to views with their annotations
     */
    @Singleton
    AnnotatedViewAccessChecker annotatedViewAccessChecker(AccessAnnotationChecker accessAnnotationChecker) {
        return new AnnotatedViewAccessChecker(accessAnnotationChecker);
    }

    /**
     * @param checkers      The access checkers
     * @param configuration The configuration of the access control
     * @return The access control of the views
     */
    @Singleton
    @Requires(missingBeans = NavigationAccessControl.class)
    NavigationAccessControl navigationAccessControl(List<NavigationAccessChecker> checkers,
                                                    VaadinSecurityConfiguration configuration) {
        MicronautNavigationAccessControl accessControl = new MicronautNavigationAccessControl(checkers, new DefaultAccessCheckDecisionResolver());
        String loginView = configuration.getLoginView();
        if (loginView != null) {
            accessControl.setLoginView(loginView);
        }
        return accessControl;
    }

    /**
     * @param accessControl The access control of the views
     * @return Checks every navigation of every UI
     */
    @Singleton
    VaadinServiceInitListener navigationAccessControlInitializer(NavigationAccessControl accessControl) {
        return event -> event.getSource().addUIInitListener(uiEvent -> uiEvent.getUI().addBeforeEnterListener(accessControl));
    }
}
