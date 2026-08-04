package com.webhookplatformu.api.dto;

import com.webhookplatformu.servis.EndpointSaglikHesaplayici;
import com.webhookplatformu.varlik.DevreDurumu;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.RetryProfili;
import java.time.Instant;
import java.util.UUID;

public record EndpointYaniti(UUID id, UUID uygulamaId, String url, String[] olayFiltresi, DevreDurumu devreDurumu,
                              RetryProfili retryProfili, int ardisikHataSayisi, Double basariOraniSon24Saat,
                              Double ortalamaGecikmeMs, Integer saglikSkoru, boolean saglikUyarisiAktif,
                              Integer hizSiniriSn, Instant olusturulma) {

    public static EndpointYaniti of(Endpoint endpoint, EndpointSaglikHesaplayici.Saglik saglik) {
        return new EndpointYaniti(endpoint.getId(), endpoint.getUygulamaId(), endpoint.getUrl(),
                endpoint.getOlayFiltresi(), endpoint.getDevreDurumu(), endpoint.getRetryProfili(),
                endpoint.getArdisikHataSayisi(), saglik.basariOrani(), saglik.ortalamaGecikmeMs(),
                saglik.skor(), endpoint.isSaglikUyarisiAktif(), endpoint.getHizSiniriSn(),
                endpoint.getOlusturulma());
    }
}
