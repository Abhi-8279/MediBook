package com.medibook.schedule.security;

import com.medibook.schedule.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/v1/schedules/internal/";

    private final AppProperties appProperties;

    public InternalServiceAuthenticationFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String headerName = appProperties.getInternal().getHeaderName();
        String providedKey = request.getHeader(headerName);

        if (!appProperties.getInternal().getApiKey().equals(providedKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Invalid internal API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
