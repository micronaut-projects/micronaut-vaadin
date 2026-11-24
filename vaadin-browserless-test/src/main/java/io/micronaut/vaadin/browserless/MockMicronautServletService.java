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
package io.micronaut.vaadin.browserless;

import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockVaadinSession;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import io.micronaut.context.BeanContext;
import io.micronaut.vaadin.MicronautVaadinServletService;

import java.io.Serial;

/**
 * A {@link MicronautVaadinServletService} for browserless tests: it creates mock sessions and disables
 * push.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MockMicronautServletService extends MicronautVaadinServletService {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient UIFactory uiFactory;

    /**
     * @param servlet                 The servlet
     * @param deploymentConfiguration The deployment configuration
     * @param beanContext             The bean context
     * @param uiFactory               Creates the UIs of the test
     */
    public MockMicronautServletService(VaadinServlet servlet,
                                       DeploymentConfiguration deploymentConfiguration,
                                       BeanContext beanContext,
                                       UIFactory uiFactory) {
        super(servlet, deploymentConfiguration, beanContext);
        this.uiFactory = uiFactory;
    }

    @Override
    protected boolean isAtmosphereAvailable() {
        return false;
    }

    @Override
    public String getMainDivId(VaadinSession session, VaadinRequest request) {
        return "ROOT-1";
    }

    @Override
    protected VaadinSession createVaadinSession(VaadinRequest request) {
        return new MockVaadinSession(this, uiFactory);
    }
}
