plugins {
    id("io.micronaut.build.internal.vaadin-module")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)

    api(projects.micronautVaadinCore)
    api(mnServlet.micronaut.servlet.engine)
    compileOnly(libs.jetty.ee10.servlet)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(mnServlet.micronaut.http.server.jetty)
    testImplementation(mnServlet.micronaut.servlet.websocket)
    testImplementation(libs.jetty.ee10.websocket.jakarta.server)
    testImplementation(mn.micronaut.http.client.jdk)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
    testRuntimeOnly(mnLogging.logback.classic)
}

micronautBuild {
    // No released version to compare against before 1.0.0
    binaryCompatibility.enabledAfter("1.0.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
