package com.pha.gateway.web;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Writes RFC 7807 bodies for responses the gateway produces itself.
 */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String type, String title, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        byte[] body = json(status, type, title, detail, CorrelationIdWebFilter.of(exchange)).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    // Every value is a constant from this module or a validated correlation ID, so no JSON escaping is needed.
    static String json(HttpStatus status, String type, String title, String detail, String correlationId) {
        return "{\"type\":\"%s\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"correlationId\":\"%s\"}"
                .formatted(type, title, status.value(), detail, correlationId);
    }
}
