package com.pha.hr.security;

import com.pha.hr.shared.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Returns RFC 7807 bodies for 401 and 403 raised by the security filter chain, instead of empty responses.
 */
@Component
public class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    public ProblemDetailsSecurityHandler(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, Problems.of(HttpStatus.UNAUTHORIZED, Problems.UNAUTHORIZED_TYPE, "Authentication required",
                "Send a valid bearer token from the PHA sign-in service."));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
            throws IOException {
        write(response, Problems.forbidden());
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), fields(problem));
    }

    // Flattened by hand: a plain JsonMapper would nest custom properties instead of following RFC 7807.
    static Map<String, Object> fields(ProblemDetail problem) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("type", problem.getType().toString());
        fields.put("title", problem.getTitle());
        fields.put("status", problem.getStatus());
        fields.put("detail", problem.getDetail());
        if (problem.getProperties() != null) {
            fields.putAll(problem.getProperties());
        }
        return fields;
    }
}
