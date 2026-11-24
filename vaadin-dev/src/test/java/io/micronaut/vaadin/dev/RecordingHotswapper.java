package io.micronaut.vaadin.dev;

import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.base.devserver.hotswap.HotswapClassEvent;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Records the classes that reach Flow's hotswap.
 */
public class RecordingHotswapper implements VaadinHotswapper {

    static final Set<String> REDEFINED = new CopyOnWriteArraySet<>();

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        if (event.isRedefined()) {
            event.getChangedClasses().forEach(type -> REDEFINED.add(type.getName()));
        }
    }
}
