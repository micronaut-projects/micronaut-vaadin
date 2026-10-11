package io.micronaut.vaadin.netty;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MutableHttpResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The response of the servlet facade, which buffers what Vaadin writes into a response of Micronaut.
 */
class NettyHttpServletResponseTest {

    @Test
    void statusAndHeadersReachTheMicronautResponse() {
        NettyHttpServletResponse response = new NettyHttpServletResponse();
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setHeader("X-One", "1");
        response.addHeader("X-Many", "a");
        response.addHeader("x-many", "b");
        response.setIntHeader("X-Int", 7);
        response.addIntHeader("X-Int", 8);
        response.setDateHeader("X-Date", 0);
        response.addDateHeader("X-Date", 1000);

        assertTrue(response.containsHeader("x-one"));
        assertEquals("1", response.getHeader("X-One"));
        assertEquals(List.of("a", "b"), List.copyOf(response.getHeaders("X-Many")));
        assertEquals(List.of("7", "8"), List.copyOf(response.getHeaders("X-Int")));
        assertEquals("Thu, 1 Jan 1970 00:00:00 GMT", response.getHeader("X-Date"));
        assertTrue(response.getHeaderNames().contains("X-One"));

        response.setHeader("X-One", null);
        assertFalse(response.containsHeader("X-One"));
        assertNull(response.getHeader("X-Missing"));

        MutableHttpResponse<byte[]> micronaut = response.toHttpResponse();
        assertEquals(201, micronaut.code());
        assertEquals(List.of("a", "b"), micronaut.getHeaders().getAll("X-Many"));
    }

    @Test
    void theContentTypeCarriesTheCharacterEncoding() {
        NettyHttpServletResponse response = new NettyHttpServletResponse();
        assertEquals("ISO-8859-1", response.getCharacterEncoding());
        assertNull(response.getContentType());

        response.setContentType("text/html; charset=UTF-8");
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("text/html;charset=UTF-8", response.getContentType());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-16");
        assertEquals("application/json;charset=UTF-16", response.getContentType().replace(" ", ""));
        assertEquals("application/json;charset=UTF-16",
            response.toHttpResponse().getHeaders().get(HttpHeaders.CONTENT_TYPE).replace(" ", ""));

        response.setContentType(null);
        response.setCharacterEncoding((String) null);
        assertNull(response.getContentType());
    }

    @Test
    void theWriterBecomesTheBody() {
        NettyHttpServletResponse response = new NettyHttpServletResponse();
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print("héllo");
        assertEquals(response.getWriter(), writer);
        assertThrows(IllegalStateException.class, response::getOutputStream);

        assertArrayEquals("héllo".getBytes(StandardCharsets.UTF_8), response.toHttpResponse().body());
    }

    @Test
    void theOutputStreamBecomesTheBody() throws IOException {
        NettyHttpServletResponse response = new NettyHttpServletResponse();
        ServletOutputStream out = response.getOutputStream();
        out.write('a');
        out.write("bcd".getBytes(StandardCharsets.US_ASCII), 1, 2);
        assertTrue(out.isReady());
        // the body is buffered: there is no asynchronous writing to listen to
        assertThrows(UnsupportedOperationException.class, () -> out.setWriteListener(null));
        assertThrows(IllegalStateException.class, response::getWriter);

        response.setContentLength(3);
        response.setContentLengthLong(3);
        assertArrayEquals("acd".getBytes(StandardCharsets.US_ASCII), response.toHttpResponse().body());
    }

    @Test
    void theBufferCanBeResetUntilCommitted() throws IOException {
        NettyHttpServletResponse response = new NettyHttpServletResponse();
        response.setBufferSize(1024);
        assertTrue(response.getBufferSize() >= 0);
        response.getOutputStream().write('x');
        response.setHeader("X-One", "1");
        response.resetBuffer();
        assertFalse(response.isCommitted());
        response.reset();
        assertFalse(response.containsHeader("X-One"));
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        response.flushBuffer();

        response.setLocale(Locale.FRANCE);
        assertEquals(Locale.FRANCE, response.getLocale());
    }

    @Test
    void errorsAndRedirectsSetTheStatus() throws IOException {
        NettyHttpServletResponse error = new NettyHttpServletResponse();
        error.sendError(HttpServletResponse.SC_NOT_FOUND, "missing");
        assertEquals(HttpServletResponse.SC_NOT_FOUND, error.getStatus());

        NettyHttpServletResponse plainError = new NettyHttpServletResponse();
        plainError.sendError(HttpServletResponse.SC_FORBIDDEN);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, plainError.getStatus());

        NettyHttpServletResponse redirect = new NettyHttpServletResponse();
        redirect.sendRedirect("/login", HttpServletResponse.SC_FOUND, true);
        assertEquals(HttpServletResponse.SC_FOUND, redirect.getStatus());
        assertEquals("/login", redirect.getHeader(HttpHeaders.LOCATION));

        assertEquals("/a?b", redirect.encodeURL("/a?b"));
        assertEquals("/a?b", redirect.encodeRedirectURL("/a?b"));
    }

    @Test
    void cookiesBecomeSetCookieHeaders() {
        NettyHttpServletResponse response = new NettyHttpServletResponse();
        Cookie cookie = new Cookie("name", "value");
        cookie.setPath("/");
        cookie.setDomain("example.com");
        cookie.setMaxAge(60);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        Cookie plain = new Cookie("plain", null);
        plain.setSecure(true);
        response.addCookie(plain);

        List<String> headers = response.toHttpResponse().getHeaders().getAll(HttpHeaders.SET_COOKIE);
        assertEquals("name=value; Path=/; Domain=example.com; Max-Age=60; Secure; HttpOnly; SameSite=Strict", headers.get(0));
        assertEquals("plain=; Secure", headers.get(1));
    }
}
