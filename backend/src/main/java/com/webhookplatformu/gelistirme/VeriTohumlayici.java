package com.webhookplatformu.gelistirme;

import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OrganizasyonRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.guvenlik.ApiAnahtariServisi;
import com.webhookplatformu.guvenlik.ApiAnahtariServisi.UretilenAnahtar;
import com.webhookplatformu.guvenlik.SecretUretici;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Organizasyon;
import com.webhookplatformu.varlik.RetryProfili;
import com.webhookplatformu.varlik.Uygulama;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Henuz bir kayit/onboarding akisi olmadigi icin (bkz Faz 4 planlama notu), kapi testi/
 * gelistirme icin iki organizasyon (kiracı izolasyonu test edilebilsin diye - bkz Faz 4.6
 * kapi testi 1-2. adim) + her birine bir uygulama+endpoint+API anahtari tohumluyor.
 * Sadece {@code seed} profili aktifken calisir - her acilista tekrar tohumlamamak icin bir
 * kere calistirilip tekrar profilsiz acilmali.
 */
@Component
@Profile("seed")
public class VeriTohumlayici implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VeriTohumlayici.class);

    private final OrganizasyonRepository organizasyonRepository;
    private final UygulamaRepository uygulamaRepository;
    private final EndpointRepository endpointRepository;
    private final SecretUretici secretUretici;
    private final SifrelemeServisi sifrelemeServisi;
    private final ApiAnahtariServisi apiAnahtariServisi;
    /**
     * Konteynerde calisirken "localhost" backend'in KENDISI oluyor - test-alici'ye ulasilamiyor
     * (docker-compose.prod.yml bunu "http://test-alici:4000/webhook" olarak geciyor, bkz Faz 5.5).
     */
    private final String tohumEndpointUrl;

    public VeriTohumlayici(OrganizasyonRepository organizasyonRepository, UygulamaRepository uygulamaRepository,
                            EndpointRepository endpointRepository, SecretUretici secretUretici,
                            SifrelemeServisi sifrelemeServisi, ApiAnahtariServisi apiAnahtariServisi,
                            @Value("${webhook.tohum-endpoint-url:http://localhost:4000/webhook}")
                            String tohumEndpointUrl) {
        this.organizasyonRepository = organizasyonRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.endpointRepository = endpointRepository;
        this.secretUretici = secretUretici;
        this.sifrelemeServisi = sifrelemeServisi;
        this.apiAnahtariServisi = apiAnahtariServisi;
        this.tohumEndpointUrl = tohumEndpointUrl;
    }

    @Override
    public void run(String... args) {
        tohumla("Demo Organizasyon A", "Demo Uygulama A");
        tohumla("Demo Organizasyon B", "Demo Uygulama B");
    }

    private void tohumla(String orgAdi, String uygulamaAdi) {
        Organizasyon organizasyon = new Organizasyon(orgAdi);
        organizasyonRepository.save(organizasyon);

        Uygulama uygulama = new Uygulama(organizasyon.getId(), uygulamaAdi, "prod");
        uygulamaRepository.save(uygulama);

        String duzSecret = secretUretici.uret();
        // URL'de mod query param'i YOK bilincli - test-alicinin davranisi artik
        // POST /varsayilan-mod ile calisma zamaninda degistiriliyor (bkz Faz 2.6).
        // HIZLI profili (maxDeneme=3): kapi testinde retry/DLQ senaryolari daha hizli tamamlansin.
        Endpoint endpoint = new Endpoint(uygulama.getId(), tohumEndpointUrl,
                sifrelemeServisi.sifrele(duzSecret), new String[0], RetryProfili.HIZLI);
        endpointRepository.save(endpoint);

        UretilenAnahtar apiAnahtari = apiAnahtariServisi.uret(organizasyon.getId());

        log.info("TOHUM [{}] organizasyonId={}", orgAdi, organizasyon.getId());
        log.info("TOHUM [{}] uygulamaId={}", orgAdi, uygulama.getId());
        log.info("TOHUM [{}] endpointId={}", orgAdi, endpoint.getId());
        log.info("TOHUM [{}] endpoint duz secret (test-alici WEBHOOK_SECRET olarak ayarla)={}", orgAdi, duzSecret);
        log.info("TOHUM [{}] API anahtari (Authorization: Bearer ...)={}", orgAdi, apiAnahtari.duzAnahtar());
    }
}
