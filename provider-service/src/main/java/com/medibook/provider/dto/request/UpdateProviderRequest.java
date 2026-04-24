package com.medibook.provider.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateProviderRequest(
        @Size(max = 120, message = "Specialization must not exceed 120 characters")
        String specialization,

        @Size(max = 255, message = "Qualification must not exceed 255 characters")
        String qualification,

        @Min(value = 0, message = "Experience years must be at least 0")
        @Max(value = 80, message = "Experience years must be at most 80")
        Integer experienceYears,

        @Size(max = 2000, message = "Bio must not exceed 2000 characters")
        String bio,

        @Size(max = 120, message = "Clinic name must not exceed 120 characters")
        String clinicName,

        @Size(max = 255, message = "Clinic address must not exceed 255 characters")
        String clinicAddress) {
}
