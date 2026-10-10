package io.micronaut.vaadin.servlet.app;

import jakarta.inject.Singleton;

@Singleton
public class GreetingService {
    public String greet(String name) {
        return "Hello " + name;
    }
}
