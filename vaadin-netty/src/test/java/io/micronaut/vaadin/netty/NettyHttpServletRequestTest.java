package io.micronaut.vaadin.netty;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.cookie.Cookie;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The request of the servlet facade, a view of a request of Micronaut.
 */
class NettyHttpServletRequestTest {

    private final NettyServletContext servletContext = new NettyServletContext(
        getClass().getClassLoader(), new NettyServletRegistration("vaadinServlet", NettyVaadinServlet.class.getName(), List.of("/ui/*")));
    private final TestSessionStore sessionStore = new TestSessionStore();

    private NettyHttpServletRequest request(MutableHttpRequest<?> micronaut, String body) {
        return new NettyHttpServletRequest(micronaut, new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
            servletContext, sessionStore, "/ui", "/view");
    }

    @Test
    void theRequestLineAndParameters() {
        MutableHttpRequest<?> micronaut = HttpRequest.GET("/ui/view?a=1&a=2&b=3");
        micronaut.getParameters().add("a", List.of("1", "2")).add("b", List.of("3"));
        NettyHttpServletRequest request = request(micronaut, "");
        assertEquals("GET", request.getMethod());
        assertEquals("/ui/view", request.getRequestURI());
        assertEquals("a=1&a=2&b=3", request.getQueryString());
        assertEquals("/ui", request.getServletPath());
        assertEquals("/view", request.getPathInfo());
        assertEquals("", request.getContextPath());
        assertNull(request.getPathTranslated());
        assertTrue(request.getRequestURL().toString().endsWith("/ui/view"));

        assertEquals("1", request.getParameter("a"));
        assertArrayEquals(new String[] {"1", "2"}, request.getParameterValues("a"));
        assertNull(request.getParameterValues("missing"));
        assertNull(request.getParameter("missing"));
        assertEquals(List.of("a", "b"), Collections.list(request.getParameterNames()).stream().sorted().toList());
        assertArrayEquals(new String[] {"3"}, request.getParameterMap().get("b"));
    }

    @Test
    void headersAndCookies() {
        MutableHttpRequest<?> micronaut = HttpRequest.GET("/ui/view")
            .header("X-Many", "a")
            .header("X-Int", "42")
            .header("If-Modified-Since", "Thu, 1 Jan 1970 00:00:01 GMT")
            .cookie(Cookie.of("name", "value"));
        micronaut.getHeaders().add("X-Many", "b");
        NettyHttpServletRequest request = request(micronaut, "");

        assertEquals("a", request.getHeader("x-many"));
        assertEquals(List.of("a", "b"), Collections.list(request.getHeaders("X-Many")));
        assertTrue(Collections.list(request.getHeaderNames()).stream().anyMatch("X-Int"::equalsIgnoreCase));
        assertEquals(42, request.getIntHeader("X-Int"));
        assertEquals(-1, request.getIntHeader("X-Missing"));
        assertEquals(1000L, request.getDateHeader("If-Modified-Since"));
        assertEquals(-1L, request.getDateHeader("X-Missing"));

        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        assertEquals(1, cookies.length);
        assertEquals("name", cookies[0].getName());
        assertEquals("value", cookies[0].getValue());
        assertNull(request(HttpRequest.GET("/ui/view"), "").getCookies());
    }

    @Test
    void theBodyIsReadOnce() throws IOException {
        NettyHttpServletRequest request = request(HttpRequest.POST("/ui/view", "")
            .contentType(MediaType.of("text/plain; charset=UTF-8"))
            .header("Content-Length", "5"), "héllo");
        assertEquals("UTF-8", request.getCharacterEncoding());
        assertEquals("text/plain;charset=UTF-8", request.getContentType().replace(" ", ""));
        assertEquals(5, request.getContentLength());
        assertEquals(5L, request.getContentLengthLong());

        ServletInputStream in = request.getInputStream();
        assertFalse(in.isFinished());
        assertTrue(in.isReady());
        byte[] bytes = new byte[16];
        int first = in.read();
        int rest = in.read(bytes, 0, bytes.length);
        assertEquals('h', first);
        assertEquals("éllo", new String(bytes, 0, rest, StandardCharsets.UTF_8));
        assertThrows(UnsupportedOperationException.class, () -> in.setReadListener(null));

        NettyHttpServletRequest text = request(HttpRequest.POST("/ui/view", ""), "line");
        text.setCharacterEncoding("UTF-8");
        assertEquals("line", text.getReader().readLine());
    }

    @Test
    void attributesAndTheConnection() {
        NettyHttpServletRequest request = request(HttpRequest.GET("http://localhost:8080/ui/view").header("Accept-Language", "fr-FR, en"), "");
        request.setAttribute("one", 1);
        assertEquals(1, request.getAttribute("one"));
        assertTrue(Collections.list(request.getAttributeNames()).contains("one"));
        request.setAttribute("one", null);
        assertNull(request.getAttribute("one"));
        request.setAttribute("two", 2);
        request.removeAttribute("two");
        assertNull(request.getAttribute("two"));

        assertEquals(Locale.FRANCE, request.getLocale());
        assertTrue(Collections.list(request.getLocales()).contains(Locale.FRANCE));
        assertEquals("http", request.getScheme());
        assertFalse(request.isSecure());
        assertNotNull(request.getServerName());
        assertNotNull(request.getProtocol());
        assertNotNull(request.getRemoteAddr());
        assertNotNull(request.getRemoteHost());
        assertNotNull(request.getLocalName());
        assertNotNull(request.getLocalAddr());
        request.getServerPort();
        request.getRemotePort();
        request.getLocalPort();
        assertSame(servletContext, request.getServletContext());
        assertEquals(DispatcherType.REQUEST, request.getDispatcherType());
        assertNull(request.getRequestDispatcher("/other"));
        assertNotNull(request.getRequestId());
        assertNotNull(request.getProtocolRequestId());
    }

    @Test
    void anonymousRequestsHaveNoUser() {
        NettyHttpServletRequest request = request(HttpRequest.GET("/ui/view"), "");
        assertNull(request.getAuthType());
        assertNull(request.getUserPrincipal());
        assertNull(request.getRemoteUser());
        assertFalse(request.isUserInRole("ADMIN"));
    }

    @Test
    void theSessionIsCreatedOnDemand() {
        NettyHttpServletRequest request = request(HttpRequest.GET("/ui/view"), "");
        assertNull(request.getSession(false));
        assertNull(request.getRequestedSessionId());
        HttpSession session = request.getSession();
        assertSame(session, request.getSession(false));
        assertNotNull(session.getId());
        assertSame(servletContext, session.getServletContext());
        assertEquals(session.getId(), request.getRequestedSessionId());
    }
}
