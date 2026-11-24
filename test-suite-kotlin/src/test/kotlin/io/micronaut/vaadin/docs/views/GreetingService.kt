package io.micronaut.vaadin.docs.views

// tag::service[]
import jakarta.inject.Singleton

@Singleton
open class GreetingService {

    fun greet(name: String) = "Hello $name"
}
// end::service[]
