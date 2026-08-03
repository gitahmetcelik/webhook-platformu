package com.webhookplatformu.servis;

import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Uygulama;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Kalıcı bir başarısızlık (KALICI_HATA veya DLQ) sonrası endpoint'in ardışık hata sayacını
 * artırıp, eşik aşılırsa devreyi açan ve audit kaydı yazan ortak mantık — hem
 * {@link com.webhookplatformu.motor.TeslimatHandlerTemel} hem {@link TeslimatServisi} kullanır.
 */
@Component
public class DevreKesiciYardimcisi {

    private static final Logger log = LoggerFactory.getLogger(DevreKesiciYardimcisi.class);

    private final EndpointRepository endpointRepository;
    private final UygulamaRepository uygulamaRepository;
    private final AuditKaydiRepository auditKaydiRepository;

    public DevreKesiciYardimcisi(EndpointRepository endpointRepository, UygulamaRepository uygulamaRepository,
                                  AuditKaydiRepository auditKaydiRepository) {
        this.endpointRepository = endpointRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.auditKaydiRepository = auditKaydiRepository;
    }

    public void kaliciBasarisizlikBildir(Endpoint endpoint) {
        boolean devreYeniAcildi = endpoint.ardisikHataArtir();
        endpointRepository.save(endpoint);
        if (devreYeniAcildi) {
            log.warn("Endpoint devresi acildi (ardisik hata esigi asildi): {}", endpoint.getId());
            Uygulama uygulama = uygulamaRepository.findById(endpoint.getUygulamaId()).orElseThrow();
            auditKaydiRepository.save(new AuditKaydi(uygulama.getOrganizasyonId(), "DEVRE_ACILDI", endpoint.getId(),
                    "ardisik_hata_sayisi=" + endpoint.getArdisikHataSayisi()));
        }
    }
}
