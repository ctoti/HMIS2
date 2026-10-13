package com.pha.gateway.security;

import com.pha.gateway.web.ProblemResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Returns RFC 7807 bodies for 401 and 403 instead of Spring Security's empty responses.
 */
@Component
public class ProblemDetailsSecurityHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    static final String UNAUTHORIZED_TYPE = "urn:pha:problem:unauthorized";
    static final String FORBIDDEN_TYPE = "urn:pha:problem:forbidden";

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return ProblemResponses.write(exchange, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_TYPE,
                "Authentication required", "Send a valid bearer token from the PHA sign-in service.");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return ProblemResponses.write(exchange, HttpStatus.FORBIDDEN, FORBIDDEN_TYPE,
                "Access denied", "Your account does not have access to this resource.");
    }
}
