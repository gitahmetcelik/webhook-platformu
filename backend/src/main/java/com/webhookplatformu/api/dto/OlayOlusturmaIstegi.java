package com.webhookplatformu.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OlayOlusturmaIstegi(
        @NotBlank String tip,
        @NotNull JsonNode payload) {
}
