package io.micronaut.vaadin.netty;

import jakarta.servlet.SessionTrackingMode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The servlet context of the servlet facade, which holds the one Vaadin servlet.
 */
class NettyServletContextTest {

    private final NettyServletRegistration registration =
        new NettyServletRegistration("vaadinServlet", NettyVaadinServlet.class.getName(), List.of("/*"));
    private final NettyServletContext context = new NettyServletContext(getClass().getClassLoader(), registration);

    @Test
    void attributesAndInitParameters() {
        context.setAttribute("one", 1);
        assertEquals(1, context.getAttribute("one"));
        assertTrue(Collections.list(context.getAttributeNames()).contains("one"));
        context.setAttribute("one", null);
        assertNull(context.getAttribute("one"));
        context.setAttribute("two", 2);
        context.removeAttribute("two");
        assertNull(context.getAttribute("two"));

        assertTrue(context.setInitParameter("p", "v"));
        assertFalse(context.setInitParameter("p", "w"));
        assertEquals("v", context.getInitParameter("p"));
        assertTrue(Collections.list(context.getInitParameterNames()).contains("p"));
    }

    @Test
    void theOneServletIsRegistered() {
        assertSame(registration, context.getServletRegistration("vaadinServlet"));
        assertNull(context.getServletRegistration("other"));
        assertEquals(Set.of("vaadinServlet"), context.getServletRegistrations().keySet());
        assertEquals("vaadinServlet", registration.getName());
        assertEquals(NettyVaadinServlet.class.getName(), registration.getClassName());
        assertTrue(registration.getMappings().contains("/*"));
        assertTrue(registration.setInitParameter("p", "v"));
        assertEquals("v", registration.getInitParameter("p"));
        assertTrue(registration.getInitParameters().containsKey("p"));
        assertTrue(registration.setInitParameters(java.util.Map.of("q", "w")).isEmpty());
        registration.getRunAsRole();
        assertTrue(context.getFilterRegistrations().isEmpty());
        assertNull(context.getFilterRegistration("filter"));
    }

    @Test
    void theContainer() {
        assertEquals("", context.getContextPath());
        // the one context of the application
        assertNull(context.getContext("/"));
        assertEquals(6, context.getMajorVersion());
        assertTrue(context.getMinorVersion() >= 0);
        assertEquals(context.getMajorVersion(), context.getEffectiveMajorVersion());
        assertEquals(context.getMinorVersion(), context.getEffectiveMinorVersion());
        assertEquals("text/html", context.getMimeType("index.html"));
        assertNotNull(context.getServerInfo());
        assertNotNull(context.getServletContextName());
        assertNotNull(context.getVirtualServerName());
        assertSame(getClass().getClassLoader(), context.getClassLoader());
        assertNull(context.getRealPath("/"));
        assertNull(context.getJspConfigDescriptor());
        assertNull(context.getRequestDispatcher("/other"));
        assertEquals(Set.of(SessionTrackingMode.COOKIE), context.getDefaultSessionTrackingModes());
        assertEquals(Set.of(SessionTrackingMode.COOKIE), context.getEffectiveSessionTrackingModes());
        context.setSessionTimeout(10);
        context.getSessionTimeout();
        context.getRequestCharacterEncoding();
        context.getResponseCharacterEncoding();
        context.log("message");
        context.log("message", new IllegalStateException("logged"));
        context.declareRoles("ADMIN");
        context.addListener(new java.util.EventListener() {
        });
    }

    @Test
    void resourcesComeFromTheClassPath() {
        // the META-INF/resources of the jars, Vaadin's push client among them
        assertNotNull(context.getResource("/VAADIN/static/push/vaadinPush.js"));
        assertNotNull(context.getResourceAsStream("/VAADIN/static/push/vaadinPush.js"));
        assertNull(context.getResource("../escape"));
        assertNull(context.getResource("/missing.txt"));
        assertNull(context.getResourceAsStream("/missing.txt"));
        context.getResourcePaths("/");
    }

    @Test
    void whatTheNettyRuntimeDoesNotSupport() {
        assertThrows(UnsupportedOperationException.class, () -> context.addServlet("other", "x.Y"));
        assertThrows(UnsupportedOperationException.class, () -> context.addFilter("filter", "x.Y"));
        assertThrows(UnsupportedOperationException.class, () -> context.addListener("x.Y"));
        assertThrows(UnsupportedOperationException.class, () -> context.setSessionTrackingModes(Set.of(SessionTrackingMode.URL)));
        assertThrows(UnsupportedOperationException.class, () -> context.addJspFile("jsp", "x.jsp"));
        assertThrows(UnsupportedOperationException.class, () -> context.setRequestCharacterEncoding("UTF-8"));
        assertThrows(UnsupportedOperationException.class, () -> context.setResponseCharacterEncoding("UTF-8"));
    }
}
