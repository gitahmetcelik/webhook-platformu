package com.webhookplatformu.gelistirme;

import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OrganizasyonRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.guvenlik.SecretUretici;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Organizasyon;
import com.webhookplatformu.varlik.Uygulama;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Henuz bir admin API'si olmadigi icin (Faz 1'in kapsami disi), kapi testi/gelistirme icin
 * tek seferlik bir organizasyon+uygulama+endpoint (test-alici'ye isaret eden) tohumluyor.
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

    public VeriTohumlayici(OrganizasyonRepository organizasyonRepository, UygulamaRepository uygulamaRepository,
                            EndpointRepository endpointRepository, SecretUretici secretUretici,
                            SifrelemeServisi sifrelemeServisi) {
        this.organizasyonRepository = organizasyonRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.endpointRepository = endpointRepository;
        this.secretUretici = secretUretici;
        this.sifrelemeServisi = sifrelemeServisi;
    }

    @Override
    public void run(String... args) {
        Organizasyon organizasyon = new Organizasyon("Demo Organizasyon");
        organizasyonRepository.save(organizasyon);

        Uygulama uygulama = new Uygulama(organizasyon.getId(), "Demo Uygulama", "prod");
        uygulamaRepository.save(uygulama);

        String duzSecret = secretUretici.uret();
        Endpoint endpoint = new Endpoint(uygulama.getId(), "http://localhost:4000/webhook?mod=ok",
                sifrelemeServisi.sifrele(duzSecret), new String[0]);
        endpointRepository.save(endpoint);

        log.info("TOHUM organizasyonId={}", organizasyon.getId());
        log.info("TOHUM uygulamaId={}", uygulama.getId());
        log.info("TOHUM endpointId={}", endpoint.getId());
        log.info("TOHUM endpoint duz secret (test-alici WEBHOOK_SECRET olarak ayarla)={}", duzSecret);
    }
}
