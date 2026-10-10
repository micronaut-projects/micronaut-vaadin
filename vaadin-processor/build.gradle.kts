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
