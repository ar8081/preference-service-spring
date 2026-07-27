package com.preferences.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preferences.presentation.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)  // runs before all other filters
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ObjectMapper objectMapper;

    @Value("${security.api-key}")
    private String validApiKey;

    @Value("${security.max-body-size-bytes:2048}")
    private int maxBodySizeBytes;

    public ApiKeyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip actuator and swagger endpoints — they don't need API key
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        // 401 — header missing entirely
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Request rejected: missing X-API-Key header for path {}", path);
            writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "API key is required. Provide X-API-Key header.", path);
            return;
        }

        // 403 — header present but wrong value
        if (!validApiKey.equals(apiKey)) {
            logger.warn("Request rejected: invalid X-API-Key for path {}", path);
            writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Invalid API key.", path);
            return;
        }

        // Check request body size for write operations
        if (isWriteMethod(request.getMethod())) {
            int contentLength = request.getContentLengthLong() > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) request.getContentLengthLong();
            if (contentLength > maxBodySizeBytes) {
                logger.warn("Request rejected: body size {} exceeds limit {} for path {}",
                        contentLength, maxBodySizeBytes, path);
                writeError(response, HttpStatus.BAD_REQUEST, "PAYLOAD_TOO_LARGE",
                        "Request body exceeds maximum allowed size of " + maxBodySizeBytes + " bytes.", path);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWriteMethod(String method) {
        return "PUT".equalsIgnoreCase(method)
                || "POST".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            String error,
                            String message,
                            String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse(error, message, path);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}