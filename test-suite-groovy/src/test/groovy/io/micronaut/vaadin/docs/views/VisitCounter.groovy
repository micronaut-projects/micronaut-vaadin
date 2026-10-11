package io.micronaut.vaadin.docs.views

// tag::counter[]
import io.micronaut.vaadin.annotation.UIScope

@UIScope // <1>
class VisitCounter {

    private int count

    int increment() {
        ++count
    }
}
// end::counter[]
