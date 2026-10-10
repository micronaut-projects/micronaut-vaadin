package io.micronaut.vaadin.devmode.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class HelloView extends VerticalLayout {
    public HelloView(GreetingService greetingService) {
        add(new Span(greetingService.greet("from Vaadin")));
    }
}
