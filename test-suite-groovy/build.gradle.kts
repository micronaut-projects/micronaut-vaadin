plugins {
    id("groovy")
    id("io.micronaut.build.internal.vaadin-tests")
}

dependencies {
    testCompileOnly(mn.micronaut.inject.groovy)
    testCompileOnly(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinCore)
    testImplementation(projects.micronautVaadinBrowserlessTest)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.spock)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}
