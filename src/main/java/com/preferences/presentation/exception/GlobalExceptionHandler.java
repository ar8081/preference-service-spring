package com.preferences.presentation.exception;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.FRENCH);

    private final MeterRegistry meterRegistry;
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MeterRegistry meterRegistry,
                                  MessageSource messageSource) {
        this.messageSource = messageSource;
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest webRequest,
            HttpServletRequest httpRequest) {

        logger.warn("Resource not found: {}", ex.getMessage());
        meterRegistry.counter("errors.not_found").increment();

        Locale locale = resolveLocale(httpRequest);
        String message = messageSource.getMessage("error.not_found", null, locale);
        String path = webRequest.getDescription(false).replace("uri=", "");

        return new ResponseEntity<>(new ErrorResponse("NOT_FOUND", message, path), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(
            RateLimitException ex,
            WebRequest webRequest,
            HttpServletRequest httpRequest) {

        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        meterRegistry.counter("errors.rate_limit").increment();

        Locale locale = resolveLocale(httpRequest);
        String message = messageSource.getMessage("error.rate_limit", null, locale);
        String path = webRequest.getDescription(false).replace("uri=", "");

        return new ResponseEntity<>(new ErrorResponse("RATE_LIMIT_EXCEEDED", message, path), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest webRequest,
            HttpServletRequest httpRequest) {

        logger.warn("Validation error: {}", ex.getMessage());
        meterRegistry.counter("errors.validation").increment();

        Locale locale = resolveLocale(httpRequest);
        String message = messageSource.getMessage("error.invalid_input", null, locale);
        String path = webRequest.getDescription(false).replace("uri=", "");

        return new ResponseEntity<>(new ErrorResponse("INVALID_INPUT", message, path), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ErrorResponse> handlePreconditionFailed(
            PreconditionFailedException ex,
            WebRequest webRequest,
            HttpServletRequest httpRequest) {

        logger.warn("Precondition failed: {}", ex.getMessage());
        meterRegistry.counter("errors.precondition_failed").increment();

        Locale locale = resolveLocale(httpRequest);
        String message = messageSource.getMessage("error.precondition_failed", null, locale);
        String path = webRequest.getDescription(false).replace("uri=", "");

        return new ResponseEntity<>(new ErrorResponse("PRECONDITION_FAILED", message, path), HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            WebRequest webRequest,
            HttpServletRequest httpRequest) {

        logger.error("Unexpected error", ex);
        meterRegistry.counter("errors.internal").increment();

        Locale locale = resolveLocale(httpRequest);
        String message = messageSource.getMessage("error.internal", null, locale);
        String path = webRequest.getDescription(false).replace("uri=", "");

        return new ResponseEntity<>(new ErrorResponse("INTERNAL_SERVER_ERROR", message, path), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            WebRequest webRequest,
            HttpServletRequest httpRequest) {
        logger.warn("Malformed request body: {}", ex.getMessage());
        meterRegistry.counter("errors.bad_request").increment();
        Locale locale = resolveLocale(httpRequest);
        String message = messageSource.getMessage("error.invalid_input", null, locale);
        String path = webRequest.getDescription(false).replace("uri=", "");
        return new ResponseEntity<>(
                new ErrorResponse("INVALID_INPUT", message, path),
                HttpStatus.BAD_REQUEST
        );
    }
    /**
     * Reads Accept-Language header and returns a supported locale.
     * Falls back to English if header is missing or language not supported.
     */
    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.ENGLISH;
        }
        try {
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
            Locale matched = Locale.lookup(ranges, SUPPORTED_LOCALES);
            return matched != null ? matched : Locale.ENGLISH;
        } catch (IllegalArgumentException e) {
            return Locale.ENGLISH;
        }
    }
}