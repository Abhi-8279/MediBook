package com.medibook.record.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AttachDocumentRequest(
        @NotBlank(message = "Attachment URL is required")
        String attachmentUrl) {
}
