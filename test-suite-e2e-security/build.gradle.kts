plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// Signing in and out in a real browser, on Netty: Vaadin's login form posts to Micronaut Security, which
// keeps the user in a JWT cookie
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinSecurity)
    testImplementation(projects.micronautVaadinNetty)
    testImplementation(mnSecurity.micronaut.security.jwt)
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
