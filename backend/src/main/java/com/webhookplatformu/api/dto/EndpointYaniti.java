package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.DevreDurumu;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.RetryProfili;
import java.time.Instant;
import java.util.UUID;

public record EndpointYaniti(UUID id, UUID uygulamaId, String url, String[] olayFiltresi, DevreDurumu devreDurumu,
                              RetryProfili retryProfili, int ardisikHataSayisi, Double basariOraniSon24Saat,
                              Instant olusturulma) {

    public static EndpointYaniti of(Endpoint endpoint, Double basariOraniSon24Saat) {
        return new EndpointYaniti(endpoint.getId(), endpoint.getUygulamaId(), endpoint.getUrl(),
                endpoint.getOlayFiltresi(), endpoint.getDevreDurumu(), endpoint.getRetryProfili(),
                endpoint.getArdisikHataSayisi(), basariOraniSon24Saat, endpoint.getOlusturulma());
    }
}
