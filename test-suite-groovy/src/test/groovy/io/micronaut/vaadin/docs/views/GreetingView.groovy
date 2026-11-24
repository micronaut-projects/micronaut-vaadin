package io.micronaut.vaadin.docs.views

// tag::view[]
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route

@Route("greeting") // <1>
class GreetingView extends VerticalLayout {

    GreetingView(GreetingService greetingService, VisitCounter counter) { // <2>
        TextField name = new TextField("Name")
        Span greeting = new Span()
        Button greet = new Button("Greet", { event ->
            counter.increment()
            greeting.text = greetingService.greet(name.value)
        })
        add(name, greet, greeting)
    }
}
// end::view[]
