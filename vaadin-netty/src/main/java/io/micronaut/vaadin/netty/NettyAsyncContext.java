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
package io.micronaut.vaadin.netty;

import io.micronaut.scheduling.TaskScheduler;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

/**
 * The asynchronous context of a request on Netty: the response is sent when {@link #complete()} is
 * called, or when the timeout expires. Atmosphere uses it for push over long polling: the request
 * waits, without holding a thread, until there is something to push.
 *
 * @author Graeme Rocher
 * @since 1.0.0
 */
final class NettyAsyncContext implements AsyncContext {

    private static final Logger LOG = LoggerFactory.getLogger(NettyAsyncContext.class);
    private static final long DEFAULT_TIMEOUT = 30_000;

    private final NettyHttpServletRequest request;
    private final NettyHttpServletResponse response;
    private final TaskScheduler scheduler;
    private final Executor executor;
    private final CompletableFuture<Void> completion = new CompletableFuture<>();
    private final List<AsyncListener> listeners = new CopyOnWriteArrayList<>();
    private volatile long timeout = DEFAULT_TIMEOUT;
    private volatile @Nullable ScheduledFuture<?> timeoutTask;

    NettyAsyncContext(NettyHttpServletRequest request,
                      NettyHttpServletResponse response,
                      TaskScheduler scheduler,
                      Executor executor) {
        this.request = request;
        this.response = response;
        this.scheduler = scheduler;
        this.executor = executor;
    }

    /**
     * Starts the timeout, once the servlet returned from serving the request.
     *
     * @return Completes when the response can be sent
     */
    CompletableFuture<Void> whenComplete() {
        if (timeout > 0 && !completion.isDone()) {
            timeoutTask = scheduler.schedule(Duration.ofMillis(timeout), this::timeout);
        }
        return completion;
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return true;
    }

    @Override
    public void dispatch() {
        throw new UnsupportedOperationException("Dispatching is not supported by Vaadin on the Netty server of Micronaut");
    }

    @Override
    public void dispatch(String path) {
        dispatch();
    }

    @Override
    public void dispatch(ServletContext context, String path) {
        dispatch();
    }

    @Override
    public void complete() {
        if (completion.isDone()) {
            return;
        }
        ScheduledFuture<?> task = timeoutTask;
        if (task != null) {
            task.cancel(false);
        }
        for (AsyncListener listener : listeners) {
            try {
                listener.onComplete(new AsyncEvent(this, request, response));
            } catch (IOException | RuntimeException e) {
                LOG.debug("An asynchronous listener failed on completion", e);
            }
        }
        completion.complete(null);
    }

    @Override
    public void start(Runnable runnable) {
        executor.execute(runnable);
    }

    @Override
    public void addListener(AsyncListener listener) {
        listeners.add(listener);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        listeners.add(listener);
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) {
        throw new UnsupportedOperationException("Creating listeners is not supported by Vaadin on the Netty server of Micronaut");
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    private void timeout() {
        for (AsyncListener listener : listeners) {
            try {
                listener.onTimeout(new AsyncEvent(this, request, response));
            } catch (IOException | RuntimeException e) {
                LOG.debug("An asynchronous listener failed on timeout", e);
            }
        }
        complete();
    }
}
