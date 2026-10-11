package io.micronaut.vaadin.docs.security

import com.vaadin.flow.component.login.LoginForm
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.vaadin.browserless.MicronautBrowserlessTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest
@Property(name = "vaadin.security.enabled", value = "true")
@Property(name = "vaadin.security.login-view", value = "/login")
class AccessControlTest : MicronautBrowserlessTest() {

    @Test
    fun anAnonymousVisitorOfAnAdminViewSignsInFirst() {
        navigate("admin", LoginView::class.java)

        val form = `$`(LoginForm::class.java).single()
        assertEquals("login", form.action)
        assertFalse(form.isError)
    }

    @Test
    fun theLoginViewShowsAFailedSignIn() {
        navigate("login?error", LoginView::class.java)

        assertTrue(`$`(LoginForm::class.java).single().isError)
    }
}
