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
