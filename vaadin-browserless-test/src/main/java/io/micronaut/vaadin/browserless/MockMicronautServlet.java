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

import com.vaadin.browserless.internal.Routes;
import com.vaadin.browserless.internal.UIFactory;
import com.vaadin.browserless.mocks.MockVaadinHelper;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServletService;
import io.micronaut.context.ApplicationContext;
import io.micronaut.vaadin.MicronautVaadinServlet;
import jakarta.servlet.ServletException;

import java.io.Serial;

/**
 * A {@link MicronautVaadinServlet} for browserless tests: it registers the given routes and uses a
 * {@link MockMicronautServletService}.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MockMicronautServlet extends MicronautVaadinServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Routes routes;
    private final transient UIFactory uiFactory;

    /**
     * @param routes             The routes to register
     * @param applicationContext The application context
     * @param uiFactory          Creates the UIs of the test
     */
    public MockMicronautServlet(Routes routes, ApplicationContext applicationContext, UIFactory uiFactory) {
        super(applicationContext);
        this.routes = routes;
        this.uiFactory = uiFactory;
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration() throws ServletException {
        MockVaadinHelper.INSTANCE.mockFlowBuildInfo(this);
        return super.createDeploymentConfiguration();
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException {
        VaadinServletService service = new MockMicronautServletService(this, deploymentConfiguration, getApplicationContext(), uiFactory);
        service.init();
        routes.register(service.getContext());
        return service;
    }
}
