package io.micronaut.vaadin.browserless.app;

import io.micronaut.vaadin.annotation.UIScope;

@UIScope
public class UiCounter {
    private int count;

    public int increment() {
        return ++count;
    }

    public int getCount() {
        return count;
    }
}
