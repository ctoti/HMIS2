package com.pha.hr.shared;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles the HR-specific and security exceptions before {@code common-java}'s catch-all advice turns them into 500s.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HrProblemAdvice {

    static final String CONFLICT_TYPE_PREFIX = "urn:pha:problem:hr:";
    static final String DATA_CONFLICT_TYPE = "urn:pha:problem:conflict";
    static final String BAD_SORT_TYPE = "urn:pha:problem:invalid-sort";

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException ex) {
        return Problems.forbidden();
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return Problems.of(HttpStatus.CONFLICT, CONFLICT_TYPE_PREFIX + ex.reason(), "Request conflicts with current state",
                ex.getMessage());
    }

    // Reached only when two writers race past the service's own checks; the database or version check wins.
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ProblemDetail dataConflict(RuntimeException ex) {
        return Problems.of(HttpStatus.CONFLICT, DATA_CONFLICT_TYPE, "Request conflicts with current state",
                "The change conflicts with a record saved at the same time. Reload and try again.");
    }

    @ExceptionHandler(SortableFields.InvalidSortException.class)
    ProblemDetail badSort(SortableFields.InvalidSortException ex) {
        return Problems.of(HttpStatus.BAD_REQUEST, BAD_SORT_TYPE, "Invalid sort", ex.getMessage());
    }
}
