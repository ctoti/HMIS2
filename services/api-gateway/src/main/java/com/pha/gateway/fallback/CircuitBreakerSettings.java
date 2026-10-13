package com.pha.gateway.fallback;

import com.pha.gateway.PhaGatewayProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerSettings {

    // Resilience4j's 1-second default time limit is too short for backends reached over provincial links.
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreaker(PhaGatewayProperties properties) {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(properties.backendTimeout()).build())
                .build());
    }
}
