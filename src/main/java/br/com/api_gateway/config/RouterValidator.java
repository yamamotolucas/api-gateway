package br.com.api_gateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/identity-ms/v1/auth/",
            "/identity-ms/v1/barber/findAll",
            "/identity-ms/v1/barber/findById",
            "/booking-ms/v1/bookings/confirm/",
            "/booking-ms/v1/barber/services/list",
            "/booking-ms/test"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}


