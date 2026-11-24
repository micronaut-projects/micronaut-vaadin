plugins {
    id("io.micronaut.build.internal.vaadin-module")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)

    api(projects.micronautVaadinCore)
    api(mn.micronaut.http.server.netty)
    // Vaadin keeps its state in the HTTP session
    api(mnSession.micronaut.session)
    // push over WebSockets
    api(mn.micronaut.websocket)
    // the servlet WebSocket API, which Vaadin's development server uses to proxy Vite: the Netty runtime serves
    // that proxy itself
    implementation(libs.jakarta.websocket.api)
    implementation(libs.jakarta.websocket.client.api)
    compileOnly(libs.vaadin.dev.server)
    // the push endpoints let Micronaut Security, when present, hand their access control to Vaadin
    compileOnly(mnSecurity.micronaut.security.annotations)
    compileOnly(mn.graalvm.nativeimage)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(projects.micronautVaadinProcessor)
    testImplementation(mn.micronaut.http.client.jdk)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(libs.vaadin.core.components)
    testImplementation(mnTest.micronaut.test.junit5)
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
