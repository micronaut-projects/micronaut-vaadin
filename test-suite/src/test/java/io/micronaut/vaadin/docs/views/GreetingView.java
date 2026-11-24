package io.micronaut.vaadin.docs.views;

// tag::view[]
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("greeting") // <1>
public class GreetingView extends VerticalLayout {

    public GreetingView(GreetingService greetingService, VisitCounter counter) { // <2>
        TextField name = new TextField("Name");
        Span greeting = new Span();
        Button greet = new Button("Greet", event -> {
            counter.increment();
            greeting.setText(greetingService.greet(name.getValue()));
        });
        add(name, greet, greeting);
    }
}
// end::view[]
