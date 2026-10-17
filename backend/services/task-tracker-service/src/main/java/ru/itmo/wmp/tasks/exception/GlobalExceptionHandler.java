package ru.itmo.wmp.tasks.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.itmo.wmp.tasks.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        TaskNotFoundException exception,
        HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(TaskAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        TaskAccessDeniedException exception,
        HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage(), request);
    }

    @ExceptionHandler(TaskTransitionException.class)
    public ResponseEntity<ErrorResponse> handleTransition(
        TaskTransitionException exception,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "TASK_TRANSITION_NOT_ALLOWED", exception.getMessage(), request);
    }

    @ExceptionHandler(TaskValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        TaskValidationException exception,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "TASK_INVALID", exception.getMessage(), request);
    }

    @ExceptionHandler(PlatformUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePlatformUnavailable(
        PlatformUnavailableException exception,
        HttpServletRequest request
    ) {
        log.warn("Platform service call failed: {}", exception.getMessage());

        return build(
            HttpStatus.SERVICE_UNAVAILABLE,
            "PLATFORM_UNAVAILABLE",
            "Platform service is unavailable",
            request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request is not valid",
                request.getRequestURI(),
                fieldErrors
            ));
    }

    private ResponseEntity<ErrorResponse> build(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request
    ) {
        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                Map.of()
            ));
    }
}
