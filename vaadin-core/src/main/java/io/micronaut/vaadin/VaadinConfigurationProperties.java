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
package io.micronaut.vaadin;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of the Vaadin integration, read from the {@code vaadin} prefix.
 *
 * <p>The property names match those of Vaadin's Spring Boot integration, so that the Vaadin
 * documentation applies unchanged.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@ConfigurationProperties(VaadinConfigurationProperties.PREFIX)
public class VaadinConfigurationProperties {

    /**
     * The configuration prefix.
     */
    public static final String PREFIX = "vaadin";

    /**
     * The default URL mapping of the Vaadin servlet.
     */
    public static final String DEFAULT_URL_MAPPING = "/*";

    /**
     * Whether the Vaadin servlet supports asynchronous requests by default.
     */
    public static final boolean DEFAULT_ASYNC_SUPPORTED = true;

    /**
     * Whether the Vaadin servlet is loaded on startup by default.
     */
    public static final boolean DEFAULT_LOAD_ON_STARTUP = true;

    private String urlMapping = DEFAULT_URL_MAPPING;
    private boolean asyncSupported = DEFAULT_ASYNC_SUPPORTED;
    private boolean loadOnStartup = DEFAULT_LOAD_ON_STARTUP;
    private List<String> allowedPackages = new ArrayList<>();
    private List<String> blockedPackages = new ArrayList<>();
    private List<String> excludeUrls = new ArrayList<>();

    /**
     * @return The URL mapping of the Vaadin servlet
     */
    public @NonNull String getUrlMapping() {
        return urlMapping;
    }

    /**
     * The URL mapping of the Vaadin servlet. Default value: {@value #DEFAULT_URL_MAPPING}.
     *
     * @param urlMapping The URL mapping
     */
    public void setUrlMapping(@NonNull String urlMapping) {
        this.urlMapping = urlMapping;
    }

    /**
     * @return Whether the Vaadin servlet supports asynchronous requests
     */
    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    /**
     * Whether the Vaadin servlet supports asynchronous requests, which push needs. Default value: {@value #DEFAULT_ASYNC_SUPPORTED}.
     *
     * @param asyncSupported Whether asynchronous requests are supported
     */
    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    /**
     * @return Whether the Vaadin servlet is loaded on startup
     */
    public boolean isLoadOnStartup() {
        return loadOnStartup;
    }

    /**
     * Whether the Vaadin servlet is loaded on startup instead of on the first request. Default value: {@value #DEFAULT_LOAD_ON_STARTUP}.
     *
     * @param loadOnStartup Whether to load on startup
     */
    public void setLoadOnStartup(boolean loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    /**
     * @return The packages searched for Vaadin types
     */
    public @NonNull List<String> getAllowedPackages() {
        return allowedPackages;
    }

    /**
     * The packages searched for Vaadin types such as routes. Empty means all packages.
     *
     * @param allowedPackages The allowed packages
     */
    public void setAllowedPackages(@NonNull List<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }

    /**
     * @return The packages never searched for Vaadin types
     */
    public @NonNull List<String> getBlockedPackages() {
        return blockedPackages;
    }

    /**
     * The packages never searched for Vaadin types.
     *
     * @param blockedPackages The blocked packages
     */
    public void setBlockedPackages(@NonNull List<String> blockedPackages) {
        this.blockedPackages = blockedPackages;
    }

    /**
     * @return The URL patterns that Vaadin does not handle
     */
    public @NonNull List<String> getExcludeUrls() {
        return excludeUrls;
    }

    /**
     * The URL patterns that Vaadin does not handle when it is mapped to the root, so that other routes and static resources keep working.
     *
     * @param excludeUrls The excluded URL patterns
     */
    public void setExcludeUrls(@NonNull List<String> excludeUrls) {
        this.excludeUrls = excludeUrls;
    }
}
