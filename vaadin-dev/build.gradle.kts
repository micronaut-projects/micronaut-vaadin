plugins {
    id("io.micronaut.build.internal.vaadin-module")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)

    api(projects.micronautVaadinCore)
    // the watch API and @DevelopmentActive of micronaut-dev live in micronaut-inject
    api(mn.micronaut.inject)
    // Flow's hotswap, present when Vaadin runs in development mode
    compileOnly(libs.vaadin.dev.server)
    // the HTTP sessions of the Netty runtime, carried over a restart when Vaadin's session serialization is on
    compileOnly(mnSession.micronaut.session)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation("io.micronaut:micronaut-dev-tck")
    testImplementation(mn.micronaut.inject.java)
    testImplementation(projects.micronautVaadinProcessor)
    testImplementation(projects.micronautVaadinNetty)
    testImplementation(mnSession.micronaut.session)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(libs.vaadin.dev.server)
    testImplementation(libs.vaadin.dev.bundle)
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(libs.playwright)
    // attaches micronaut-dev's agent to the test JVM, for edits applied in place
    testImplementation("net.bytebuddy:byte-buddy-agent:1.18.13")
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
    jvmArgs("-Djdk.attach.allowAttachSelf=true", "-XX:+EnableDynamicAgentLoading")
    // development mode writes generated frontend files into the project: keep them under build/
    val projectDir = layout.buildDirectory.dir("vaadin-project")
    doFirst {
        projectDir.get().asFile.mkdirs()
    }
    systemProperty("vaadin.project.basedir", projectDir.get().asFile.absolutePath)
}
