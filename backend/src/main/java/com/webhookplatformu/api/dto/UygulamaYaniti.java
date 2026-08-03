package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.Uygulama;
import java.time.Instant;
import java.util.UUID;

public record UygulamaYaniti(UUID id, UUID organizasyonId, String ad, String ortam, Instant olusturulma) {

    public static UygulamaYaniti of(Uygulama uygulama) {
        return new UygulamaYaniti(uygulama.getId(), uygulama.getOrganizasyonId(), uygulama.getAd(),
                uygulama.getOrtam(), uygulama.getOlusturulma());
    }
}
