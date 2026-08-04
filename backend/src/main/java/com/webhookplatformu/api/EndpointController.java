package com.webhookplatformu.api;

import com.webhookplatformu.api.dto.EndpointOlusturmaIstegi;
import com.webhookplatformu.api.dto.EndpointYaniti;
import com.webhookplatformu.api.dto.ImzaDogrulaIstegi;
import com.webhookplatformu.api.dto.ImzaDogrulaYaniti;
import com.webhookplatformu.api.dto.SecretRotasyonYaniti;
import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.guvenlik.HmacImzalayici;
import com.webhookplatformu.guvenlik.SecretUretici;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.guvenlik.SsrfKorumaServisi;
import com.webhookplatformu.servis.BekleyenTeslimatKuyruklayici;
import com.webhookplatformu.servis.EndpointSaglikHesaplayici;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.RetryProfili;
import com.webhookplatformu.varlik.Uygulama;
import com.webhookplatformu.yapilandirma.OrganizasyonBaglami;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/endpointler")
public class EndpointController {

    /** Rotasyonda eski secret'in imza dogrulamada hala kabul edildigi pencere (bkz Faz 4.1). */
    private static final long SECRET_ROTASYON_GRACE_SAAT = 24;

    private final EndpointRepository endpointRepository;
    private final UygulamaRepository uygulamaRepository;
    private final AuditKaydiRepository auditKaydiRepository;
    private final EndpointSaglikHesaplayici saglikHesaplayici;
    private final SecretUretici secretUretici;
    private final SifrelemeServisi sifrelemeServisi;
    private final HmacImzalayici hmacImzalayici;
    private final OrganizasyonBaglami organizasyonBaglami;
    private final BekleyenTeslimatKuyruklayici bekleyenTeslimatKuyruklayici;
    private final SsrfKorumaServisi ssrfKorumaServisi;

    public EndpointController(EndpointRepository endpointRepository, UygulamaRepository uygulamaRepository,
                               AuditKaydiRepository auditKaydiRepository, EndpointSaglikHesaplayici saglikHesaplayici,
                               SecretUretici secretUretici, SifrelemeServisi sifrelemeServisi,
                               HmacImzalayici hmacImzalayici, OrganizasyonBaglami organizasyonBaglami,
                               BekleyenTeslimatKuyruklayici bekleyenTeslimatKuyruklayici,
                               SsrfKorumaServisi ssrfKorumaServisi) {
        this.endpointRepository = endpointRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.auditKaydiRepository = auditKaydiRepository;
        this.saglikHesaplayici = saglikHesaplayici;
        this.secretUretici = secretUretici;
        this.sifrelemeServisi = sifrelemeServisi;
        this.hmacImzalayici = hmacImzalayici;
        this.organizasyonBaglami = organizasyonBaglami;
        this.bekleyenTeslimatKuyruklayici = bekleyenTeslimatKuyruklayici;
        this.ssrfKorumaServisi = ssrfKorumaServisi;
    }

    @GetMapping("/{id}")
    public EndpointYaniti detay(@PathVariable UUID id) {
        Endpoint endpoint = bul(id);
        return EndpointYaniti.of(endpoint, saglikHesaplayici.hesapla(endpoint));
    }

