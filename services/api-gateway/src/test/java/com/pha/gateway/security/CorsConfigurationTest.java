package com.pha.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pha.gateway.PhaGatewayProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;

class CorsConfigurationTest {

    private final PhaGatewayProperties properties =
            new PhaGatewayProperties(List.of("http://localhost:5173"), Duration.ofSeconds(10));

    @Test
    void acceptsPreflightFromAllowedOrigin() {
        MockServerWebExchange exchange = preflightFrom("http://localhost:5173");
        CorsConfiguration config = SecurityConfig.corsConfigurationSource(properties).getCorsConfiguration(exchange);

        boolean accepted = new DefaultCorsProcessor().process(config, exchange);

        assertThat(accepted).isTrue();
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }

    @Test
    void rejectsPreflightFromOtherOrigin() {
        MockServerWebExchange exchange = preflightFrom("https://evil.example");
        CorsConfiguration config = SecurityConfig.corsConfigurationSource(properties).getCorsConfiguration(exchange);

        assertThat(new DefaultCorsProcessor().process(config, exchange)).isFalse();
    }

    private static MockServerWebExchange preflightFrom(String origin) {
        return MockServerWebExchange.from(MockServerHttpRequest.options("http://gateway.local/api/v1/hr/ping")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()));
    }
}
