package com.genai.tasks.shared.web;

/**
 * One invalid field in a 400 ProblemDetail, exposed under the {@code errors} property.
 */
public record FieldViolation(String field, String message) {
}
