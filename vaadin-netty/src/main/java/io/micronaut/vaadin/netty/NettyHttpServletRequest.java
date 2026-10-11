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
package io.micronaut.vaadin.netty;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.session.Session;
import io.micronaut.session.SessionStore;
import io.micronaut.session.http.HttpSessionFilter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A request of the servlet API over a request of Micronaut, with the parts of the API that Vaadin uses.
 * The body is read from a stream, on a thread that may block.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyHttpServletRequest implements HttpServletRequest {

    private final HttpRequest<?> request;
    private final InputStream body;
    private final NettyServletContext servletContext;
    private final SessionStore<Session> sessionStore;
    private final String servletPath;
    private final @Nullable String pathInfo;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private @Nullable String characterEncoding;
    private @Nullable NettyHttpSession session;

    NettyHttpServletRequest(HttpRequest<?> request,
                            InputStream body,
                            NettyServletContext servletContext,
                            SessionStore<Session> sessionStore,
                            String servletPath,
                            @Nullable String pathInfo) {
        this.request = request;
        this.body = body;
        this.servletContext = servletContext;
        this.sessionStore = sessionStore;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.characterEncoding = request.getContentType().flatMap(MediaType::getCharset).map(Charset::name).orElse(null);
    }

    @Override
    public @Nullable String getAuthType() {
        return null;
    }

    @Override
    public Cookie @Nullable [] getCookies() {
        List<Cookie> cookies = request.getCookies().getAll().stream()
            .map(cookie -> new Cookie(cookie.getName(), cookie.getValue()))
            .toList();
        return cookies.isEmpty() ? null : cookies.toArray(Cookie[]::new);
    }

    @Override
    public long getDateHeader(String name) {
        return request.getHeaders().findDate(name).map(date -> date.toInstant().toEpochMilli()).orElse(-1L);
    }

    @Override
    public @Nullable String getHeader(String name) {
        return request.getHeaders().get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(request.getHeaders().getAll(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(request.getHeaders().names());
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        return value == null ? -1 : Integer.parseInt(value);
    }

    @Override
    public String getMethod() {
        return request.getMethodName();
    }

    @Override
    public @Nullable String getPathInfo() {
        return pathInfo;
    }

    @Override
    public @Nullable String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public @Nullable String getQueryString() {
        return request.getUri().getRawQuery();
    }

    @Override
    public @Nullable String getRemoteUser() {
        Principal principal = getUserPrincipal();
        return principal == null ? null : principal.getName();
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public @Nullable Principal getUserPrincipal() {
        return request.getUserPrincipal().orElse(null);
    }

    @Override
    public @Nullable String getRequestedSessionId() {
        return currentSession().map(Session::getId).orElse(null);
    }

    @Override
    public String getRequestURI() {
        return request.getUri().getRawPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(getScheme()).append("://").append(host()).append(getRequestURI());
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public @Nullable HttpSession getSession(boolean create) {
        if (session == null) {
            Session existing = currentSession().orElse(null);
            if (existing == null) {
                if (!create) {
                    return null;
                }
                existing = sessionStore.newSession();
                // Micronaut Session's filter saves the session and writes its cookie with the response
                request.setAttribute(HttpSessionFilter.SESSION_ATTRIBUTE, existing);
            }
            session = new NettyHttpSession(existing, sessionStore, servletContext);
        }
        return session;
    }

    @Override
    public HttpSession getSession() {
        HttpSession httpSession = getSession(true);
        if (httpSession == null) {
            throw new IllegalStateException("No session");
        }
        return httpSession;
    }

    @Override
    public String changeSessionId() {
        throw unsupported();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return currentSession().isPresent();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return currentSession().isPresent();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        throw unsupported();
    }

    @Override
    public void login(String username, String password) {
        throw unsupported();
    }

    @Override
    public void logout() {
        throw unsupported();
    }

    @Override
    public Collection<Part> getParts() {
        throw unsupported();
    }

    @Override
    public @Nullable Part getPart(String name) {
        throw unsupported();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        throw unsupported();
    }

    @Override
    public @Nullable Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public @Nullable String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String encoding) {
        this.characterEncoding = encoding;
    }

    @Override
    public int getContentLength() {
        long length = getContentLengthLong();
        return length > Integer.MAX_VALUE ? -1 : (int) length;
    }

    @Override
    public long getContentLengthLong() {
        return request.getContentLength();
    }

    @Override
    public @Nullable String getContentType() {
        return request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new ServletInputStream() {
            private boolean finished;

            @Override
            public boolean isFinished() {
                return finished;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw unsupported();
            }

            @Override
            public int read() throws IOException {
                int read = body.read();
                finished = read == -1;
                return read;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                int read = body.read(bytes, offset, length);
                finished = read == -1;
                return read;
            }
        };
    }

    @Override
    public @Nullable String getParameter(String name) {
        return request.getParameters().get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(request.getParameters().names());
    }

    @Override
    public String @Nullable [] getParameterValues(String name) {
        List<String> values = request.getParameters().getAll(name);
        return values.isEmpty() ? null : values.toArray(String[]::new);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        for (String name : request.getParameters().names()) {
            parameters.put(name, request.getParameters().getAll(name).toArray(String[]::new));
        }
        return parameters;
    }

    @Override
    public String getProtocol() {
        return switch (request.getHttpVersion()) {
            case HTTP_1_0 -> "HTTP/1.0";
            case HTTP_2_0 -> "HTTP/2.0";
            default -> "HTTP/1.1";
        };
    }

    @Override
    public String getScheme() {
        return request.isSecure() ? "https" : "http";
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerAddress().getPort();
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = characterEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(characterEncoding);
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress address = request.getRemoteAddress();
        return address.getAddress() == null ? address.getHostString() : address.getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteAddress().getHostString();
    }

    @Override
    public void setAttribute(String name, @Nullable Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Locale getLocale() {
        return request.getLocale().orElse(Locale.getDefault());
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(List.of(getLocale()));
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public @Nullable RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return request.getRemoteAddress().getPort();
    }

    @Override
    public String getLocalName() {
        return request.getServerAddress().getHostString();
    }

    @Override
    public String getLocalAddr() {
        InetSocketAddress address = request.getServerAddress();
        return address.getAddress() == null ? address.getHostString() : address.getAddress().getHostAddress();
    }

    @Override
    public int getLocalPort() {
        return request.getServerAddress().getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public AsyncContext startAsync() {
        throw unsupported();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        throw unsupported();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("The request is not asynchronous");
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getRequestId() {
        return Integer.toHexString(System.identityHashCode(request));
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        throw unsupported();
    }

    private Optional<Session> currentSession() {
        return request.getAttribute(HttpSessionFilter.SESSION_ATTRIBUTE, Session.class);
    }

    private String host() {
        String host = request.getHeaders().get(HttpHeaders.HOST);
        return host == null ? getServerName() + ":" + getServerPort() : host;
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by Vaadin on the Netty server of Micronaut");
    }
}
