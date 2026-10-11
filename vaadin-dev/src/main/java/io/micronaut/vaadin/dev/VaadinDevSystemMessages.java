/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.vaadin.dev;

import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.DefaultSystemMessagesProvider;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

/**
 * A restart in development mode ends every Vaadin session: rather than tell the user that the session
 * expired, the browser reloads the page quietly, and the view comes back from the new generation. An
 * application that sets its own system messages keeps them.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
@Singleton
@DevelopmentActive
@Requires(property = "vaadin.dev.quiet-restart", notEquals = "false")
@Internal
final class VaadinDevSystemMessages implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        if (service.getSystemMessagesProvider() != DefaultSystemMessagesProvider.get()) {
            return;
        }
        CustomizedSystemMessages messages = new CustomizedSystemMessages();
        messages.setSessionExpiredNotificationEnabled(false);
        service.setSystemMessagesProvider(info -> messages);
    }
}
