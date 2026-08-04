package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.Olay;
import com.webhookplatformu.varlik.Teslimat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OlayOzetiYaniti(UUID id, UUID uygulamaId, String tip, String disKaynakId, String traceId,
                               Instant olusturulma, List<TeslimatOzetiYaniti> teslimatlar) {

    public static OlayOzetiYaniti of(Olay olay, List<Teslimat> teslimatlar) {
        return new OlayOzetiYaniti(olay.getId(), olay.getUygulamaId(), olay.getTip(), olay.getDisKaynakId(),
                olay.getTraceId(), olay.getOlusturulma(),
                teslimatlar.stream().map(TeslimatOzetiYaniti::of).toList());
    }
}
