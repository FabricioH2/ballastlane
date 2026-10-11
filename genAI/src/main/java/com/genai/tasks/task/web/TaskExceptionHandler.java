package com.genai.tasks.task.web;

import com.genai.tasks.shared.web.FieldViolation;
import com.genai.tasks.shared.web.Problems;
import com.genai.tasks.task.application.TaskConflictException;
import com.genai.tasks.task.application.TaskNotFoundException;
import com.genai.tasks.task.domain.TaskValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class TaskExceptionHandler {

    /**
     * Same body whether the task is missing or belongs to someone else, so ids reveal nothing.
     */
    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Task not found");
    }

    @ExceptionHandler(TaskConflictException.class)
    ProblemDetail handleConflict(TaskConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The task was changed or deleted by another request. Reload it and try again.");
    }

    @ExceptionHandler(TaskValidationException.class)
    ProblemDetail handleValidation(TaskValidationException ex) {
        return Problems.badRequest("Task validation failed", ex.violations().stream()
                .map(violation -> new FieldViolation(violation.field(), violation.message()))
                .toList());
    }
}