    @PatchMapping("/{id}")
    @Transactional
    public EndpointYaniti guncelle(@PathVariable UUID id, @Valid @RequestBody EndpointOlusturmaIstegi istek) {
        Endpoint endpoint = bul(id);
        try {
            ssrfKorumaServisi.dogrula(istek.url());
        } catch (SsrfKorumaServisi.SsrfIhlali e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        RetryProfili profil = istek.retryProfili() != null ? istek.retryProfili() : endpoint.getRetryProfili();
        String[] filtre = istek.olayFiltresi() != null ? istek.olayFiltresi() : endpoint.getOlayFiltresi();
        endpoint.guncelle(istek.url(), filtre, profil, istek.hizSiniriSn());
        endpointRepository.save(endpoint);
        return EndpointYaniti.of(endpoint, saglikHesaplayici.hesapla(endpoint));
    }

    @PostMapping("/{id}/devre-sifirla")
    @Transactional
    public void devreSifirla(@PathVariable UUID id) {
        Endpoint endpoint = bul(id);
        endpoint.devreyiKapat();
        endpointRepository.save(endpoint);

        // Devre acikken BEKLEMEDE'de biriken teslimatlar da kuyruga alinmali - aksi halde
        // KALICI OLARAK kaybolurlardi: saglik sondasi yalnizca devresi ACIK endpoint'leri
        // tariyor, elle kapatilan endpoint bir daha hic taranmiyor (bkz
        // BekleyenTeslimatKuyruklayici; Faz 5.6 kapi testinde canli yiginda gozlemlendi).
        int kuyruklanan = bekleyenTeslimatKuyruklayici.birikenleriKuyrugaAl(endpoint);

        auditKaydiRepository.save(new AuditKaydi(organizasyonBaglami.getOrganizasyonId(), "DEVRE_ELLE_SIFIRLANDI",
                endpoint.getId(), "kuyruga_alinan_bekleyen=" + kuyruklanan));
    }

    /** Yeni secret uretir, eskisini {@value SECRET_ROTASYON_GRACE_SAAT} saat gecerli tutar (bkz Faz 4.1). */
    @PostMapping("/{id}/secret-rotasyon")
    @Transactional
    public SecretRotasyonYaniti secretRotasyon(@PathVariable UUID id) {
        Endpoint endpoint = bul(id);
        String duzYeniSecret = secretUretici.uret();
        endpoint.secretRotasyonuBaslat(sifrelemeServisi.sifrele(duzYeniSecret), SECRET_ROTASYON_GRACE_SAAT);
        endpointRepository.save(endpoint);
        auditKaydiRepository.save(new AuditKaydi(organizasyonBaglami.getOrganizasyonId(), "SECRET_ROTASYONU",
                endpoint.getId(), "grace_saat=" + SECRET_ROTASYON_GRACE_SAAT));
        return new SecretRotasyonYaniti(duzYeniSecret, endpoint.getEskiSecretGecerlilikBitis());
    }

    /**
     * Musteri tarafinin gordugu bir imzayi hem aktif hem (grace penceresindeyse) eski secret'a
     * karsi dogrular - rotasyonun gercekten calistigini kanitlamak icin (bkz Faz 4.6 kapi testi).
     */
    @PostMapping("/{id}/imza-dogrula")
    public ImzaDogrulaYaniti imzaDogrula(@PathVariable UUID id, @Valid @RequestBody ImzaDogrulaIstegi istek) {
        Endpoint endpoint = bul(id);
        byte[] govde = istek.govde().getBytes(StandardCharsets.UTF_8);

        String aktifSecret = sifrelemeServisi.cozumle(endpoint.getImzaSecret());
        if (hmacImzalayici.dogrula(aktifSecret, istek.webhookId(), istek.zamanDamgasi(), govde, istek.imza())) {
            return new ImzaDogrulaYaniti(true, "AKTIF");
        }
        if (endpoint.eskiSecretGecerliMi()) {
            String eskiSecret = sifrelemeServisi.cozumle(endpoint.getEskiImzaSecret());
            if (hmacImzalayici.dogrula(eskiSecret, istek.webhookId(), istek.zamanDamgasi(), govde, istek.imza())) {
                return new ImzaDogrulaYaniti(true, "ESKI");
            }
        }
        return new ImzaDogrulaYaniti(false, null);
    }

    private Endpoint bul(UUID id) {
        Endpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint bulunamadi: " + id));
        Uygulama uygulama = uygulamaRepository.findById(endpoint.getUygulamaId()).orElseThrow();
        if (!uygulama.getOrganizasyonId().equals(organizasyonBaglami.getOrganizasyonId())) {
            // 403 degil 404 - varlik sizdirmamak icin (bkz Faz 4.6 kapi testi 2. adim).
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint bulunamadi: " + id);
        }
        return endpoint;
    }
}
