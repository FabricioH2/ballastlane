package com.genai.tasks.task.domain;

import java.util.List;

/**
 * Thrown when a task would violate one of its invariants. Carries every violation found, not just the first.
 */
public class TaskValidationException extends RuntimeException {

    private final List<Violation> violations;

    public TaskValidationException(List<Violation> violations) {
        super("Invalid task: " + violations);
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }

    public record Violation(String field, String message) {
    }
}
