package io.micronaut.vaadin.netty;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A multipart request reaches Vaadin with its parts read into temporary files, which are deleted once
 * Vaadin has served it. The browser tests upload through Vaadin's upload component.
 */
@MicronautTest
@Property(name = "vaadin.production-mode", value = "true")
class UploadTest {

    private static final String BOUNDARY = "boundary";

    @Inject
    EmbeddedServer server;

    @Test
    void vaadinServesAMultipartRequest() throws Exception {
        String body = """
            --boundary\r
            Content-Disposition: form-data; name="file"; filename="hello.txt"\r
            Content-Type: text/plain\r
            \r
            Hello\r
            --boundary\r
            Content-Disposition: form-data; name="name"\r
            \r
            value\r
            --boundary--\r
            """;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(server.getURI().resolve("/"))
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
            // not a request of a Vaadin upload: Vaadin answers it, without failing on the multipart body
            assertTrue(response.statusCode() < 500, () -> "status " + response.statusCode() + ": " + response.body());
        }
    }
}
