package io.micronaut.vaadin.e2e.security.app;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class Users<B> implements HttpRequestAuthenticationProvider<B> {

    @Override
    public AuthenticationResponse authenticate(HttpRequest<B> requestContext, AuthenticationRequest<String, String> authRequest) {
        if (!"secret".equals(authRequest.getSecret())) {
            return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
        }
        return switch (authRequest.getIdentity()) {
            case "admin" -> AuthenticationResponse.success("admin", List.of("ADMIN"));
            case "user" -> AuthenticationResponse.success("user");
            default -> AuthenticationResponse.failure(AuthenticationFailureReason.USER_NOT_FOUND);
        };
    }
}
