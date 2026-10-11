plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
}

// A Vaadin add-on, as a third-party jar: a component with a frontend module of its own, compiled without
// Micronaut's annotation processors, so that it is not in the compile-time index of an application using it
dependencies {
    api(platform(libs.boms.vaadin.flow))
    api(libs.vaadin.flow.server)
}
