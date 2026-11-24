package io.micronaut.vaadin.netty;

import io.micronaut.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A part of a multipart request, read into a temporary file.
 */
class NettyPartTest {

    @TempDir
    Path directory;

    @Test
    void aFile() throws IOException {
        Path file = Files.writeString(directory.resolve("upload.part"), "Hello", StandardCharsets.UTF_8);
        NettyPart part = new NettyPart("file", "hello.txt", "text/plain", file);

        assertEquals("file", part.getName());
        assertEquals("hello.txt", part.getSubmittedFileName());
        assertEquals("text/plain", part.getContentType());
        assertEquals(5, part.getSize());
        try (InputStream in = part.getInputStream()) {
            assertEquals("Hello", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals("text/plain", part.getHeader("content-type"));
        assertEquals("form-data; name=\"file\"; filename=\"hello.txt\"", part.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertNull(part.getHeader("X-Other"));
        assertEquals(List.of("text/plain"), List.copyOf(part.getHeaders(HttpHeaders.CONTENT_TYPE)));
        assertEquals(List.of(), List.copyOf(part.getHeaders("X-Other")));
        assertEquals(List.of(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.CONTENT_TYPE), List.copyOf(part.getHeaderNames()));

        Path copy = directory.resolve("copy.txt");
        part.write(copy.toString());
        assertEquals("Hello", Files.readString(copy));

        part.delete();
        assertFalse(Files.exists(file));
        assertEquals(-1, part.getSize());
    }

    @Test
    void aField() throws IOException {
        NettyPart part = new NettyPart("name", null, null, Files.writeString(directory.resolve("field.part"), "value"));
        assertNull(part.getSubmittedFileName());
        assertNull(part.getContentType());
        assertEquals("form-data; name=\"name\"", part.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(List.of(HttpHeaders.CONTENT_DISPOSITION), List.copyOf(part.getHeaderNames()));
    }
}
