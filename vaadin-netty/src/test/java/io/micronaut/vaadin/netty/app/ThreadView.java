package io.micronaut.vaadin.netty.app;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import io.netty.util.concurrent.FastThreadLocalThread;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records the threads that create it: Vaadin must never run application code on a Netty event loop.
 */
@Route("thread")
public class ThreadView extends Span {

    public static final Set<String> EVENT_LOOP_THREADS = ConcurrentHashMap.newKeySet();

    public ThreadView() {
        Thread thread = Thread.currentThread();
        // the threads of Netty's event loops are FastThreadLocalThreads
        if (thread instanceof FastThreadLocalThread) {
            EVENT_LOOP_THREADS.add(thread.getName());
        }
        setText("Created on " + (thread.isVirtual() ? "a virtual thread" : thread.getName()));
    }
}
