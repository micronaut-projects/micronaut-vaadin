plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// The browser tests of test-suite-e2e, on Netty. Push is not available on Netty yet, so its view, the
// application shell that enables it, and its test are left out
val e2eTests = project(":test-suite-e2e").file("src/test")
sourceSets {
    named("test") {
        java {
            srcDir(File(e2eTests, "java"))
            exclude("**/app/PushView.java", "**/app/AppShell.java")
        }
        resources.srcDir(File(e2eTests, "resources"))
    }
}

dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
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
    filter {
        excludeTestsMatching("*.BrowserTest.backgroundUpdatesArePushed")
    }
    // development mode writes generated frontend files into the project: keep them under build/
    val projectDir = layout.buildDirectory.dir("vaadin-project")
    doFirst {
        projectDir.get().asFile.mkdirs()
    }
    systemProperty("vaadin.project.basedir", projectDir.get().asFile.absolutePath)
}
