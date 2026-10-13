package com.pha.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class UserOrIpKeyResolverTest {

    private final UserOrIpKeyResolver resolver = new UserOrIpKeyResolver();

    @Test
    void usesTheSignedInUser() {
        MockServerWebExchange exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("/"))
                .principal(() -> "user-123")
                .build();

        assertThat(resolver.resolve(exchange).block()).isEqualTo("user:user-123");
    }

    @Test
    void fallsBackToTheClientIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(new InetSocketAddress("10.1.2.3", 5555)));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:10.1.2.3");
    }

    @Test
    void groupsCallersWithNoKnownAddress() {
        MockServerWebExchange unresolved = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").remoteAddress(InetSocketAddress.createUnresolved("pha.example", 1)));
        MockServerWebExchange none = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        assertThat(resolver.resolve(unresolved).block()).isEqualTo("ip:unknown");
        assertThat(UserOrIpKeyResolver.clientIp(none)).isEqualTo("unknown");
    }
}
