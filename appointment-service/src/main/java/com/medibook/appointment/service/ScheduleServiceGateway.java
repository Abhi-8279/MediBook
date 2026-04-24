package com.medibook.appointment.service;

public interface ScheduleServiceGateway {

    ScheduleSlotSummary getSlotById(String slotId);

    void bookSlot(String slotId, String bookingReference);

    void releaseSlot(String slotId);

    void completeSlot(String slotId);
}
