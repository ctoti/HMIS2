package com.pha.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class ProblemResponsesTest {

    @Test
    void writesAnRfc7807Body() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        exchange.getAttributes().put(CorrelationIdWebFilter.ATTRIBUTE, "corr-12345678");

        ProblemResponses.write(exchange, HttpStatus.FORBIDDEN, "urn:x", "Title", "Detail").block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo(
                "{\"type\":\"urn:x\",\"title\":\"Title\",\"status\":403,\"detail\":\"Detail\",\"correlationId\":\"corr-12345678\"}");
    }

    @Test
    void leavesCommittedResponsesAlone() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        exchange.getResponse().setComplete().block();

        ProblemResponses.write(exchange, HttpStatus.FORBIDDEN, "urn:x", "Title", "Detail").block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }
}
