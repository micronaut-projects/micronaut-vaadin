package io.micronaut.vaadin.e2e.app;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Updates the page from a background thread: the update only reaches the browser by push.
 */
@Route("push")
public class PushView extends VerticalLayout {

    public PushView() {
        Span status = new Span("waiting");
        status.setId("status");
        Button start = new Button("Start", event -> {
            UI ui = event.getSource().getUI().orElseThrow();
            CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                .execute(() -> ui.access(() -> status.setText("pushed")));
        });
        start.setId("start");
        add(start, status);
    }
}
