package com.medibook.appointment.security;

import com.medibook.appointment.service.AuthServiceGateway;
import com.medibook.appointment.service.AuthTokenValidationResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthServiceGateway authServiceGateway;

    public AuthTokenAuthenticationFilter(AuthServiceGateway authServiceGateway) {
        this.authServiceGateway = authServiceGateway;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthTokenValidationResponse validationResponse =
                authServiceGateway.validateAccessToken(authHeader.substring(7));

        if (validationResponse.valid() && validationResponse.active()) {
            AuthenticatedUser principal = new AuthenticatedUser(
                    validationResponse.userId(),
                    validationResponse.email(),
                    validationResponse.role());
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + validationResponse.role().name())));
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}
