plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinCore)
    testImplementation(projects.micronautVaadinBrowserlessTest)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}
