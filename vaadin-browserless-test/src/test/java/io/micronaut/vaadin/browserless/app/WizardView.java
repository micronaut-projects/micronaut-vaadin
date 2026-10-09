package io.micronaut.vaadin.browserless.app;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("wizard")
public class WizardView extends Div {
    private final WizardState state;

    public WizardView(WizardState state) {
        this.state = state;
    }

    public WizardState getState() {
        return state;
    }
}
