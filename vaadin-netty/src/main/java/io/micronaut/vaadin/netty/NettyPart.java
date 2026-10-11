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
import jakarta.servlet.http.Part;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A part of a multipart request, such as an uploaded file, read into a temporary file before Vaadin
 * handles the request. The file is deleted once the response is sent.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyPart implements Part {

    private final String name;
    private final @Nullable String fileName;
    private final @Nullable String contentType;
    private final Path file;

    NettyPart(String name, @Nullable String fileName, @Nullable String contentType, Path file) {
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(file);
    }

    @Override
    public @Nullable String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable String getSubmittedFileName() {
        return fileName;
    }

    @Override
    public long getSize() {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void write(String fileName) throws IOException {
        Files.copy(file, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void delete() throws IOException {
        Files.deleteIfExists(file);
    }

    @Override
    public @Nullable String getHeader(String name) {
        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
            return contentType;
        }
        if (HttpHeaders.CONTENT_DISPOSITION.equalsIgnoreCase(name)) {
            return contentDisposition();
        }
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        String value = getHeader(name);
        return value == null ? List.of() : List.of(value);
    }

    @Override
    public Collection<String> getHeaderNames() {
        List<String> names = new ArrayList<>();
        names.add(HttpHeaders.CONTENT_DISPOSITION);
        if (contentType != null) {
            names.add(HttpHeaders.CONTENT_TYPE);
        }
        return names;
    }

    private String contentDisposition() {
        String disposition = "form-data; name=\"" + name + "\"";
        return fileName == null ? disposition : disposition + "; filename=\"" + fileName + "\"";
    }
}
