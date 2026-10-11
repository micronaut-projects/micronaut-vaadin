plugins {
    id("io.micronaut.build.internal.vaadin-module")
}

dependencies {
    api(mn.micronaut.core.processor)
}

micronautBuild {
    // No released version to compare against before 1.0.0
    binaryCompatibility.enabledAfter("1.0.0")
}

dependencies {
    testImplementation(mn.micronaut.inject.java.test)
    testImplementation(platform(libs.boms.vaadin.flow))
    testImplementation(libs.vaadin.flow.server)
    testImplementation(mnTest.junit.jupiter.api)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
