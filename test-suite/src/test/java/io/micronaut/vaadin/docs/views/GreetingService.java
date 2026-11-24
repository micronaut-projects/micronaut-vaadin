package io.micronaut.vaadin.docs.views;

// tag::service[]
import jakarta.inject.Singleton;

@Singleton
public class GreetingService {

    public String greet(String name) {
        return "Hello " + name;
    }
}
// end::service[]
