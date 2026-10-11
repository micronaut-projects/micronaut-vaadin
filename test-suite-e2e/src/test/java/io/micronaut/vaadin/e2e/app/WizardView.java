package io.micronaut.vaadin.e2e.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Shows which instance of its route scoped draft it has.
 */
@Route("wizard")
public class WizardView extends VerticalLayout {

    public WizardView(WizardDraft draft) {
        Span instance = new Span(String.valueOf(draft.getInstance()));
        instance.setId("draft");
        add(instance);
    }
}
