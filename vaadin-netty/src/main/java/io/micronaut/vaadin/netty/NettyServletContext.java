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

import io.micronaut.http.MediaType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The servlet context of Vaadin on Netty: holds Vaadin's attributes, and serves the web resources of the
 * {@code META-INF/resources} of the jars on the classpath. It knows one servlet, the Vaadin servlet; it
 * cannot register others.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyServletContext implements ServletContext {

    private static final Logger LOG = LoggerFactory.getLogger(NettyServletContext.class);
    private static final String WEB_RESOURCES = "META-INF/resources";

    private final ClassLoader classLoader;
    private final NettyServletRegistration registration;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, String> initParameters = new ConcurrentHashMap<>();
    private int sessionTimeout;

    NettyServletContext(ClassLoader classLoader, NettyServletRegistration registration) {
        this.classLoader = classLoader;
        this.registration = registration;
    }

    NettyServletRegistration getRegistration() {
        return registration;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public @Nullable ServletContext getContext(String uripath) {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 1;
    }

    @Override
    public @Nullable String getMimeType(String file) {
        return MediaType.forFilename(file).toString();
    }

    @Override
    public @Nullable Set<String> getResourcePaths(String path) {
        return null;
    }

    @Override
    public @Nullable URL getResource(String path) {
        if (!path.startsWith("/") || path.contains("..")) {
            return null;
        }
        return classLoader.getResource(WEB_RESOURCES + path);
    }

    @Override
    public @Nullable InputStream getResourceAsStream(String path) {
        if (!path.startsWith("/") || path.contains("..")) {
            return null;
        }
        return classLoader.getResourceAsStream(WEB_RESOURCES + path);
    }

    @Override
    public @Nullable RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public @Nullable RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public void log(String msg) {
        LOG.info(msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        LOG.error(message, throwable);
    }

    @Override
    public @Nullable String getRealPath(String path) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return "Micronaut Netty";
    }

    @Override
    public @Nullable String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return initParameters.putIfAbsent(name, value) == null;
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
    public void setAttribute(String name, @Nullable Object object) {
        if (object == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, object);
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getServletContextName() {
        return "vaadin";
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        throw unsupported();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        throw unsupported();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw unsupported();
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        throw unsupported();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) {
        throw unsupported();
    }

    @Override
    public @Nullable ServletRegistration getServletRegistration(String servletName) {
        return registration.getName().equals(servletName) ? registration : null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Map.of(registration.getName(), registration);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw unsupported();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw unsupported();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        throw unsupported();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) {
        throw unsupported();
    }

    @Override
    public @Nullable FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Map.of();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw unsupported();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        throw unsupported();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Set.of(SessionTrackingMode.COOKIE);
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Set.of(SessionTrackingMode.COOKIE);
    }

    @Override
    public void addListener(String className) {
        throw unsupported();
    }

    @Override
    public <T extends EventListener> void addListener(T listener) {
        // the context has no lifecycle events to deliver: Vaadin's listeners are not needed on Netty
        LOG.debug("Ignoring the servlet context listener {}", listener);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        throw unsupported();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) {
        throw unsupported();
    }

    @Override
    public @Nullable JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void declareRoles(String... roleNames) {
        // roles come from Micronaut Security
    }

    @Override
    public String getVirtualServerName() {
        return "micronaut";
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public @Nullable String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        throw unsupported();
    }

    @Override
    public @Nullable String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not supported by Vaadin on the Netty server of Micronaut");
    }
}
