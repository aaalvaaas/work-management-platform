package ru.itmo.wmp.mail.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.itmo.wmp.mail.dto.response.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            ErrorCode.VALIDATION_ERROR.name(),
            "Validation failed",
            request.getRequestURI(),
            errors
        );

        return ResponseEntity
            .badRequest()
            .body(response);
    }

    @ExceptionHandler(MailNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMailNotFound(
        MailNotFoundException ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            ErrorCode.MAIL_NOT_FOUND.name(),
            ex.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }

    @ExceptionHandler(DuplicateMailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateMail(
        DuplicateMailException ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            ErrorCode.DUPLICATE_MAIL.name(),
            ex.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }

    @ExceptionHandler(InvalidMailStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
        InvalidMailStatusTransitionException ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            ErrorCode.INVALID_STATUS_TRANSITION.name(),
            ex.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity
            .badRequest()
            .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
        Exception ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorCode.INTERNAL_ERROR.name(),
            ex.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
