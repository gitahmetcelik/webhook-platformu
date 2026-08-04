package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.time.Instant;
import java.util.UUID;

public record TeslimatOzetiYaniti(UUID id, UUID olayId, UUID endpointId, TeslimatDurumu durum,
                                   UUID anaTeslimatId, String traceId, Instant olusturulma, Instant guncellenme) {

    public static TeslimatOzetiYaniti of(Teslimat teslimat) {
        return new TeslimatOzetiYaniti(teslimat.getId(), teslimat.getOlayId(), teslimat.getEndpointId(),
                teslimat.getDurum(), teslimat.getAnaTeslimatId(), teslimat.getTraceId(), teslimat.getOlusturulma(),
                teslimat.getGuncellenme());
    }
}
