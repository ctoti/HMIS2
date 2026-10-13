package com.pha.gateway;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway settings that are not route definitions.
 *
 * @param allowedOrigins browser origins allowed to call the API (the web portal)
 * @param backendTimeout how long a backend call may take before the circuit breaker treats it as failed
 */
@ConfigurationProperties("pha.gateway")
public record PhaGatewayProperties(List<String> allowedOrigins, Duration backendTimeout) {

    public PhaGatewayProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        backendTimeout = backendTimeout == null ? Duration.ofSeconds(10) : backendTimeout;
    }
}
