package com.pha.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @Test
    void keepsValidIncomingIdAndForwardsIt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/hr/x").header(CorrelationIdWebFilter.HEADER, "abc-12345678"));
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();

        filter.filter(exchange, e -> { seen.set(e); return Mono.empty(); }).block();

        assertThat(seen.get().getRequest().getHeaders().getFirst(CorrelationIdWebFilter.HEADER)).isEqualTo("abc-12345678");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER)).isEqualTo("abc-12345678");
        assertThat(CorrelationIdWebFilter.of(seen.get())).isEqualTo("abc-12345678");
    }

    @Test
    void replacesMissingOrMalformedIds() {
        assertThat(CorrelationIdWebFilter.resolve(null)).matches("[0-9a-f-]{36}");
        assertThat(CorrelationIdWebFilter.resolve("bad\r\nheader")).matches("[0-9a-f-]{36}");
        assertThat(CorrelationIdWebFilter.resolve("short")).matches("[0-9a-f-]{36}");
    }

    @Test
    void runsBeforeEverythingElse() {
        assertThat(filter.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void readsAnEmptyIdWhenTheFilterHasNotRun() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        assertThat(CorrelationIdWebFilter.of(exchange)).isEmpty();
    }
}
