package io.micronaut.vaadin.docs.views

// tag::view[]
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route

@Route("greeting") // <1>
class GreetingView(greetingService: GreetingService, counter: VisitCounter) : VerticalLayout() { // <2>

    init {
        val name = TextField("Name")
        val greeting = Span()
        val greet = Button("Greet") {
            counter.increment()
            greeting.text = greetingService.greet(name.value)
        }
        add(name, greet, greeting)
    }
}
// end::view[]
