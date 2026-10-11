package io.micronaut.vaadin.browserless.app;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import io.micronaut.vaadin.annotation.RouteScopeOwner;

@Layout
public class MainLayout extends Div implements RouterLayout {
    private final LayoutState state;

    public MainLayout(@RouteScopeOwner(MainLayout.class) LayoutState state) {
        this.state = state;
    }

    public LayoutState getState() {
        return state;
    }
}
