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
package io.micronaut.vaadin;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * The Vaadin service of Micronaut: instantiates through {@link MicronautInstantiator}, registers the
 * {@link UIInitListener} beans and runs Vaadin's background tasks on the executor named
 * {@value #EXECUTOR_NAME} when one is configured.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
public class MicronautVaadinServletService extends VaadinServletService {

    /**
     * The name of the executor that runs Vaadin's background tasks, configured with
     * {@code micronaut.executors.vaadin}. Without it, Vaadin uses its own executor.
     */
    public static final String EXECUTOR_NAME = "vaadin";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String WEB_RESOURCES = "META-INF/resources";

    private final transient BeanContext beanContext;

    /**
     * @param servlet                 The servlet
     * @param deploymentConfiguration The deployment configuration
     * @param beanContext             The bean context
     */
    public MicronautVaadinServletService(VaadinServlet servlet,
                                         DeploymentConfiguration deploymentConfiguration,
                                         BeanContext beanContext) {
        super(servlet, deploymentConfiguration);
        this.beanContext = beanContext;
    }

    /**
     * @return The bean context
     */
    public BeanContext getBeanContext() {
        return beanContext;
    }

    @Override
    public void init() throws ServiceException {
        super.init();
        beanContext.getBeansOfType(UIInitListener.class).forEach(this::addUIInitListener);
    }

    @Override
    protected Optional<Instantiator> loadInstantiators() throws ServiceException {
        Optional<Instantiator> spiInstantiator = super.loadInstantiators();
        Collection<Instantiator> beans = beanContext.getBeansOfType(Instantiator.class);
        if (spiInstantiator.isPresent() && !beans.isEmpty()) {
            throw new ServiceException("Cannot initialize the Vaadin service: there is an instantiator registered with the Java SPI, "
                + spiInstantiator.get() + ", and instantiator beans: " + beans);
        }
        if (spiInstantiator.isPresent()) {
            return spiInstantiator;
        }
        if (beans.size() > 1) {
            throw new ServiceException("Cannot initialize the Vaadin service: there are several instantiator beans: " + beans);
        }
        return Optional.of(beans.isEmpty() ? new MicronautInstantiator(this, beanContext) : beans.iterator().next());
    }

    /**
     * Finds a static resource of Vaadin, such as the push client, also in the {@code META-INF/resources} of
     * the jars on the classpath: embedded servlet containers and Netty do not serve them as web resources.
     *
     * @param path The path of the resource, starting with {@code /}
     * @return The resource, or {@code null}
     */
    @Override
    public @Nullable URL getStaticResource(String path) {
        URL resource = super.getStaticResource(path);
        if (resource == null && path.startsWith("/") && !path.contains("..")) {
            resource = getClassLoader().getResource(WEB_RESOURCES + path);
        }
        return resource;
    }

    @Override
    protected Executor createDefaultExecutor() {
        return beanContext.findBean(Executor.class, Qualifiers.byName(EXECUTOR_NAME))
            .orElseGet(super::createDefaultExecutor);
    }
}
