plugins {
    id("java-library")
    id("io.micronaut.build.internal.vaadin-tests")
    // builds the production frontend bundle of the application, as in any Vaadin application
    id("com.vaadin") version "25.3.1"
}

// The browser tests of test-suite-e2e against a production build: Vaadin's Gradle plugin builds the
// frontend bundle from the views of the application, which therefore live in the main source set here
val e2eTests = project(":test-suite-e2e").file("src/test")
sourceSets {
    named("main") {
        java {
            srcDir(File(e2eTests, "java"))
            include("**/app/**")
        }
    }
    named("test") {
        java {
            srcDir(File(e2eTests, "java"))
            include("**/BrowserTest.java")
        }
        resources.srcDir(File(e2eTests, "resources"))
    }
}

vaadin {
    productionMode = true
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautVaadinProcessor)
    implementation(projects.micronautVaadinServlet)
    implementation(mnServlet.micronaut.http.server.jetty)
    implementation(mnServlet.micronaut.servlet.websocket)
    implementation(libs.jetty.ee10.websocket.jakarta.server)
    implementation(mnSerde.micronaut.serde.jackson)
    implementation(libs.vaadin.core.components)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(libs.playwright)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks.withType<Test>().configureEach {
    // Vaadin's plugin writes the production bundle, and the token file that turns production mode on, into
    // the jar of the application: the tests run the jar, as a deployed application does
    val jar = tasks.named<Jar>("jar")
    dependsOn(jar)
    classpath = files(jar) + classpath.minus(sourceSets.main.get().output)
}
