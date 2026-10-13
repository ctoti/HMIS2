package com.pha.gateway.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Gives every request a correlation ID before security runs, so rejected requests carry one too,
 * and forwards it to the backend service.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    public static final String ATTRIBUTE = CorrelationIdWebFilter.class.getName() + ".id";

    // Same rule as common-java's servlet filter: keeps header injection and arbitrary caller text out.
    private static final Pattern ACCEPTED = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = resolve(exchange.getRequest().getHeaders().getFirst(HEADER));
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        exchange.getAttributes().put(ATTRIBUTE, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    public static String of(ServerWebExchange exchange) {
        Object id = exchange.getAttribute(ATTRIBUTE);
        return id instanceof String value ? value : "";
    }

    static String resolve(String incoming) {
        if (incoming != null && ACCEPTED.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
