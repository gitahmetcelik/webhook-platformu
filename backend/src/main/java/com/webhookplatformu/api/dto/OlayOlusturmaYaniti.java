package com.webhookplatformu.api.dto;

import java.util.List;
import java.util.UUID;

public record OlayOlusturmaYaniti(UUID olayId, int teslimatSayisi, List<UUID> teslimatIdleri) {
}
