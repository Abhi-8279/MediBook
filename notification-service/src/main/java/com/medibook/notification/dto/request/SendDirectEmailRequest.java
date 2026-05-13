package com.medibook.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendDirectEmailRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email must be valid")
        String toEmail,

        @NotBlank(message = "Subject is required")
        @Size(max = 120, message = "Subject must be at most 120 characters")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message must be at most 4000 characters")
        String message) {
}
