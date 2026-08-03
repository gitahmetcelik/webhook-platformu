package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.ApiAnahtari;
import java.time.Instant;
import java.util.UUID;

public record ApiAnahtariYaniti(UUID id, String anahtarOnek, Instant olusturulma, Instant iptalEdilme) {

    public static ApiAnahtariYaniti of(ApiAnahtari a) {
        return new ApiAnahtariYaniti(a.getId(), a.getAnahtarOnek(), a.getOlusturulma(), a.getIptalEdilme());
    }
}
