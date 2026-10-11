package io.micronaut.vaadin.netty;

import io.micronaut.http.HttpRequest;
import io.micronaut.scheduling.ScheduledExecutorTaskScheduler;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The asynchronous context of a request, used by push over long polling.
 */
class NettyAsyncContextTest {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final NettyServletContext servletContext = new NettyServletContext(
        getClass().getClassLoader(), new NettyServletRegistration("vaadinServlet", NettyVaadinServlet.class.getName(), List.of("/*")));
    private final NettyHttpServletRequest request = new NettyHttpServletRequest(HttpRequest.GET("/push"),
        new ByteArrayInputStream(new byte[0]), servletContext, new TestSessionStore(), "", "/push");
    private final NettyHttpServletResponse response = new NettyHttpServletResponse();
    private final List<String> events = new CopyOnWriteArrayList<>();

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    private NettyAsyncContext asyncContext() {
        request.setAsyncContextFactory(() -> new NettyAsyncContext(request, response, new ScheduledExecutorTaskScheduler(executor), executor));
        return (NettyAsyncContext) request.startAsync();
    }

    @Test
    void aRequestIsOnlyAsynchronousWhenTheRuntimeSupportsIt() {
        assertFalse(request.isAsyncSupported());
        assertFalse(request.isAsyncStarted());
        assertThrows(IllegalStateException.class, request::startAsync);
        assertThrows(IllegalStateException.class, request::getAsyncContext);

        NettyAsyncContext asyncContext = asyncContext();
        assertTrue(request.isAsyncSupported());
        assertTrue(request.isAsyncStarted());
        assertSame(asyncContext, request.startAsync(request, response));
        assertSame(asyncContext, request.getAsyncContext());
        assertSame(asyncContext, request.asyncContext());
        assertSame(request, asyncContext.getRequest());
        assertSame(response, asyncContext.getResponse());
        assertTrue(asyncContext.hasOriginalRequestAndResponse());
    }

    @Test
    void completingSendsTheResponseOnce() throws Exception {
        NettyAsyncContext asyncContext = asyncContext();
        asyncContext.addListener(listener("first"));
        asyncContext.addListener(failingListener(), request, response);
        CompletableFuture<Void> done = asyncContext.whenComplete();
        assertFalse(done.isDone());

        asyncContext.complete();
        asyncContext.complete();
        done.get(5, TimeUnit.SECONDS);
        assertEquals(List.of("first complete"), events);
    }

    @Test
    void theTimeoutCompletesTheRequest() throws Exception {
        NettyAsyncContext asyncContext = asyncContext();
        asyncContext.setTimeout(10);
        assertEquals(10, asyncContext.getTimeout());
        asyncContext.addListener(listener("listener"));
        asyncContext.addListener(failingListener());

        asyncContext.whenComplete().get(5, TimeUnit.SECONDS);
        assertEquals(List.of("listener timeout", "listener complete"), events);
    }

    @Test
    void withoutATimeoutTheRequestWaitsForCompletion() {
        NettyAsyncContext asyncContext = asyncContext();
        asyncContext.setTimeout(0);
        CompletableFuture<Void> done = asyncContext.whenComplete();
        assertFalse(done.isDone());
        asyncContext.complete();
        assertTrue(done.isDone());
    }

    @Test
    void tasksRunOnTheExecutor() throws Exception {
        CompletableFuture<String> thread = new CompletableFuture<>();
        asyncContext().start(() -> thread.complete(Thread.currentThread().getName()));
        assertNotEquals(Thread.currentThread().getName(), thread.get(5, TimeUnit.SECONDS));
    }

    @Test
    void dispatchingIsNotSupported() {
        NettyAsyncContext asyncContext = asyncContext();
        assertThrows(UnsupportedOperationException.class, asyncContext::dispatch);
        assertThrows(UnsupportedOperationException.class, () -> asyncContext.dispatch("/other"));
        assertThrows(UnsupportedOperationException.class, () -> asyncContext.dispatch(servletContext, "/other"));
        assertThrows(UnsupportedOperationException.class, () -> asyncContext.createListener(AsyncListener.class));
    }

    private AsyncListener listener(String name) {
        return new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                events.add(name + " complete");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                events.add(name + " timeout");
            }

            @Override
            public void onError(AsyncEvent event) {
                events.add(name + " error");
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                events.add(name + " start");
            }
        };
    }

    private static AsyncListener failingListener() {
        return new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                throw new IOException("complete");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                throw new IllegalStateException("timeout");
            }

            @Override
            public void onError(AsyncEvent event) {
                // not called
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                // not called
            }
        };
    }
}
