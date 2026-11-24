plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// The view access tests of vaadin-security, run on Jetty: there, Vaadin's requests are authenticated by a
// servlet filter, as Micronaut Security's filter only runs for Micronaut's own routes
val securityTests = project(":micronaut-vaadin-security").file("src/test")
sourceSets {
    named("test") {
        java.srcDir(File(securityTests, "java"))
        resources.srcDir(File(securityTests, "resources"))
    }
}

dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinSecurity)
    testImplementation(projects.micronautVaadinServlet)
    testImplementation(mnServlet.micronaut.http.server.jetty)
    testImplementation(mn.micronaut.http.client.jdk)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}
