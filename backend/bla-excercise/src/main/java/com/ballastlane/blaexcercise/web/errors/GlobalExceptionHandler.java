package com.ballastlane.blaexcercise.web.errors;

import com.ballastlane.blaexcercise.domain.exception.InvalidCredentialsException;
import com.ballastlane.blaexcercise.domain.exception.InvalidPaginationException;
import com.ballastlane.blaexcercise.domain.exception.InvalidPokemonIdentifierException;
import com.ballastlane.blaexcercise.domain.exception.LocalPokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.exception.PokemonAlreadySyncedException;
import com.ballastlane.blaexcercise.domain.exception.PokemonCatalogUnavailableException;
import com.ballastlane.blaexcercise.domain.exception.PokemonNotFoundException;
import com.ballastlane.blaexcercise.domain.exception.UsernameAlreadyTakenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @ExceptionHandler(LocalPokemonNotFoundException.class)
    public ProblemDetail handleLocalPokemonNotFound(LocalPokemonNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Local pokemon not found", ex.getMessage());
    }

    @ExceptionHandler(PokemonAlreadySyncedException.class)
    public ProblemDetail handlePokemonAlreadySynced(PokemonAlreadySyncedException ex) {
        return problem(HttpStatus.CONFLICT, "Pokemon already synced", ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ProblemDetail handleUsernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        return problem(HttpStatus.CONFLICT, "Username already taken", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", ex.getMessage());
    }

    /**
     * @Valid failed on a request body. The default answer only says "Invalid request content",
     * so we add which fields failed and why: {"errors": {"password": "size must be between 8 and 72"}}.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "One or more fields are invalid");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
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
