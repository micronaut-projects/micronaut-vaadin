package io.micronaut.vaadin.docs.quickstart

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.vaadin.VaadinConfigurationProperties
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest(startApplication = false)
@Property(name = "vaadin.url-mapping", value = "/ui/*")
@Property(name = "vaadin.exclude-urls", value = "/api/**,/health")
class VaadinConfigurationTest {

    // tag::config[]
    @Inject
    lateinit var vaadin: VaadinConfigurationProperties // <1>

    @Test
    fun readsTheVaadinConfiguration() {
        assertEquals("/ui/*", vaadin.urlMapping) // <2>
        assertEquals(listOf("/api/**", "/health"), vaadin.excludeUrls)
    }
    // end::config[]
}
