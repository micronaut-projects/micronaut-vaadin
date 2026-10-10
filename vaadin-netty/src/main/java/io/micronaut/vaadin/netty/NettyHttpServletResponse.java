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
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * A response of the servlet API that buffers what Vaadin writes, then becomes a response of Micronaut.
 * Vaadin's responses to the browser are small, except for static files.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyHttpServletResponse implements HttpServletResponse {

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<Cookie> cookies = new ArrayList<>();
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private int status = SC_OK;
    private @Nullable String contentType;
    private @Nullable String characterEncoding;
    private Locale locale = Locale.getDefault();
    private @Nullable PrintWriter writer;
    private @Nullable ServletOutputStream outputStream;

    /**
     * @return The response for Micronaut
     */
    MutableHttpResponse<byte[]> toHttpResponse() {
        if (writer != null) {
            writer.flush();
        }
        MutableHttpResponse<byte[]> response = HttpResponse.<byte[]>ok().status(status);
        headers.forEach((name, values) -> values.forEach(value -> response.getHeaders().add(name, value)));
        String fullContentType = getContentType();
        if (fullContentType != null) {
            response.getHeaders().set(HttpHeaders.CONTENT_TYPE, fullContentType);
        }
        for (Cookie cookie : cookies) {
            response.getHeaders().add(HttpHeaders.SET_COOKIE, setCookieHeader(cookie));
        }
        if (body.size() > 0) {
            response.body(body.toByteArray());
        }
        return response;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public void sendError(int sc, @Nullable String msg) {
        resetBuffer();
        status = sc;
        if (msg != null) {
            setContentType("text/plain");
            body.writeBytes(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void sendError(int sc) {
        sendError(sc, null);
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) {
        if (clearBuffer) {
            resetBuffer();
        }
        status = sc;
        setHeader(HttpHeaders.LOCATION, location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, formatDate(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, formatDate(date));
    }

    @Override
    public void setHeader(String name, @Nullable String value) {
        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
            setContentType(value);
        } else if (value == null) {
            headers.remove(name);
        } else {
            List<String> values = new ArrayList<>();
            values.add(value);
            headers.put(name, values);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
            setContentType(value);
        } else {
            headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, String.valueOf(value));
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public @Nullable String getHeader(String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, List.of());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding == null ? StandardCharsets.ISO_8859_1.name() : characterEncoding;
    }

    @Override
    public @Nullable String getContentType() {
        if (contentType == null) {
            return null;
        }
        return characterEncoding == null || contentType.contains("charset=")
            ? contentType
            : contentType + ";charset=" + characterEncoding;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    throw new UnsupportedOperationException("Not supported by Vaadin on the Netty server of Micronaut");
                }

                @Override
                public void write(int b) {
                    body.write(b);
                }

                @Override
                public void write(byte[] bytes, int offset, int length) {
                    body.write(bytes, offset, length);
                }
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called");
        }
        if (writer == null) {
            if (characterEncoding == null) {
                characterEncoding = StandardCharsets.UTF_8.name();
            }
            writer = new PrintWriter(new OutputStreamWriter(body, Charset.forName(characterEncoding)));
        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(@Nullable String charset) {
        if (writer == null) {
            this.characterEncoding = charset;
        }
    }

    @Override
    public void setContentLength(int len) {
        // the length of the buffered body is used
    }

    @Override
    public void setContentLengthLong(long len) {
        // the length of the buffered body is used
    }

    @Override
    public void setContentType(@Nullable String type) {
        if (type == null) {
            contentType = null;
            return;
        }
        int charset = type.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (charset >= 0) {
            setCharacterEncoding(type.substring(charset + "charset=".length()).trim());
            String mediaType = type.substring(0, charset).trim();
            contentType = mediaType.endsWith(";") ? mediaType.substring(0, mediaType.length() - 1).trim() : mediaType;
        } else {
            contentType = type;
        }
    }

    @Override
    public void setBufferSize(int size) {
        // the whole response is buffered
    }

    @Override
    public int getBufferSize() {
        return body.size();
    }

    @Override
    public void flushBuffer() {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public void resetBuffer() {
        if (writer != null) {
            writer.flush();
        }
        body.reset();
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        resetBuffer();
        headers.clear();
        cookies.clear();
        status = SC_OK;
        contentType = null;
    }

    @Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    private static String formatDate(long date) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC));
    }

    private static String setCookieHeader(Cookie cookie) {
        StringBuilder header = new StringBuilder(cookie.getName()).append('=').append(cookie.getValue() == null ? "" : cookie.getValue());
        if (cookie.getPath() != null) {
            header.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getDomain() != null) {
            header.append("; Domain=").append(cookie.getDomain());
        }
        if (cookie.getMaxAge() >= 0) {
            header.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.getSecure()) {
            header.append("; Secure");
        }
        if (cookie.isHttpOnly()) {
            header.append("; HttpOnly");
        }
        String sameSite = cookie.getAttribute("SameSite");
        if (sameSite != null) {
            header.append("; SameSite=").append(sameSite);
        }
        return header.toString();
    }
}
