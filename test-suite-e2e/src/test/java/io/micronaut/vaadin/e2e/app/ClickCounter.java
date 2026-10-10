package io.micronaut.vaadin.e2e.app;

import io.micronaut.vaadin.annotation.UIScope;

@UIScope
public class ClickCounter {
    private int count;

    public int increment() {
        return ++count;
    }
}
