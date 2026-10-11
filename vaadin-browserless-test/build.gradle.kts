plugins {
    id("io.micronaut.build.internal.vaadin-module")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)

    api(projects.micronautVaadinCore)
    api(platform(libs.boms.vaadin.browserless.test))
    api(libs.vaadin.browserless.test.junit6)
    api(mnTest.micronaut.test.junit5)
    // The testers reference every free component; applications bring the ones they use
    compileOnly(libs.vaadin.core.components)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(libs.vaadin.core.components)
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
