plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
    id("org.graalvm.buildtools.native")
}

// The browser test application as a native executable on Netty, in production mode. The views, the
// production bundle and the token file that turns production mode on come from the jar of
// test-suite-production, so that Vaadin's Gradle plugin builds the bundle once. The browser tests run on
// the JVM and drive the executable
dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    implementation(projects.testSuiteProduction) {
        // only its jar: the servlet runtime it runs on is replaced with Netty
        isTransitive = false
    }
    implementation(projects.micronautVaadinNetty)
    implementation(mnSerde.micronaut.serde.jackson)
    implementation(libs.vaadin.core.components)
    runtimeOnly(mnLogging.logback.classic)

    testImplementation(libs.playwright)
    testImplementation(mnTest.junit.jupiter.api)
}

graalvmNative {
    toolchainDetection = false
    binaries {
        named("main") {
            mainClass = "io.micronaut.vaadin.nativeimage.Application"
            // an executable, not the shared library the plugin builds for a java-library project
            sharedLibrary = false
            buildArgs.add("-H:+ReportExceptionStackTraces")
            resources.autodetect()
        }
    }
}
tasks.named<Test>("test") {
    // the tests drive the native executable: nativeBrowserTest builds it, then runs them
    enabled = false
}

val nativeBrowserTest by tasks.registering(Test::class) {
    description = "Runs the browser tests against the native executable"
    group = "verification"
    val nativeCompile = tasks.named("nativeCompile")
    dependsOn(nativeCompile)
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    systemProperty("native.executable", layout.buildDirectory.file("native/nativeCompile/test-suite-native").get().asFile.absolutePath)
}
