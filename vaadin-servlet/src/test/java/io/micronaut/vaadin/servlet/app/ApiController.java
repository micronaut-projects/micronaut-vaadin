package io.micronaut.vaadin.servlet.app;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/api")
public class ApiController {
    private final GreetingService greetingService;

    public ApiController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @Get("/hello")
    public String hello() {
        return greetingService.greet("from Micronaut");
    }
}
