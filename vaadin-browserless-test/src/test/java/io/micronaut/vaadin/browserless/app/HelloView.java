package io.micronaut.vaadin.browserless.app;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class HelloView extends VerticalLayout {
    private final UiCounter counter;
    private final SessionState session;
    private final Span greeting;
    private final Span count = new Span("0");

    public HelloView(GreetingService greetingService, UiCounter counter, SessionState session) {
        this.counter = counter;
        this.session = session;
        this.greeting = new Span(greetingService.greet("Micronaut"));
        Button button = new Button("Count", event -> count.setText(String.valueOf(counter.increment())));
        add(greeting, button, count);
    }

    public UiCounter getCounter() {
        return counter;
    }

    public SessionState getSession() {
        return session;
    }

    public Span getGreeting() {
        return greeting;
    }

    public Span getCount() {
        return count;
    }
}
