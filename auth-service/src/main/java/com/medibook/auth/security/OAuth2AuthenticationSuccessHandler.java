package com.medibook.auth.security;

import com.medibook.auth.config.AppProperties;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppProperties appProperties;
    private final AuthService authService;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public OAuth2AuthenticationSuccessHandler(
            AppProperties appProperties,
            AuthService authService,
            HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.appProperties = appProperties;
        this.authService = authService;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            AuthProvider provider = "github".equalsIgnoreCase(registrationId) ? AuthProvider.GITHUB : AuthProvider.GOOGLE;
            AuthResponse authResponse = authService.handleOAuthLogin(
                    provider,
                    authorizationRequestRepository.loadRequestedRole(request),
                    oauth2User.getAttributes());

            String redirectUri = appProperties.getOauth2().getAuthorizedRedirectUris().getFirst();
            String targetUrl = UriComponentsBuilder.fromUri(URI.create(redirectUri))
                    .queryParam("accessToken", authResponse.accessToken())
                    .queryParam("refreshToken", authResponse.refreshToken())
                    .queryParam("userId", authResponse.user().userId())
                    .queryParam("role", authResponse.user().role())
                    .build()
                    .toUriString();

            clearAuthenticationAttributes(request);
            authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception exception) {
            authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            String targetUrl = UriComponentsBuilder
                    .fromUri(URI.create(appProperties.getOauth2().getAuthorizedRedirectUris().getFirst()))
                    .queryParam("error", exception.getMessage())
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
