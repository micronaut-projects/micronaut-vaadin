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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;
import org.jspecify.annotations.Nullable;

/**
 * Configuration of the access control of Vaadin views, under {@code vaadin.security}.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@ConfigurationProperties(VaadinSecurityConfiguration.PREFIX)
public class VaadinSecurityConfiguration implements Toggleable {

    /**
     * The configuration prefix.
     */
    public static final String PREFIX = "vaadin.security";

    /**
     * Whether the access control of Vaadin views is enabled by default.
     */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * The default path of the logout endpoint of Micronaut Security.
     */
    public static final String DEFAULT_LOGOUT_PATH = "/logout";

    private boolean enabled = DEFAULT_ENABLED;
    private @Nullable String loginView;
    private String logoutPath = DEFAULT_LOGOUT_PATH;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether Vaadin checks the access to its views. Default value: {@value #DEFAULT_ENABLED}.
     *
     * @param enabled Whether the access control is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return The path of the login view
     */
    public @Nullable String getLoginView() {
        return loginView;
    }

    /**
     * The path of the view, or of another page, where an anonymous user who navigates to a view that needs
     * authentication is taken. Without it, the user gets an access denied error.
     *
     * @param loginView The path of the login view, such as {@code /login}
     */
    public void setLoginView(@Nullable String loginView) {
        this.loginView = loginView;
    }

    /**
     * @return The path the browser goes to when the user signs out
     */
    public String getLogoutPath() {
        return logoutPath;
    }

    /**
     * The path the browser goes to when the user signs out with {@link AuthenticationContext#logout()}: the
     * logout endpoint of Micronaut Security. Default value: {@value #DEFAULT_LOGOUT_PATH}.
     *
     * @param logoutPath The logout path
     */
    public void setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
    }
}
