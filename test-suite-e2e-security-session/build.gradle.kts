plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// The sign in and out tests of test-suite-e2e-security, on Netty, with the authentication kept in the
// Micronaut session
val securityTests = project(":test-suite-e2e-security").file("src/test")
sourceSets {
    named("test") {
        java.srcDir(File(securityTests, "java"))
    }
}

dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinSecurity)
    testImplementation(projects.micronautVaadinNetty)
    testImplementation(mnSecurity.micronaut.security.session)
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
