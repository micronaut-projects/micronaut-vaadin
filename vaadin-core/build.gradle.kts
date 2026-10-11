plugins {
    id("io.micronaut.build.internal.vaadin-module")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)

    api(mn.micronaut.context)
    api(platform(libs.boms.vaadin.flow))
    api(libs.vaadin.flow.server)
    // Vaadin declares the servlet API as provided: the Netty runtime has no servlet container to provide it
    api(mnServlet.servlet.api)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(platform(libs.boms.vaadin.browserless.test))
    testImplementation(libs.vaadin.browserless.test.shared)
    testImplementation(libs.vaadin.flow.html.components)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
    testRuntimeOnly(mnLogging.logback.classic)
}

micronautBuild {
    // No released version to compare against before 1.0.0
    binaryCompatibility.enabledAfter("1.0.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
