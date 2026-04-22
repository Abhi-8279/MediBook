package com.medibook.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medibook.schedule.dto.request.AddSlotRequest;
import com.medibook.schedule.dto.request.GenerateRecurringSlotsRequest;
import com.medibook.schedule.dto.request.InternalSlotBookingRequest;
import com.medibook.schedule.dto.response.AvailabilitySlotResponse;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.enums.RecurrenceType;
import com.medibook.schedule.enums.Role;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.exception.SlotConflictException;
import com.medibook.schedule.repository.AvailabilitySlotRepository;
import com.medibook.schedule.security.AuthenticatedUser;
import com.medibook.schedule.service.impl.ScheduleServiceImpl;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Mock
    private ProviderServiceGateway providerServiceGateway;

    private ScheduleServiceImpl scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleServiceImpl(availabilitySlotRepository, providerServiceGateway, FIXED_CLOCK);
    }

    @Test
    void shouldCreateProviderSlotAndSyncAvailability() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "meera@medibook.com",
                Role.PROVIDER);
        AddSlotRequest request = new AddSlotRequest(
                LocalDate.of(2026, 4, 27),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                30);

        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(availabilitySlotRepository.existsOverlappingSlot(any(), any(), any(), any(), any())).thenReturn(false);
        when(availabilitySlotRepository.saveAndFlush(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilitySlotRepository.findSlots(
                        eq("provider-1"),
                        isNull(),
                        eq(LocalDate.of(2026, 4, 22)),
                        isNull(),
                        eq(false),
                        eq(false)))
                .thenReturn(List.of(buildSlotOn(LocalDate.of(2026, 4, 27), LocalTime.of(10, 0), LocalTime.of(10, 30))));

        AvailabilitySlotResponse response = scheduleService.addSlot(authenticatedUser, request);

        ArgumentCaptor<AvailabilitySlot> slotCaptor = ArgumentCaptor.forClass(AvailabilitySlot.class);
        verify(availabilitySlotRepository).saveAndFlush(slotCaptor.capture());
        AvailabilitySlot savedSlot = slotCaptor.getValue();

        assertThat(savedSlot.getProviderId()).isEqualTo("provider-1");
        assertThat(savedSlot.getDurationMinutes()).isEqualTo(30);
        assertThat(savedSlot.getRecurrenceType()).isEqualTo(RecurrenceType.NONE);
        assertThat(response.providerId()).isEqualTo("provider-1");
        verify(providerServiceGateway).updateProviderAvailability("provider-1", true);
    }

    @Test
    void shouldRejectOverlappingSlotCreation() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "meera@medibook.com",
                Role.PROVIDER);
        AddSlotRequest request = new AddSlotRequest(
                LocalDate.of(2026, 4, 25),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                30);

        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(availabilitySlotRepository.existsOverlappingSlot(any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> scheduleService.addSlot(authenticatedUser, request))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void shouldGenerateWeeklyRecurringSlots() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "provider-user-1",
                "meera@medibook.com",
                Role.PROVIDER);
        GenerateRecurringSlotsRequest request = new GenerateRecurringSlotsRequest(
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 5, 6),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                30,
                RecurrenceType.WEEKLY,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                null);

        when(providerServiceGateway.getProviderByUserId("provider-user-1"))
                .thenReturn(new ProviderSummary("provider-1", "provider-user-1", true, true));
        when(availabilitySlotRepository.existsOverlappingSlot(any(), any(), any(), any(), any())).thenReturn(false);
        when(availabilitySlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilitySlotRepository.findSlots(
                        eq("provider-1"),
                        isNull(),
                        eq(LocalDate.of(2026, 4, 22)),
                        isNull(),
                        eq(false),
                        eq(false)))
                .thenReturn(List.of(buildSlotOn(LocalDate.of(2026, 4, 23), LocalTime.of(9, 0), LocalTime.of(9, 30))));

        List<AvailabilitySlotResponse> response = scheduleService.generateRecurringSlots(authenticatedUser, request);

        assertThat(response).hasSizeGreaterThanOrEqualTo(2);
        assertThat(response).allMatch(slot -> slot.recurrenceType() == RecurrenceType.WEEKLY);
        verify(providerServiceGateway).updateProviderAvailability("provider-1", true);
    }

    @Test
    void shouldHideBookedSlotFromPublicLookup() {
        AvailabilitySlot slot = buildSlot();
        slot.setBooked(true);

        when(availabilitySlotRepository.findBySlotId("slot-1")).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> scheduleService.getSlotById("slot-1", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Slot not found");
    }

    @Test
    void shouldHideExpiredSlotsFromPublicProviderListing() {
        AvailabilitySlot expiredSlot = buildSlotOn("slot-expired", LocalDate.of(2026, 4, 22), LocalTime.of(8, 0), LocalTime.of(8, 30));
        AvailabilitySlot futureSlot = buildSlotOn("slot-future", LocalDate.of(2026, 4, 22), LocalTime.of(11, 0), LocalTime.of(11, 30));

        when(availabilitySlotRepository.findSlots(
                        eq("provider-1"),
                        isNull(),
                        eq(LocalDate.of(2026, 4, 22)),
                        isNull(),
                        eq(false),
                        eq(false)))
                .thenReturn(List.of(expiredSlot, futureSlot));

        List<AvailabilitySlotResponse> response = scheduleService.getProviderSlots(
                "provider-1",
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().slotId()).isEqualTo("slot-future");
        verify(providerServiceGateway).assertProviderPubliclyVisible("provider-1");
    }

    @Test
    void shouldHideUnverifiedProviderScheduleFromPublicReads() {
        AvailabilitySlot slot = buildSlot();

        when(availabilitySlotRepository.findBySlotId("slot-1")).thenReturn(Optional.of(slot));
        doThrow(new ResourceNotFoundException("Provider not found"))
                .when(providerServiceGateway)
                .assertProviderPubliclyVisible("provider-1");

        assertThatThrownBy(() -> scheduleService.getSlotById("slot-1", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider not found");
    }

    @Test
    void shouldBookSlotInternally() {
        AvailabilitySlot slot = buildSlot();

        when(availabilitySlotRepository.findBySlotId("slot-1")).thenReturn(Optional.of(slot));
        when(availabilitySlotRepository.saveAndFlush(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilitySlotRepository.findSlots(
                        eq("provider-1"),
                        isNull(),
                        eq(LocalDate.of(2026, 4, 22)),
                        isNull(),
                        eq(false),
                        eq(false)))
                .thenReturn(List.of());

        AvailabilitySlotResponse response = scheduleService.bookSlotInternally(
                "slot-1",
                new InternalSlotBookingRequest("appointment-77"));

        assertThat(response.booked()).isTrue();
        assertThat(response.bookingReference()).isEqualTo("appointment-77");
        verify(providerServiceGateway).updateProviderAvailability("provider-1", false);
    }

    @Test
    void shouldSyncProviderAvailabilityUsingOnlyStillBookableSlots() {
        AvailabilitySlot slot = buildSlot();

        when(availabilitySlotRepository.findBySlotId("slot-1")).thenReturn(Optional.of(slot));
        when(availabilitySlotRepository.saveAndFlush(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(availabilitySlotRepository.findSlots(
                        eq("provider-1"),
                        isNull(),
                        eq(LocalDate.of(2026, 4, 22)),
                        isNull(),
                        eq(false),
                        eq(false)))
                .thenReturn(List.of(buildSlotOn(LocalDate.of(2026, 4, 22), LocalTime.of(8, 0), LocalTime.of(8, 30))));

        scheduleService.bookSlotInternally("slot-1", new InternalSlotBookingRequest("appointment-90"));

        verify(providerServiceGateway).updateProviderAvailability("provider-1", false);
    }

    private AvailabilitySlot buildSlot() {
        return buildSlotOn("slot-1", LocalDate.of(2026, 4, 24), LocalTime.of(14, 0), LocalTime.of(14, 30));
    }

    private AvailabilitySlot buildSlotOn(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return buildSlotOn("slot-1", date, startTime, endTime);
    }

    private AvailabilitySlot buildSlotOn(String slotId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setSlotId(slotId);
        slot.setProviderId("provider-1");
        slot.setSlotDate(date);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setDurationMinutes(30);
        slot.setBooked(false);
        slot.setBlocked(false);
        slot.setRecurrenceType(RecurrenceType.NONE);
        return slot;
    }
}
