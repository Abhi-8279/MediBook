package com.medibook.schedule.service;

import com.medibook.schedule.config.AppProperties;
import com.medibook.schedule.exception.ExternalServiceException;
import com.medibook.schedule.exception.ResourceNotFoundException;
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
public class ProviderServiceClient implements ProviderServiceGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public ProviderServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public ProviderSummary getProviderByUserId(String userId) {
        HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
        try {
            ResponseEntity<ProviderSummary> response = restTemplate.exchange(
                    providerServiceBaseUrl() + "/api/v1/providers/internal/users/" + userId,
                    HttpMethod.GET,
                    entity,
                    ProviderSummary.class);
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("Provider profile not found");
            }
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Provider profile not found");
            }
            throw new ExternalServiceException("Provider service failed while fetching the provider profile", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Provider service is currently unreachable", exception);
        }
    }

    @Override
    public void assertProviderPubliclyVisible(String providerId) {
        try {
            ResponseEntity<ProviderSummary> response = restTemplate.exchange(
                    providerServiceBaseUrl() + "/api/v1/providers/" + providerId,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    ProviderSummary.class);
            ProviderSummary provider = response.getBody();
            if (provider == null || !provider.verified()) {
                throw new ResourceNotFoundException("Provider not found");
            }
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Provider not found");
            }
            throw new ExternalServiceException("Provider service failed while validating provider visibility", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Provider service is currently unreachable", exception);
        }
    }

    @Override
    public void updateProviderAvailability(String providerId, boolean available) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProviderAvailabilityUpdateRequest> entity =
                new HttpEntity<>(new ProviderAvailabilityUpdateRequest(available), headers);
        try {
            restTemplate.exchange(
                    providerServiceBaseUrl() + "/api/v1/providers/internal/" + providerId + "/availability",
                    HttpMethod.PUT,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Provider profile not found");
            }
            throw new ExternalServiceException("Provider service failed while syncing provider availability", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Provider service is currently unreachable", exception);
        }
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(appProperties.getInternal().getHeaderName(), appProperties.getInternal().getApiKey());
        return headers;
    }

    private String providerServiceBaseUrl() {
        return appProperties.getProviderService().getBaseUrl().replaceAll("/+$", "");
    }

    private record ProviderAvailabilityUpdateRequest(boolean available) {
    }
}
