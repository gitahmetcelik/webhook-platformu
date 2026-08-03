package com.webhookplatformu.api.dto;

import java.util.UUID;

/** {@code secret} sadece oluşturma anında bir kez döner, sonra hiçbir yerden okunamaz. */
public record EndpointOlusturmaYaniti(UUID id, String url, String secret) {
}
