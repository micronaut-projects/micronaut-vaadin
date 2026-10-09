package io.micronaut.vaadin.docs.quickstart

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.vaadin.VaadinConfigurationProperties
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name = "vaadin.url-mapping", value = "/ui/*")
@Property(name = "vaadin.exclude-urls", value = "/api/**,/health")
class VaadinConfigurationTest extends Specification {

    // tag::config[]
    @Inject
    VaadinConfigurationProperties vaadin // <1>

    void "reads the Vaadin configuration"() {
        expect:
        vaadin.urlMapping == "/ui/*" // <2>
        vaadin.excludeUrls == ["/api/**", "/health"]
    }
    // end::config[]
}
