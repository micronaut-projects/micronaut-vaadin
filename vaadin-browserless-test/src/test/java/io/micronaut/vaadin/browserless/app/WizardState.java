package io.micronaut.vaadin.browserless.app;

import io.micronaut.vaadin.annotation.RouteScope;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.atomic.AtomicInteger;

@RouteScope
public class WizardState {
    public static final AtomicInteger DESTROYED = new AtomicInteger();

    @PreDestroy
    void destroy() {
        DESTROYED.incrementAndGet();
    }
}
