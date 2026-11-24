package io.micronaut.vaadin.e2e.app;

import jakarta.inject.Singleton;

@Singleton
public class GreetingService {
    public String greet(String name) {
        return "Hello " + name;
    }
}
