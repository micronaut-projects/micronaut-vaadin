package io.micronaut.vaadin.docs.views

// tag::service[]
import jakarta.inject.Singleton

@Singleton
class GreetingService {

    String greet(String name) {
        "Hello $name"
    }
}
// end::service[]
