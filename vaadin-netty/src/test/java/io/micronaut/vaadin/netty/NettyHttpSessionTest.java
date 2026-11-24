package io.micronaut.vaadin.netty;

import io.micronaut.session.Session;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The session of the servlet facade, a view of a session of Micronaut.
 */
class NettyHttpSessionTest {

    @Test
    void attributesAndLifetimeAreThoseOfTheMicronautSession() {
        TestSessionStore store = new TestSessionStore();
        Session micronaut = store.newSession();
        NettyServletContext context = new NettyServletContext(getClass().getClassLoader(),
            new NettyServletRegistration("vaadinServlet", NettyVaadinServlet.class.getName(), List.of("/*")));
        HttpSession session = new NettyHttpSession(micronaut, store, context);

        assertEquals(micronaut.getId(), session.getId());
        assertTrue(session.getCreationTime() > 0);
        assertTrue(session.getLastAccessedTime() > 0);
        assertEquals(context, session.getServletContext());
        session.setMaxInactiveInterval(60);
        assertEquals(60, session.getMaxInactiveInterval());

        session.setAttribute("one", 1);
        assertEquals(1, session.getAttribute("one"));
        assertTrue(Collections.list(session.getAttributeNames()).contains("one"));
        session.setAttribute("one", null);
        assertNull(session.getAttribute("one"));
        session.setAttribute("two", 2);
        session.removeAttribute("two");
        assertNull(session.getAttribute("two"));

        session.isNew();
        store.save(micronaut);
        session.invalidate();
        assertFalse(store.sessions.containsKey(micronaut.getId()));
    }
}
