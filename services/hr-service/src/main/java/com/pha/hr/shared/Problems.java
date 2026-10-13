package com.pha.hr.shared;

import com.pha.common.web.CorrelationIdFilter;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds RFC 7807 bodies with the same shape as {@code common-java}'s advice, including the correlation ID.
 */
public final class Problems {

    public static final String UNAUTHORIZED_TYPE = "urn:pha:problem:unauthorized";
    public static final String FORBIDDEN_TYPE = "urn:pha:problem:forbidden";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }

    public static ProblemDetail forbidden() {
        return of(HttpStatus.FORBIDDEN, FORBIDDEN_TYPE, "Access denied",
                "Your account does not have access to this resource.");
    }
}
