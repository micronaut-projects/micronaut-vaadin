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

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The path and query of a request, as a relative URI for the authentication fetchers.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class LocalUri {

    private static final String PATH_SEPARATOR = "/";

    private LocalUri() {
    }

    /**
     * The path and query of a request: a request URI with a scheme or an authority, such as
     * {@code //host/path}, falls back to the root.
     *
     * @param requestUri The raw request URI
     * @param query The raw query, if any
     * @return A relative URI
     */
    static String of(String requestUri, @Nullable String query) {
        try {
            URI uri = new URI(requestUri);
            String path = uri.getRawPath();
            if (uri.getScheme() == null && uri.getRawAuthority() == null && path != null && path.startsWith(PATH_SEPARATOR)) {
                return query == null ? path : path + "?" + query;
            }
        } catch (URISyntaxException _) {
            // not a valid path
        }
        return PATH_SEPARATOR;
    }
}
