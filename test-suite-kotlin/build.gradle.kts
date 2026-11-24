plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
    id("io.micronaut.build.internal.vaadin-tests")
}

dependencies {
    kspTest(mn.micronaut.inject.kotlin)
    kspTest(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinCore)
    testImplementation(projects.micronautVaadinSecurity)
    testImplementation(projects.micronautVaadinBrowserlessTest)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}
