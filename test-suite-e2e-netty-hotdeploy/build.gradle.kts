plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// Vaadin's development mode with Vite (vaadin.frontend.hotdeploy) on Netty: the browser receives the changes of the
// frontend sources through the runtime's proxy of Vite's WebSocket. Vite needs Node.js, and its first start installs
// the frontend dependencies
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinNetty)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(libs.vaadin.dev.server)
    testImplementation(libs.playwright)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    // development mode reads the frontend sources and writes generated files into the project: keep it under build/
    val projectDir = layout.buildDirectory.dir("vaadin-project")
    val frontend = layout.projectDirectory.dir("src/test/frontend")
    inputs.dir(frontend)
    doFirst {
        val target = projectDir.get().dir("src/main/frontend").asFile
        target.mkdirs()
        frontend.asFile.listFiles()?.forEach { it.copyTo(File(target, it.name), overwrite = true) }
    }
    systemProperty("vaadin.project.basedir", projectDir.get().asFile.absolutePath)
}
