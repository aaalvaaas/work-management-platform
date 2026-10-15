package ru.itmo.wmp.platform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.itmo.wmp.platform.dto.common.ErrorResponse;
import ru.itmo.wmp.platform.exception.auth.AccountBlockedException;
import ru.itmo.wmp.platform.exception.auth.InvalidCredentialsException;
import ru.itmo.wmp.platform.exception.auth.InvalidTokenException;
import ru.itmo.wmp.platform.exception.registration.AlreadyRegisteredException;
import ru.itmo.wmp.platform.exception.registration.EmailAlreadyUsedException;
import ru.itmo.wmp.platform.exception.registration.InvitationNotValidException;
import ru.itmo.wmp.platform.exception.registration.RegistrationNotAllowedException;
import ru.itmo.wmp.platform.exception.user.UserNotFoundException;

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

        return build(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR,
            "Validation failed",
            request,
            errors
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
        UserNotFoundException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.NOT_FOUND,
            ErrorCode.USER_NOT_FOUND,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.UNAUTHORIZED,
            ErrorCode.INVALID_CREDENTIALS,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
        InvalidTokenException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.UNAUTHORIZED,
            ErrorCode.INVALID_TOKEN,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountBlocked(
        AccountBlockedException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.FORBIDDEN,
            ErrorCode.ACCOUNT_BLOCKED,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(RegistrationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationNotAllowed(
        RegistrationNotAllowedException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.FORBIDDEN,
            ErrorCode.REGISTRATION_NOT_ALLOWED,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyRegistered(
        AlreadyRegisteredException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.CONFLICT,
            ErrorCode.ALREADY_REGISTERED,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(
        EmailAlreadyUsedException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.CONFLICT,
            ErrorCode.EMAIL_ALREADY_USED,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(InvitationNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvitationNotValid(
        InvitationNotValidException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.BAD_REQUEST,
            ErrorCode.INVALID_INVITATION,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.FORBIDDEN,
            ErrorCode.ACCESS_DENIED,
            "Access denied",
            request,
            Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
        Exception ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_ERROR,
            ex.getMessage(),
            request,
            Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> build(
        HttpStatus status,
        ErrorCode code,
        String message,
        HttpServletRequest request,
        Map<String, String> errors
    ) {
        ErrorResponse response = new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            code.name(),
            message,
            request.getRequestURI(),
            errors
        );

        return ResponseEntity
            .status(status)
            .body(response);
    }
}
