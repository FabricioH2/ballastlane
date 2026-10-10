package com.ballastlane.blaexcercise.web.errors;

import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.exception.InvalidPokemonIdentifierException;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.exception.PokemonCatalogUnavailableException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid pagination", ex.getMessage());
    }

    @ExceptionHandler(InvalidPokemonIdentifierException.class)
    public ProblemDetail handleInvalidPokemonIdentifier(InvalidPokemonIdentifierException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid pokemon identifier", ex.getMessage());
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    public ProblemDetail handlePokemonNotFound(PokemonNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Pokemon not found", ex.getMessage());
    }

    @ExceptionHandler(PokemonAlreadySyncedException.class)
    public ProblemDetail handlePokemonAlreadySynced(PokemonAlreadySyncedException ex) {
        return problem(HttpStatus.CONFLICT, "Pokemon already synced", ex.getMessage());
    }

    @ExceptionHandler(PokemonCatalogUnavailableException.class)
    public ProblemDetail handleCatalogUnavailable(PokemonCatalogUnavailableException ex) {
        log.warn("Pokemon catalog unavailable", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Pokemon catalog unavailable", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
