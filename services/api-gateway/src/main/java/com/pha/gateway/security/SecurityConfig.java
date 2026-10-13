package com.pha.gateway.security;

import com.pha.gateway.PhaGatewayProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, ProblemDetailsSecurityHandler problems, PhaGatewayProperties properties) {
        return http
                // Bearer tokens are never sent automatically by browsers, so API calls need no CSRF token.
                // Requests that carry cookies still must, in case cookie-based sign-in is ever added.
                .csrf(csrf -> csrf
                        .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                                CsrfWebFilter.DEFAULT_CSRF_MATCHER, SecurityConfig::carriesCookies))
                        .accessDeniedHandler(problems))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // Passed explicitly: Security ignores a CorsConfigurationSource bean when the gateway defines another.
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .build();
    }

    static CorsConfigurationSource corsConfigurationSource(PhaGatewayProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
        cors.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After"));
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    static Mono<MatchResult> carriesCookies(ServerWebExchange exchange) {
        return exchange.getRequest().getCookies().isEmpty() ? MatchResult.notMatch() : MatchResult.match();
    }
}
