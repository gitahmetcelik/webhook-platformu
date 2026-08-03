package com.webhookplatformu.servis;

import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Endpoint listesinde "son 24 saat başarı oranı" göstermek için (bkz Faz 3.4). */
@Component
public class EndpointSaglikHesaplayici {

    private final TeslimatRepository teslimatRepository;

    public EndpointSaglikHesaplayici(TeslimatRepository teslimatRepository) {
        this.teslimatRepository = teslimatRepository;
    }

    public Double basariOraniSon24Saat(Endpoint endpoint) {
        Instant esik = Instant.now().minus(24, ChronoUnit.HOURS);
        long toplam = teslimatRepository.countByEndpointIdAndOlusturulmaAfter(endpoint.getId(), esik);
        if (toplam == 0) {
            return null;
        }
        long basarili = teslimatRepository.countByEndpointIdAndDurumAndOlusturulmaAfter(
                endpoint.getId(), TeslimatDurumu.BASARILI, esik);
        return (basarili * 100.0) / toplam;
    }
}
