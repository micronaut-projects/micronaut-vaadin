plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// An application using a Vaadin add-on with a frontend module of its own, in development mode on Netty: the
// add-on's component is not in the compile-time index of the application, which only knows its own classes
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.testSuiteE2eAddonLib)
    testImplementation(projects.micronautVaadinNetty)
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
