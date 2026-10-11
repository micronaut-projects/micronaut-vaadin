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
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.simple.SimpleHttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.vaadin.servlet.VaadinServletFilter;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Authenticates the requests of Vaadin on a servlet container with the authentication fetchers of
 * Micronaut Security, which only run for Micronaut's own routes there, and makes the
 * {@link Authentication} the principal of the request, with its roles.
 *
 * <p>The fetchers see the method, URI, headers and cookies of the request, which suits stateless
 * authentication: HTTP Basic, or a token in a cookie or a header.</p>
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
@Requires(classes = VaadinServletFilter.class)
@Requires(beans = VaadinSecurityConfiguration.class)
final class VaadinAuthenticationFilter implements VaadinServletFilter {

    private final List<AuthenticationFetcher<HttpRequest<?>>> fetchers;

    VaadinAuthenticationFilter(List<AuthenticationFetcher<HttpRequest<?>>> fetchers) {
        this.fetchers = fetchers;
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = authenticate(request);
        chain.doFilter(authentication == null ? request : new AuthenticatedRequest(request, authentication), response);
    }

    private @Nullable Authentication authenticate(HttpServletRequest request) {
        if (fetchers.isEmpty()) {
            return null;
        }
        HttpRequest<?> micronautRequest = toMicronautRequest(request);
        for (AuthenticationFetcher<HttpRequest<?>> fetcher : fetchers) {
            Authentication authentication = first(fetcher.fetchAuthentication(micronautRequest));
            if (authentication != null) {
                return authentication;
            }
        }
        return null;
    }

    /**
     * Waits for the first value of a publisher: the request runs on a thread of the servlet container,
     * which may block.
     */
    private static <T> @Nullable T first(Publisher<T> publisher) {
        CompletableFuture<@Nullable T> result = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<T>() {
            private @Nullable Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(T value) {
                result.complete(value);
                if (subscription != null) {
                    subscription.cancel();
                }
            }

            @Override
            public void onError(Throwable error) {
                result.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                result.complete(null);
            }
        });
        return result.join();
    }

    private static HttpRequest<?> toMicronautRequest(HttpServletRequest request) {
        String uri = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        MutableHttpRequest<Object> micronautRequest = new SimpleHttpRequest<>(HttpMethod.parse(request.getMethod()), uri, null);
        for (String name : Collections.list(request.getHeaderNames())) {
            for (String value : Collections.list(request.getHeaders(name))) {
                micronautRequest.getHeaders().add(name, value);
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                micronautRequest.cookie(io.micronaut.http.cookie.Cookie.of(cookie.getName(), cookie.getValue()));
            }
        }
        return micronautRequest;
    }

    /**
     * A request whose principal is the authentication of Micronaut Security.
     */
    private static final class AuthenticatedRequest extends HttpServletRequestWrapper {

        private final Authentication authentication;

        AuthenticatedRequest(HttpServletRequest request, Authentication authentication) {
            super(request);
            this.authentication = authentication;
        }

        @Override
        public Principal getUserPrincipal() {
            return authentication;
        }

        @Override
        public String getRemoteUser() {
            return authentication.getName();
        }

        @Override
        public boolean isUserInRole(String role) {
            return authentication.getRoles().contains(role);
        }
    }
}
