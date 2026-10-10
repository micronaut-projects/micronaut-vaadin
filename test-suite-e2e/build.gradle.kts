plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// End-to-end tests in a real browser, with Playwright: Vaadin on Jetty in development mode, with the
// pre-built development bundle (no Node.js), and push over WebSockets
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinServlet)
    testImplementation(mnServlet.micronaut.http.server.jetty)
    testImplementation(mnServlet.micronaut.servlet.websocket)
    testImplementation(libs.jetty.ee10.websocket.jakarta.server)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(libs.vaadin.dev.server)
    testImplementation(libs.vaadin.dev.bundle)
    testImplementation(libs.playwright)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    // development mode writes generated frontend files into the project: keep them under build/
    val projectDir = layout.buildDirectory.dir("vaadin-project")
    doFirst {
        projectDir.get().asFile.mkdirs()
    }
    systemProperty("vaadin.project.basedir", projectDir.get().asFile.absolutePath)
}
