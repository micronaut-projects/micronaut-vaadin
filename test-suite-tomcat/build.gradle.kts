plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// The servlet runtime tests of vaadin-servlet, run on Tomcat
val servletTests = project(":micronaut-vaadin-servlet").file("src/test")
sourceSets {
    named("test") {
        java.srcDir(File(servletTests, "java"))
        resources.srcDir(File(servletTests, "resources"))
    }
}

dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinServlet)
    testImplementation(mnServlet.micronaut.http.server.tomcat)
    testImplementation(mnServlet.micronaut.servlet.websocket)
    testImplementation(libs.tomcat.embed.websocket)
    testImplementation(mn.micronaut.http.client.jdk)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}
