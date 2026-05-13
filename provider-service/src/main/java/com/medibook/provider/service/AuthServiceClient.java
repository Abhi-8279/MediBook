package com.medibook.provider.service;

import com.medibook.provider.config.AppProperties;
import com.medibook.provider.exception.ExternalServiceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthServiceClient implements AuthServiceGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public AuthServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public AuthTokenValidationResponse validateAccessToken(String accessToken) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ValidateTokenRequest> entity = new HttpEntity<>(new ValidateTokenRequest(accessToken), headers);

        try {
            ResponseEntity<AuthTokenValidationResponse> response = restTemplate.exchange(
                    authServiceBaseUrl() + "/api/v1/auth/internal/tokens/validate",
                    HttpMethod.POST,
                    entity,
                    AuthTokenValidationResponse.class);
            return response.getBody() == null
                    ? new AuthTokenValidationResponse(false, null, null, null, false)
                    : response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                return new AuthTokenValidationResponse(false, null, null, null, false);
            }
            throw new ExternalServiceException("Auth service failed while validating the access token", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Auth service is currently unreachable", exception);
        }
    }

    @Override
    public AuthUserSummary getUserById(String userId) {
        HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
        try {
            ResponseEntity<AuthUserSummary> response = restTemplate.exchange(
                    authServiceBaseUrl() + "/api/v1/auth/internal/users/" + userId,
                    HttpMethod.GET,
                    entity,
                    AuthUserSummary.class);
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("Linked auth user was not found");
            }
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Linked auth user was not found");
            }
            throw new ExternalServiceException("Auth service failed while fetching the provider user profile", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Auth service is currently unreachable", exception);
        }
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(appProperties.getInternal().getHeaderName(), appProperties.getInternal().getApiKey());
        return headers;
    }

    private String authServiceBaseUrl() {
        return appProperties.getAuthService().getBaseUrl().replaceAll("/+$", "");
    }

    private record ValidateTokenRequest(String accessToken) {
    }
}
