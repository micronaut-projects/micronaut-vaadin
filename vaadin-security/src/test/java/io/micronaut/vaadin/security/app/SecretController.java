package io.micronaut.vaadin.security.app;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

@Controller("/api")
public class SecretController {

    @Get("/secret")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public String secret() {
        return "secret";
    }
}
