package io.micronaut.vaadin;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaadinConfigurationPropertiesTest {

    @Test
    void defaults() {
        try (ApplicationContext context = ApplicationContext.run()) {
            VaadinConfigurationProperties config = context.getBean(VaadinConfigurationProperties.class);
            assertEquals("/*", config.getUrlMapping());
            assertTrue(config.isAsyncSupported());
            assertTrue(config.isLoadOnStartup());
            assertTrue(config.getExcludeUrls().isEmpty());
        }
    }

    @Test
    void configured() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "vaadin.url-mapping", "/ui/*",
            "vaadin.exclude-urls", List.of("/api/**", "/health")
        ))) {
            VaadinConfigurationProperties config = context.getBean(VaadinConfigurationProperties.class);
            assertEquals("/ui/*", config.getUrlMapping());
            assertEquals(List.of("/api/**", "/health"), config.getExcludeUrls());
        }
    }
}
