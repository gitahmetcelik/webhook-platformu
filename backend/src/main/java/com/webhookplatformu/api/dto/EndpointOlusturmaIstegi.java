package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.RetryProfili;
import jakarta.validation.constraints.NotBlank;

public record EndpointOlusturmaIstegi(@NotBlank String url, String[] olayFiltresi, RetryProfili retryProfili,
                                       Integer hizSiniriSn) {
}
