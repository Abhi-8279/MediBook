package com.medibook.appointment.service;

import com.medibook.appointment.config.AppProperties;
import com.medibook.appointment.exception.AppointmentConflictException;
import com.medibook.appointment.exception.ExternalServiceException;
import com.medibook.appointment.exception.ResourceNotFoundException;
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
public class ScheduleServiceClient implements ScheduleServiceGateway {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public ScheduleServiceClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public ScheduleSlotSummary getSlotById(String slotId) {
        HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
        try {
            ResponseEntity<ScheduleSlotSummary> response = restTemplate.exchange(
                    scheduleServiceBaseUrl() + "/api/v1/schedules/internal/slots/" + slotId,
                    HttpMethod.GET,
                    entity,
                    ScheduleSlotSummary.class);
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("Linked schedule slot was not found");
            }
            return response.getBody();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Linked schedule slot was not found");
            }
            throw new ExternalServiceException("Schedule service failed while fetching the slot", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Schedule service is currently unreachable", exception);
        }
    }

    @Override
    public void bookSlot(String slotId, String bookingReference) {
        HttpHeaders headers = createInternalHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InternalSlotBookingRequest> entity =
                new HttpEntity<>(new InternalSlotBookingRequest(bookingReference), headers);
        try {
            restTemplate.exchange(
                    scheduleServiceBaseUrl() + "/api/v1/schedules/internal/slots/" + slotId + "/book",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Linked schedule slot was not found");
            }
            if (exception.getStatusCode().value() == 409) {
                throw new AppointmentConflictException("The selected slot is no longer available");
            }
            throw new ExternalServiceException("Schedule service failed while booking the slot", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Schedule service is currently unreachable", exception);
        }
    }

    @Override
    public void releaseSlot(String slotId) {
        HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
        try {
            restTemplate.exchange(
                    scheduleServiceBaseUrl() + "/api/v1/schedules/internal/slots/" + slotId + "/release",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Linked schedule slot was not found");
            }
            if (exception.getStatusCode().value() == 409) {
                throw new AppointmentConflictException("The linked slot could not be released cleanly");
            }
            throw new ExternalServiceException("Schedule service failed while releasing the slot", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Schedule service is currently unreachable", exception);
        }
    }

    @Override
    public void completeSlot(String slotId) {
        HttpEntity<Void> entity = new HttpEntity<>(createInternalHeaders());
        try {
            restTemplate.exchange(
                    scheduleServiceBaseUrl() + "/api/v1/schedules/internal/slots/" + slotId + "/complete",
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Linked schedule slot was not found");
            }
            if (exception.getStatusCode().value() == 409) {
                throw new AppointmentConflictException("The linked slot could not be completed cleanly");
            }
            throw new ExternalServiceException("Schedule service failed while completing the slot", exception);
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Schedule service is currently unreachable", exception);
        }
    }

    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(appProperties.getInternal().getHeaderName(), appProperties.getInternal().getApiKey());
        return headers;
    }

    private String scheduleServiceBaseUrl() {
        return appProperties.getScheduleService().getBaseUrl().replaceAll("/+$", "");
    }

    private record InternalSlotBookingRequest(String bookingReference) {
    }
}
