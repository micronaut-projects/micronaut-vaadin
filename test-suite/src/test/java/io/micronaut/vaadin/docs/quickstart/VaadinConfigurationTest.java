package io.micronaut.vaadin.docs.quickstart;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.vaadin.VaadinConfigurationProperties;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
@Property(name = "vaadin.url-mapping", value = "/ui/*")
@Property(name = "vaadin.exclude-urls", value = "/api/**,/health")
class VaadinConfigurationTest {

    // tag::config[]
    @Inject
    VaadinConfigurationProperties vaadin; // <1>

    @Test
    void readsTheVaadinConfiguration() {
        assertEquals("/ui/*", vaadin.getUrlMapping()); // <2>
        assertEquals(List.of("/api/**", "/health"), vaadin.getExcludeUrls());
    }
    // end::config[]
}
