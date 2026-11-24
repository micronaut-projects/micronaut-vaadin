package io.micronaut.vaadin.browserless.app;

import io.micronaut.vaadin.annotation.VaadinSessionScope;

import java.util.UUID;

@VaadinSessionScope
public class SessionState {
    private final String id = UUID.randomUUID().toString();

    public String getId() {
        return id;
    }
}
