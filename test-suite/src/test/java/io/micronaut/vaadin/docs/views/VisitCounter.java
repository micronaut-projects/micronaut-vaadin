package io.micronaut.vaadin.docs.views;

// tag::counter[]
import io.micronaut.vaadin.annotation.UIScope;

@UIScope // <1>
public class VisitCounter {

    private int count;

    public int increment() {
        return ++count;
    }
}
// end::counter[]
