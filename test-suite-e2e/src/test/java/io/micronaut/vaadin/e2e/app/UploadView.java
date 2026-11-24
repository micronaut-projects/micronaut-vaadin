package io.micronaut.vaadin.e2e.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;

import java.nio.charset.StandardCharsets;

/**
 * Shows the name and content of an uploaded file.
 */
@Route("upload")
public class UploadView extends VerticalLayout {

    public UploadView() {
        Span result = new Span("none");
        result.setId("result");
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) ->
            getUI().ifPresent(ui -> ui.access(() ->
                result.setText(metadata.fileName() + ": " + new String(data, StandardCharsets.UTF_8))))));
        upload.setId("upload");
        add(upload, result);
    }
}
