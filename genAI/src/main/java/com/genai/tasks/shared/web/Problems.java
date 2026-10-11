package com.genai.tasks.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;

/**
 * Factory for the RFC 7807 bodies this API returns.
 */
public final class Problems {

    public static final String ERRORS_PROPERTY = "errors";

    private Problems() {
    }

    public static ProblemDetail badRequest(String detail, List<FieldViolation> violations) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        return withViolations(problem, violations);
    }

    public static ProblemDetail withViolations(ProblemDetail problem, List<FieldViolation> violations) {
        if (!violations.isEmpty()) {
            problem.setProperty(ERRORS_PROPERTY, List.copyOf(violations));
        }
        return problem;
    }
}
