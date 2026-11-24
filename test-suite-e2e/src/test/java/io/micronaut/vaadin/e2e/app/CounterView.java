package io.micronaut.vaadin.e2e.app;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class CounterView extends VerticalLayout {

    public CounterView(GreetingService greetingService, ClickCounter counter) {
        Span greeting = new Span(greetingService.greet("from Vaadin"));
        greeting.setId("greeting");
        Span count = new Span("0");
        count.setId("count");
        Button button = new Button("Count", event -> count.setText(String.valueOf(counter.increment())));
        button.setId("count-button");
        add(greeting, button, count);
    }
}
