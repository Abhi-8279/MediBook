package com.medibook.auth.security;

import com.medibook.auth.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public OAuth2AuthenticationFailureHandler(
            AppProperties appProperties,
            HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.appProperties = appProperties;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        String targetUrl = UriComponentsBuilder
                .fromUri(URI.create(appProperties.getOauth2().getAuthorizedRedirectUris().getFirst()))
                .queryParam("error", exception.getMessage())
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
