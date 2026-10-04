package com.blog.filter;

import com.blog.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that protects all /api/admin/** endpoints by requiring a valid
 * X-Admin-Key header.
 * 
 * Supports two modes:
 * 1. Plaintext comparison (for dev/testing): admin.key=dev-admin-key
 * 2. BCrypt hash comparison (for prod): admin.key=$2a$10$...
 * 
 * Returns 403 with an ErrorResponse body if the header is missing or incorrect.
 * Requirements: 3.2 (Security)
 */
@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    @Value("${admin.key}")
    private String adminKeyOrHash;

    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminAuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only apply this filter to /api/admin/** requests
        return !path.startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(ADMIN_KEY_HEADER);

        // Validate: key must be provided and must match
        if (providedKey == null || !isValidKey(providedKey)) {
            rejectWithForbidden(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validate the provided key.
     * Supports both plaintext (dev) and BCrypt hashes (prod).
     */
    private boolean isValidKey(String providedKey) {
        try {
            // If admin.key starts with $2a$ or $2b$ or $2y$, it's a BCrypt hash
            if (adminKeyOrHash.startsWith("$2a$") || adminKeyOrHash.startsWith("$2b$") || adminKeyOrHash.startsWith("$2y$")) {
                // Production: Use BCrypt comparison
                return passwordEncoder.matches(providedKey, adminKeyOrHash);
            } else {
                // Development: Use plaintext comparison
                return providedKey.equals(adminKeyOrHash);
            }
        } catch (Exception e) {
            // If anything fails, reject the request
            return false;
        }
    }

    private void rejectWithForbidden(HttpServletResponse response) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                "Forbidden",
                "Missing or invalid X-Admin-Key header"
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
