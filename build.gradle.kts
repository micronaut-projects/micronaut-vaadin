plugins {
    id("io.micronaut.build.internal.parent")
}

repositories {
    // Micronaut 5.3 is unreleased: the code coverage aggregation resolves the modules' dependencies
    // here, so the root project needs the snapshots repository too (see vaadin-base).
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
}

// The test suites exercise the modules end to end (browsers, HTTP, servlet containers, security): their test runs
// count toward the coverage of the modules' classes in the aggregate report that SonarCloud reads
dependencies {
    subprojects.filter { it.name.startsWith("test-suite") }.forEach { "jacocoAggregation"(it) }
}
