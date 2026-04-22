package com.medibook.schedule.service;

import com.medibook.schedule.dto.request.AddBulkSlotsRequest;
import com.medibook.schedule.dto.request.AddSlotRequest;
import com.medibook.schedule.dto.request.BlockSlotRequest;
import com.medibook.schedule.dto.request.GenerateRecurringSlotsRequest;
import com.medibook.schedule.dto.request.InternalSlotBookingRequest;
import com.medibook.schedule.dto.request.UpdateSlotRequest;
import com.medibook.schedule.dto.response.AvailabilitySlotResponse;
import com.medibook.schedule.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    AvailabilitySlotResponse addSlot(AuthenticatedUser authenticatedUser, AddSlotRequest request);

    List<AvailabilitySlotResponse> addBulkSlots(AuthenticatedUser authenticatedUser, AddBulkSlotsRequest request);

    List<AvailabilitySlotResponse> generateRecurringSlots(
            AuthenticatedUser authenticatedUser,
            GenerateRecurringSlotsRequest request);

    List<AvailabilitySlotResponse> getProviderSlots(
            String providerId,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean includeBooked,
            Boolean includeBlocked,
            AuthenticatedUser authenticatedUser);

    List<AvailabilitySlotResponse> getMySlots(
            AuthenticatedUser authenticatedUser,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo);

    AvailabilitySlotResponse getSlotById(String slotId, AuthenticatedUser authenticatedUser);

    AvailabilitySlotResponse updateSlot(String slotId, AuthenticatedUser authenticatedUser, UpdateSlotRequest request);

    AvailabilitySlotResponse blockSlot(String slotId, AuthenticatedUser authenticatedUser, BlockSlotRequest request);

    AvailabilitySlotResponse unblockSlot(String slotId, AuthenticatedUser authenticatedUser);

    void deleteSlot(String slotId, AuthenticatedUser authenticatedUser);

    AvailabilitySlotResponse bookSlotInternally(String slotId, InternalSlotBookingRequest request);

    AvailabilitySlotResponse releaseSlotInternally(String slotId);

    AvailabilitySlotResponse completeSlotInternally(String slotId);

    AvailabilitySlotResponse getSlotByIdInternally(String slotId);

    List<AvailabilitySlotResponse> getAvailableSlotsByProviderInternally(
            String providerId,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo);
}
