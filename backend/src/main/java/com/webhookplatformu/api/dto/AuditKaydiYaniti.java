package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.AuditKaydi;
import java.time.Instant;
import java.util.UUID;

public record AuditKaydiYaniti(UUID id, String tur, UUID hedefId, String detay, Instant olusturulma) {

    public static AuditKaydiYaniti of(AuditKaydi a) {
        return new AuditKaydiYaniti(a.getId(), a.getTur(), a.getHedefId(), a.getDetay(), a.getOlusturulma());
    }
}
