package com.pha.gateway.fallback;

import com.pha.gateway.web.ProblemResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Target of the circuit breaker's forward: when a backend is down or too slow.
 */
@RestController
public class FallbackController {

    static final String UNAVAILABLE_TYPE = "urn:pha:problem:service-unavailable";

    @RequestMapping("/fallback/unavailable")
    public Mono<Void> unavailable(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().set("Retry-After", "30");
        return ProblemResponses.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_TYPE,
                "Service temporarily unavailable", "The service did not respond. Try again shortly.");
    }
}
