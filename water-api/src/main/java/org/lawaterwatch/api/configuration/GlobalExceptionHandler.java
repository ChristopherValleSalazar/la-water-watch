package org.lawaterwatch.api.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.lawaterwatch.api.exception.PwsidNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

//Every error a controller throws ends up here, so the frontend always gets the same short JSON instead of a stack trace
//The full detail (and the stack trace for 5xx) goes to the server log
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //Long enough to say what broke, short enough not to dump internals on the frontend
    private static final int MAX_MESSAGE_LENGTH = 200;

    //Standard error body: status code, its name, what happened and when
    public record ApiError(int status, String error, String message, Instant timestamp) {}

    //429 says as little as possible, so someone probing our limits learns nothing about them
    public record RateLimitError(int status, String message) {}

    @ExceptionHandler(PwsidNotFoundException.class)
    public ResponseEntity<ApiError> pwsidNotFound(PwsidNotFoundException e, HttpServletRequest request) {
        log.warn("{} {} -> 404: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //Anything else: Spring's own exceptions carry their status, unknown ones are a 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> anyError(Exception e, HttpServletRequest request) {
        HttpStatusCode status = statusOf(e);
        String where = request.getMethod() + " " + request.getRequestURI();

        if (status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            log.warn("{} -> 429 from {}: {}", where, request.getRemoteAddr(), e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new RateLimitError(429, "Too many requests, please try again later."));
        }

        String message = messageFor(e, request);
        if (status.is5xxServerError()) {
            //Full stack trace only in the log
            log.error("{} -> {}: {}", where, status.value(), message, e);
        } else {
            log.warn("{} -> {}: {}", where, status.value(), message);
        }
        return body(status, message);
    }

    private static HttpStatusCode statusOf(Exception e) {
        //Spring MVC exceptions and ResponseStatusException know their own status
        if (e instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode();
        }
        //Bad input Spring would otherwise map to 400 after us: unparsable body, wrong type in a path/query param
        if (e instanceof HttpMessageNotReadableException || e instanceof TypeMismatchException
                || e instanceof BindException) {
            return HttpStatus.BAD_REQUEST;
        }
        //Any exception class annotated with @ResponseStatus keeps its status
        ResponseStatus annotated = AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
        if (annotated != null) {
            return annotated.code();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    //A plain sentence that says what went wrong and, where it helps, how to fix the request
    private static String messageFor(Exception e, HttpServletRequest request) {
        return switch (e) {
            case MethodArgumentTypeMismatchException mismatch -> typeMismatch(mismatch);
            case HttpMessageNotReadableException ignored -> "Request body is missing or is not valid JSON";
            case NoResourceFoundException ignored ->
                    "No endpoint at " + request.getMethod() + " " + request.getRequestURI();
            case ResponseStatusException statusException when statusException.getReason() != null ->
                    statusException.getReason();
            case ErrorResponse errorResponse when errorResponse.getBody().getDetail() != null ->
                    errorResponse.getBody().getDetail();
            default -> rootCause(e);
        };
    }

    //e.g. sort=NOPE -> "Invalid value 'NOPE' for 'sort'. Allowed values: NONE, WATERSCORE, ..."
    private static String typeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value '" + e.getValue() + "' for '" + e.getName() + "'";
        Class<?> type = e.getRequiredType();
        if (type != null && type.isEnum()) {
            return message + ". Allowed values: " + Arrays.stream(type.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        return type == null ? message : message + ", expected " + type.getSimpleName();
    }

    //The last link of the cause chain is where it actually broke, e.g. "SQLiteException: database is locked"
    private static String rootCause(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage() == null ? "" : ": " + root.getMessage().strip();
        String message = root.getClass().getSimpleName() + detail;
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    private static ResponseEntity<ApiError> body(HttpStatusCode status, String message) {
        HttpStatus known = HttpStatus.resolve(status.value());
        String error = known == null ? "Error" : known.getReasonPhrase();
        return ResponseEntity.status(status).body(new ApiError(status.value(), error, message, Instant.now()));
    }
}
