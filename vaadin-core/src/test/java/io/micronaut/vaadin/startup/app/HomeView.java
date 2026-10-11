package io.micronaut.vaadin.startup.app;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@Route("")
@RouteAlias("home")
public class HomeView extends Div {
    public HomeView(Clock clock) {
    }
}
