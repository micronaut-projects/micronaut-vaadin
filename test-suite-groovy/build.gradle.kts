plugins {
    id("groovy")
    id("io.micronaut.build.internal.vaadin-tests")
}

dependencies {
    testCompileOnly(mn.micronaut.inject.groovy)
    testImplementation(projects.micronautVaadinCore)
    testImplementation(mnTest.micronaut.test.spock)
    testRuntimeOnly(mnLogging.logback.classic)
}
