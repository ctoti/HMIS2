package com.pha.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;

class ProblemDetailsSecurityHandlerTest {

    @Test
    void deniedAccessBecomesForbiddenProblem() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/hr/x"));

        new ProblemDetailsSecurityHandler().handle(exchange, new AccessDeniedException("no")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(ProblemDetailsSecurityHandler.FORBIDDEN_TYPE);
    }
}
