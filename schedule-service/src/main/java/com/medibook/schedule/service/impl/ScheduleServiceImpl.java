package com.medibook.schedule.service.impl;

import com.medibook.schedule.dto.request.AddBulkSlotsRequest;
import com.medibook.schedule.dto.request.AddSlotRequest;
import com.medibook.schedule.dto.request.BlockSlotRequest;
import com.medibook.schedule.dto.request.GenerateRecurringSlotsRequest;
import com.medibook.schedule.dto.request.InternalSlotBookingRequest;
import com.medibook.schedule.dto.request.UpdateSlotRequest;
import com.medibook.schedule.dto.response.AvailabilitySlotResponse;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.enums.RecurrenceType;
import com.medibook.schedule.enums.Role;
import com.medibook.schedule.exception.ExternalServiceException;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.exception.SlotConflictException;
import com.medibook.schedule.repository.AvailabilitySlotRepository;
import com.medibook.schedule.security.AuthenticatedUser;
import com.medibook.schedule.service.ProviderServiceGateway;
import com.medibook.schedule.service.ProviderSummary;
import com.medibook.schedule.service.ScheduleService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ProviderServiceGateway providerServiceGateway;
    private final Clock clock;

    @Autowired
    public ScheduleServiceImpl(
            AvailabilitySlotRepository availabilitySlotRepository,
            ProviderServiceGateway providerServiceGateway) {
        this(availabilitySlotRepository, providerServiceGateway, Clock.systemUTC());
    }

    public ScheduleServiceImpl(
            AvailabilitySlotRepository availabilitySlotRepository,
            ProviderServiceGateway providerServiceGateway,
            Clock clock) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.providerServiceGateway = providerServiceGateway;
        this.clock = clock;
    }

    @Override
    public AvailabilitySlotResponse addSlot(AuthenticatedUser authenticatedUser, AddSlotRequest request) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        SlotWindow slotWindow = SlotWindow.from(request.date(), request.startTime(), request.endTime(), request.durationMinutes());
        validateSlotWindow(slotWindow);
        assertNoOverlap(providerId, slotWindow, null);

        AvailabilitySlot slot = buildSlot(providerId, slotWindow, RecurrenceType.NONE);
        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(providerId);
        return toResponse(saved);
    }

    @Override
    public List<AvailabilitySlotResponse> addBulkSlots(AuthenticatedUser authenticatedUser, AddBulkSlotsRequest request) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        List<SlotWindow> slotWindows = request.slots()
                .stream()
                .map(slot -> SlotWindow.from(slot.date(), slot.startTime(), slot.endTime(), slot.durationMinutes()))
                .sorted(Comparator.comparing(SlotWindow::date).thenComparing(SlotWindow::startTime))
                .toList();

        validateBulkWindows(slotWindows);

        List<AvailabilitySlot> slots = slotWindows.stream()
                .map(slotWindow -> buildSlot(providerId, slotWindow, RecurrenceType.NONE))
                .toList();

        List<AvailabilitySlotResponse> response = availabilitySlotRepository.saveAll(slots)
                .stream()
                .map(this::toResponse)
                .toList();
        syncProviderAvailabilityQuietly(providerId);
        return response;
    }

    @Override
    public List<AvailabilitySlotResponse> generateRecurringSlots(
            AuthenticatedUser authenticatedUser,
            GenerateRecurringSlotsRequest request) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        validateRecurringRequest(request);

        Set<LocalDate> dates = generateRecurringDates(request);
        List<SlotWindow> slotWindows = dates.stream()
                .map(date -> SlotWindow.from(date, request.startTime(), request.endTime(), request.durationMinutes()))
                .sorted(Comparator.comparing(SlotWindow::date).thenComparing(SlotWindow::startTime))
                .toList();

        validateBulkWindows(slotWindows);

        List<AvailabilitySlot> slots = slotWindows.stream()
                .map(slotWindow -> buildSlot(providerId, slotWindow, request.recurrenceType()))
                .toList();

        List<AvailabilitySlotResponse> response = availabilitySlotRepository.saveAll(slots)
                .stream()
                .map(this::toResponse)
                .toList();
        syncProviderAvailabilityQuietly(providerId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getProviderSlots(
            String providerId,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean includeBooked,
            Boolean includeBlocked,
            AuthenticatedUser authenticatedUser) {
        DateFilters filters = normalizeFilters(date, dateFrom, dateTo);
        boolean privilegedViewer = isAdmin(authenticatedUser) || isOwner(providerId, authenticatedUser);
        if (!privilegedViewer) {
            providerServiceGateway.assertProviderPubliclyVisible(providerId);
        }
        boolean effectiveIncludeBooked = privilegedViewer && (includeBooked == null || includeBooked);
        boolean effectiveIncludeBlocked = privilegedViewer && (includeBlocked == null || includeBlocked);

        return availabilitySlotRepository.findSlots(
                        providerId,
                        filters.date(),
                        filters.dateFrom(),
                        filters.dateTo(),
                        effectiveIncludeBooked,
                        effectiveIncludeBlocked)
                .stream()
                .filter(slot -> privilegedViewer || isBookableAtCurrentTime(slot))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getMySlots(
            AuthenticatedUser authenticatedUser,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo) {
        String providerId = resolveCurrentProviderId(authenticatedUser);
        DateFilters filters = normalizeFilters(date, dateFrom, dateTo);
        return availabilitySlotRepository.findSlots(providerId, filters.date(), filters.dateFrom(), filters.dateTo(), true, true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilitySlotResponse getSlotById(String slotId, AuthenticatedUser authenticatedUser) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        if (!isAdmin(authenticatedUser) && !isOwner(slot.getProviderId(), authenticatedUser)) {
            providerServiceGateway.assertProviderPubliclyVisible(slot.getProviderId());
        }
        if (!canViewSlot(slot, authenticatedUser)) {
            throw new ResourceNotFoundException("Slot not found");
        }
        return toResponse(slot);
    }

    @Override
    public AvailabilitySlotResponse updateSlot(String slotId, AuthenticatedUser authenticatedUser, UpdateSlotRequest request) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        assertCanManage(slot, authenticatedUser);
        if (slot.isBooked()) {
            throw new IllegalStateException("Booked slots cannot be updated");
        }

        SlotWindow slotWindow = SlotWindow.from(request.date(), request.startTime(), request.endTime(), request.durationMinutes());
        validateSlotWindow(slotWindow);
        assertNoOverlap(slot.getProviderId(), slotWindow, slot.getSlotId());

        slot.setSlotDate(slotWindow.date());
        slot.setStartTime(slotWindow.startTime());
        slot.setEndTime(slotWindow.endTime());
        slot.setDurationMinutes(slotWindow.durationMinutes());

        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
        return toResponse(saved);
    }

    @Override
    public AvailabilitySlotResponse blockSlot(String slotId, AuthenticatedUser authenticatedUser, BlockSlotRequest request) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        assertCanManage(slot, authenticatedUser);
        if (slot.isBooked()) {
            throw new IllegalStateException("Booked slots cannot be blocked");
        }

        slot.setBlocked(true);
        slot.setBlockedReason(blankToNull(request.reason()));
        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
        return toResponse(saved);
    }

    @Override
    public AvailabilitySlotResponse unblockSlot(String slotId, AuthenticatedUser authenticatedUser) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        assertCanManage(slot, authenticatedUser);
        if (!slot.isBlocked()) {
            throw new IllegalStateException("Slot is not currently blocked");
        }

        slot.setBlocked(false);
        slot.setBlockedReason(null);
        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
        return toResponse(saved);
    }

    @Override
    public void deleteSlot(String slotId, AuthenticatedUser authenticatedUser) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        assertCanManage(slot, authenticatedUser);
        if (slot.isBooked()) {
            throw new IllegalStateException("Booked slots cannot be deleted");
        }

        availabilitySlotRepository.delete(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
    }

    @Override
    public AvailabilitySlotResponse bookSlotInternally(String slotId, InternalSlotBookingRequest request) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        if (slot.isBlocked()) {
            throw new SlotConflictException("Blocked slots cannot be booked");
        }
        if (slot.isBooked()) {
            throw new SlotConflictException("Slot is already booked");
        }
        if (slotEndDateTime(slot).isBefore(LocalDateTime.now(clock))) {
            throw new IllegalStateException("Past slots cannot be booked");
        }

        slot.setBooked(true);
        slot.setBookingReference(request.bookingReference().trim());
        slot.setBookedAt(Instant.now(clock));
        slot.setCompleted(false);
        slot.setCompletedAt(null);

        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
        return toResponse(saved);
    }

    @Override
    public AvailabilitySlotResponse releaseSlotInternally(String slotId) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        if (!slot.isBooked()) {
            throw new IllegalStateException("Slot is not currently booked");
        }

        slot.setBooked(false);
        slot.setBookingReference(null);
        slot.setBookedAt(null);
        slot.setCompleted(false);
        slot.setCompletedAt(null);

        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
        return toResponse(saved);
    }

    @Override
    public AvailabilitySlotResponse completeSlotInternally(String slotId) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        if (!slot.isBooked()) {
            throw new IllegalStateException("Only booked slots can be completed");
        }
        if (slot.isCompleted()) {
            throw new IllegalStateException("Slot is already completed");
        }

        slot.setCompleted(true);
        slot.setCompletedAt(Instant.now(clock));

        AvailabilitySlot saved = availabilitySlotRepository.saveAndFlush(slot);
        syncProviderAvailabilityQuietly(slot.getProviderId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilitySlotResponse getSlotByIdInternally(String slotId) {
        return toResponse(findSlotOrThrow(slotId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getAvailableSlotsByProviderInternally(
            String providerId,
            LocalDate date,
            LocalDate dateFrom,
            LocalDate dateTo) {
        providerServiceGateway.assertProviderPubliclyVisible(providerId);
        DateFilters filters = normalizeFilters(date, dateFrom, dateTo);
        return availabilitySlotRepository.findSlots(providerId, filters.date(), filters.dateFrom(), filters.dateTo(), false, false)
                .stream()
                .filter(this::isBookableAtCurrentTime)
                .map(this::toResponse)
                .toList();
    }

    private AvailabilitySlot findSlotOrThrow(String slotId) {
        return availabilitySlotRepository.findBySlotId(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
    }

    private void validateBulkWindows(List<SlotWindow> slotWindows) {
        if (slotWindows.isEmpty()) {
            throw new IllegalArgumentException("At least one slot is required");
        }

        for (int index = 0; index < slotWindows.size(); index++) {
            SlotWindow current = slotWindows.get(index);
            validateSlotWindow(current);
            for (int previousIndex = 0; previousIndex < index; previousIndex++) {
                SlotWindow previous = slotWindows.get(previousIndex);
                if (current.overlaps(previous)) {
                    throw new SlotConflictException("One or more requested slots overlap each other");
                }
            }
        }
    }

    private void validateRecurringRequest(GenerateRecurringSlotsRequest request) {
        if (request.recurrenceType() == RecurrenceType.NONE) {
            throw new IllegalArgumentException("Recurring slot generation requires DAILY, WEEKLY, or CUSTOM recurrence");
        }
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("Recurring start date must be on or before end date");
        }
        if (request.recurrenceType() == RecurrenceType.CUSTOM
                && (request.daysOfWeek() == null || request.daysOfWeek().isEmpty())
                && request.intervalDays() == null) {
            throw new IllegalArgumentException("Custom recurrence requires daysOfWeek or intervalDays");
        }
    }

    private Set<LocalDate> generateRecurringDates(GenerateRecurringSlotsRequest request) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        LocalDate current = request.startDate();

        if (request.recurrenceType() == RecurrenceType.DAILY) {
            while (!current.isAfter(request.endDate())) {
                dates.add(current);
                current = current.plusDays(1);
            }
            return dates;
        }

        Set<DayOfWeek> activeDays = request.daysOfWeek() == null || request.daysOfWeek().isEmpty()
                ? Set.of(request.startDate().getDayOfWeek())
                : request.daysOfWeek();

        if (request.recurrenceType() == RecurrenceType.WEEKLY) {
            while (!current.isAfter(request.endDate())) {
                if (activeDays.contains(current.getDayOfWeek())) {
                    dates.add(current);
                }
                current = current.plusDays(1);
            }
            return dates;
        }

        if (request.daysOfWeek() != null && !request.daysOfWeek().isEmpty()) {
            while (!current.isAfter(request.endDate())) {
                if (activeDays.contains(current.getDayOfWeek())) {
                    dates.add(current);
                }
                current = current.plusDays(1);
            }
            return dates;
        }

        int intervalDays = request.intervalDays();
        while (!current.isAfter(request.endDate())) {
            dates.add(current);
            current = current.plusDays(intervalDays);
        }
        return dates;
    }

    private String resolveCurrentProviderId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PROVIDER) {
            throw new AccessDeniedException("Only providers can manage schedule slots");
        }
        ProviderSummary provider = providerServiceGateway.getProviderByUserId(authenticatedUser.userId());
        return provider.providerId();
    }

    private boolean isOwner(String providerId, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() != Role.PROVIDER) {
            return false;
        }
        try {
            return providerId.equals(resolveCurrentProviderId(authenticatedUser));
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }

    private boolean isAdmin(AuthenticatedUser authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.role() == Role.ADMIN;
    }

    private void assertCanManage(AvailabilitySlot slot, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser)) {
            return;
        }
        if (isOwner(slot.getProviderId(), authenticatedUser)) {
            return;
        }
        throw new AccessDeniedException("You can only manage your own slots");
    }

    private boolean canViewSlot(AvailabilitySlot slot, AuthenticatedUser authenticatedUser) {
        if (isAdmin(authenticatedUser) || isOwner(slot.getProviderId(), authenticatedUser)) {
            return true;
        }
        return isBookableAtCurrentTime(slot);
    }

    private void validateSlotWindow(SlotWindow slotWindow) {
        if (!slotWindow.startTime().isBefore(slotWindow.endTime())) {
            throw new IllegalArgumentException("Slot start time must be before end time");
        }
        long actualMinutes = Duration.between(slotWindow.startTime(), slotWindow.endTime()).toMinutes();
        if (actualMinutes != slotWindow.durationMinutes()) {
            throw new IllegalArgumentException("Duration must exactly match the difference between start and end time");
        }
        if (LocalDateTime.of(slotWindow.date(), slotWindow.endTime()).isBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Past slots cannot be created or updated");
        }
    }

    private void assertNoOverlap(String providerId, SlotWindow slotWindow, String excludeSlotId) {
        if (availabilitySlotRepository.existsOverlappingSlot(
                providerId,
                slotWindow.date(),
                slotWindow.startTime(),
                slotWindow.endTime(),
                excludeSlotId)) {
            throw new SlotConflictException("The requested slot overlaps with an existing slot");
        }
    }

    private DateFilters normalizeFilters(LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
        if (date != null) {
            return new DateFilters(date, null, null);
        }

        LocalDate effectiveFrom = dateFrom;
        LocalDate effectiveTo = dateTo;
        if (effectiveFrom == null && effectiveTo == null) {
            effectiveFrom = LocalDate.now(clock);
        }
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("dateFrom must be on or before dateTo");
        }
        return new DateFilters(null, effectiveFrom, effectiveTo);
    }

    private AvailabilitySlot buildSlot(String providerId, SlotWindow slotWindow, RecurrenceType recurrenceType) {
        assertNoOverlap(providerId, slotWindow, null);
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setSlotId(UUID.randomUUID().toString());
        slot.setProviderId(providerId);
        slot.setSlotDate(slotWindow.date());
        slot.setStartTime(slotWindow.startTime());
        slot.setEndTime(slotWindow.endTime());
        slot.setDurationMinutes(slotWindow.durationMinutes());
        slot.setBooked(false);
        slot.setBlocked(false);
        slot.setRecurrenceType(recurrenceType);
        return slot;
    }

    private void syncProviderAvailabilityQuietly(String providerId) {
        long visibleFutureSlots = availabilitySlotRepository
                .findSlots(providerId, null, LocalDate.now(clock), null, false, false)
                .stream()
                .filter(this::isBookableAtCurrentTime)
                .count();
        try {
            providerServiceGateway.updateProviderAvailability(providerId, visibleFutureSlots > 0);
        } catch (ExternalServiceException | ResourceNotFoundException exception) {
            // Keep slot operations available even if downstream availability sync is temporarily stale.
        }
    }

    private AvailabilitySlotResponse toResponse(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(
                slot.getSlotId(),
                slot.getProviderId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getDurationMinutes(),
                slot.isBooked(),
                slot.isBlocked(),
                slot.getRecurrenceType(),
                slot.getBlockedReason(),
                slot.getBookingReference(),
                slot.getBookedAt(),
                slot.isCompleted(),
                slot.getCompletedAt(),
                slot.getCreatedAt(),
                slot.getUpdatedAt());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime slotEndDateTime(AvailabilitySlot slot) {
        return LocalDateTime.of(slot.getSlotDate(), slot.getEndTime());
    }

    private boolean isBookableAtCurrentTime(AvailabilitySlot slot) {
        return !slot.isBooked()
                && !slot.isBlocked()
                && !slot.isCompleted()
                && !slotEndDateTime(slot).isBefore(LocalDateTime.now(clock));
    }

    private record DateFilters(LocalDate date, LocalDate dateFrom, LocalDate dateTo) {
    }

    private record SlotWindow(LocalDate date, LocalTime startTime, LocalTime endTime, int durationMinutes) {

        static SlotWindow from(LocalDate date, LocalTime startTime, LocalTime endTime, Integer durationMinutes) {
            return new SlotWindow(date, startTime, endTime, durationMinutes);
        }

        boolean overlaps(SlotWindow other) {
            return date.equals(other.date())
                    && startTime.isBefore(other.endTime())
                    && endTime.isAfter(other.startTime());
        }
    }
}
