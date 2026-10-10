package com.ballastlane.blaexcercise.web.errors;

import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.exception.InvalidPokemonIdentifierException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid pagination", ex);
    }

    @ExceptionHandler(InvalidPokemonIdentifierException.class)
    public ProblemDetail handleInvalidPokemonIdentifier(InvalidPokemonIdentifierException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid pokemon identifier", ex);
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    public ProblemDetail handlePokemonNotFound(PokemonNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Pokemon not found", ex);
    }

    private static ProblemDetail problem(HttpStatus status, String title, RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
