package io.micronaut.vaadin.e2e.app;

import io.micronaut.vaadin.annotation.RouteScope;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lives as long as the wizard is in the navigation chain of its browser window, across reloads.
 */
@RouteScope
public class WizardDraft {

    private static final AtomicInteger INSTANCES = new AtomicInteger();

    private final int instance = INSTANCES.incrementAndGet();

    public int getInstance() {
        return instance;
    }
}
