package com.example.nova.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handling so that no stack traces or internal details
 * are ever leaked to API clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserExists(UserAlreadyExistsException ex,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), request, null));
    }

    @ExceptionHandler(CompanyDomainAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCompanyDomainExists(CompanyDomainAlreadyExistsException ex,
                                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), request, null));
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<Map<String, Object>> handleTokenRefresh(TokenRefreshException ex,
                                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN, ex.getMessage(), request, null));
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordResetToken(InvalidPasswordResetTokenException ex,
                                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN, ex.getMessage(), request, null));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex,
                                                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null));
    }

    @ExceptionHandler(InvalidMfaCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMfa(InvalidMfaCodeException ex,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException ex,
                                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(body(HttpStatus.LOCKED, ex.getMessage(), request, null));
    }

    @ExceptionHandler(SsoAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleSsoAuth(SsoAuthenticationException ex,
                                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, null));
    }

    @ExceptionHandler(CompanionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCompanionNotFound(CompanionNotFoundException ex,
                                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage(), request, null));
    }

    @ExceptionHandler(CompanionEmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCompanionEmailExists(CompanionEmailAlreadyExistsException ex,
                                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), request, null));
    }

    @ExceptionHandler(InvalidCalendarRangeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCalendarRange(InvalidCalendarRangeException ex,
                                                                             HttpServletRequest request) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProjectNotFound(ProjectNotFoundException ex,
                                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage(), request, null));
    }

    @ExceptionHandler(ProjectAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleProjectExists(ProjectAlreadyExistsException ex,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), request, null));
    }

    @ExceptionHandler(NoAvailableCompanionException.class)
    public ResponseEntity<Map<String, Object>> handleNoAvailableCompanion(NoAvailableCompanionException ex,
                                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, ex.getMessage(), request, null));
    }

    @ExceptionHandler(VoiceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVoiceNotFound(VoiceNotFoundException ex,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, ex.getMessage(), request, null));
    }

    @ExceptionHandler(PaymentNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotSupported(PaymentNotSupportedException ex,
                                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(body(HttpStatus.NOT_IMPLEMENTED, ex.getMessage(), request, null));
    }

    /**
     * Spring MVC resolves @ExceptionHandler methods before an exception ever
     * reaches the security filter chain's AccessDeniedHandler, so without this,
     * @PreAuthorize denials (AuthorizationDeniedException, the modern
     * AuthorizationManager-based replacement for AccessDeniedException) would
     * fall through to the generic 500 handler below instead of a 403.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request, null));
    }

    @ExceptionHandler({BadCredentialsException.class, LockedException.class, DisabledException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        // Deliberately generic message to avoid user enumeration
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(HttpStatus.UNAUTHORIZED, "Invalid username or password", request, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null));
    }

    private Map<String, Object> body(HttpStatus status, String message, HttpServletRequest request, Map<String, String> errors) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", Instant.now().toString());
        map.put("status", status.value());
        map.put("error", status.getReasonPhrase());
        map.put("message", message);
        map.put("path", request.getRequestURI());
        if (errors != null) {
            map.put("fieldErrors", errors);
        }
        return map;
    }
}
