package io.micronaut.vaadin.docs.views

// tag::counter[]
import io.micronaut.vaadin.annotation.UIScope

@UIScope // <1>
open class VisitCounter {

    private var count = 0

    fun increment() = ++count
}
// end::counter[]
