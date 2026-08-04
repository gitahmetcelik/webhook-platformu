package com.webhookplatformu.servis;

import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.servis.EndpointSaglikHesaplayici.Saglik;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Uygulama;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint saglik skorunu periyodik olarak kontrol eder; skor esigin altina DUSTUGU ve tekrar
 * USTUNE CIKTIGI anlarda organizasyona bir audit kaydi birakir (bkz Faz 5.3).
 *
 * <p>Bildirim kanali olarak e-posta degil audit/dashboard secildi: bu urunde henuz kullanici/
 * e-posta modeli yok (dashboard oturumu bile API anahtari yapistirmayla calisiyor, bkz Faz 4),
 * yani gonderecek bir adres yok. Audit kaydi hem {@code /audit} sayfasinda hemen gorunuyor hem
 * de mevcut altyapiyi kullaniyor - e-posta eklendiginde ayni gecis noktasina baglanabilir.</p>
 *
 * <p>Birden fazla instance'ta ayni anda calismasi zararsiz: gecis kontrolu ({@code
 * saglikUyarisiniGuncelle}) ayni transaction icinde okunup yazildigi icin en fazla bir kayit
 * olusur - bu yuzden ShedLock'a gerek gorulmedi ({@code TeslimatServisi.dlqUzlastir} ile ayni
 * gerekce).</p>
 */
@Component
public class EndpointSaglikIzleyici {

    private static final Logger log = LoggerFactory.getLogger(EndpointSaglikIzleyici.class);

    private final EndpointRepository endpointRepository;
    private final UygulamaRepository uygulamaRepository;
    private final AuditKaydiRepository auditKaydiRepository;
    private final EndpointSaglikHesaplayici saglikHesaplayici;

    public EndpointSaglikIzleyici(EndpointRepository endpointRepository, UygulamaRepository uygulamaRepository,
                                   AuditKaydiRepository auditKaydiRepository,
                                   EndpointSaglikHesaplayici saglikHesaplayici) {
        this.endpointRepository = endpointRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.auditKaydiRepository = auditKaydiRepository;
        this.saglikHesaplayici = saglikHesaplayici;
    }

    @Scheduled(fixedDelayString = "${webhook.saglik-kontrol-araligi:PT60S}")
    @Transactional
    public void saglikKontrolEt() {
        for (Endpoint endpoint : endpointRepository.findAll()) {
            kontrolEt(endpoint);
        }
    }

    /** Tek bir endpoint icin kontrol - testlerin periyodik dongüyu beklemeden cagirabilmesi icin public. */
    @Transactional
    public void kontrolEt(Endpoint endpoint) {
        Saglik saglik = saglikHesaplayici.hesapla(endpoint);
        if (saglik.skor() == null) {
            // Trafik yok - skor "bilinmiyor", mevcut uyari durumu oldugu gibi birakilir.
            return;
        }

        boolean uyariGerekli = saglik.uyariGerektirirMi();
        if (!endpoint.saglikUyarisiniGuncelle(uyariGerekli)) {
            return;
        }
        endpointRepository.save(endpoint);

        Uygulama uygulama = uygulamaRepository.findById(endpoint.getUygulamaId()).orElseThrow();
        String tur = uyariGerekli ? "SAGLIK_SKORU_DUSTU" : "SAGLIK_SKORU_DUZELDI";
        String detay = "skor=" + saglik.skor() + " esik=" + EndpointSaglikHesaplayici.UYARI_ESIGI
                + " basari_orani=" + String.format("%.1f", saglik.basariOrani())
                + (saglik.ortalamaGecikmeMs() == null ? ""
                        : " ortalama_gecikme_ms=" + Math.round(saglik.ortalamaGecikmeMs()));

        auditKaydiRepository.save(new AuditKaydi(uygulama.getOrganizasyonId(), tur, endpoint.getId(), detay));
        log.info("Endpoint saglik durumu degisti: {} -> {} ({})", endpoint.getId(), tur, detay);
    }
}
