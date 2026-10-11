package io.micronaut.vaadin.startup.app;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;

public class NotFoundView extends Div implements HasErrorParameter<NotFoundException> {
    public NotFoundView(Clock clock) {
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return 404;
    }
}
