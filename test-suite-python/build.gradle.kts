plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
    id("io.micronaut.build.internal.python")
}

dependencies {
    // Annotation processors MUST be testImplementation (not testAnnotationProcessor): the Python
    // compiler takes the compile classpath as its annotation processor path.
    testImplementation(mn.micronaut.inject.python.test)
    testImplementation(mn.micronaut.context.python)
    testImplementation(projects.micronautVaadinProcessor)

    testImplementation(projects.micronautVaadinCore)
    testImplementation(projects.micronautVaadinSecurity)
    testImplementation(projects.micronautVaadinBrowserlessTest)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(mnTest.junit.jupiter.api)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    systemProperty("micronaut.python.pool.enabled", "false")
}

// The samples of views written in Python need Pyronaut fixes that micronaut-core has not released yet
// (micronaut-core#13845, #13846, #13543 and others in progress): they are compiled with -Ppython-views, against
// a micronaut-core that has them, passed with --include-build
if (providers.gradleProperty("python-views").isPresent) {
    // one source root: the compiler names the packages of a second root after its directory
    val mergedPythonSources = tasks.register<Sync>("mergePythonViewSources") {
        from(layout.projectDirectory.dir("src/test/python"))
        from(layout.projectDirectory.dir("src/test/python-views"))
        into(layout.buildDirectory.dir("python-views-sources"))
    }
    tasks.named<io.micronaut.build.python.PythonCompile>("compileTestPython") {
        dependsOn(mergedPythonSources)
        source.setFrom(mergedPythonSources.map { it.destinationDir })
    }
}
