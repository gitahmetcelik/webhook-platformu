package com.webhookplatformu.api;

import com.webhookplatformu.api.dto.ApiAnahtariUretimYaniti;
import com.webhookplatformu.api.dto.ApiAnahtariYaniti;
import com.webhookplatformu.api.dto.AuditKaydiYaniti;
import com.webhookplatformu.api.dto.KullanimGunlukYaniti;
import com.webhookplatformu.api.dto.OrganizasyonBenYaniti;
import com.webhookplatformu.depo.ApiAnahtariRepository;
import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.OrganizasyonRepository;
import com.webhookplatformu.guvenlik.ApiAnahtariServisi;
import com.webhookplatformu.guvenlik.ApiAnahtariServisi.UretilenAnahtar;
import com.webhookplatformu.servis.KullanimSayaciServisi;
import com.webhookplatformu.varlik.ApiAnahtari;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.Organizasyon;
import com.webhookplatformu.yapilandirma.OrganizasyonBaglami;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Faz 4: organizasyon oz-bilgisi, kullanim/kota, audit log, API anahtari yonetimi. */
@RestController
public class OrganizasyonController {

    private final OrganizasyonRepository organizasyonRepository;
    private final ApiAnahtariRepository apiAnahtariRepository;
    private final ApiAnahtariServisi apiAnahtariServisi;
    private final AuditKaydiRepository auditKaydiRepository;
    private final KullanimSayaciServisi kullanimSayaciServisi;
    private final OrganizasyonBaglami organizasyonBaglami;

    public OrganizasyonController(OrganizasyonRepository organizasyonRepository,
                                   ApiAnahtariRepository apiAnahtariRepository, ApiAnahtariServisi apiAnahtariServisi,
                                   AuditKaydiRepository auditKaydiRepository,
                                   KullanimSayaciServisi kullanimSayaciServisi,
                                   OrganizasyonBaglami organizasyonBaglami) {
        this.organizasyonRepository = organizasyonRepository;
        this.apiAnahtariRepository = apiAnahtariRepository;
        this.apiAnahtariServisi = apiAnahtariServisi;
        this.auditKaydiRepository = auditKaydiRepository;
        this.kullanimSayaciServisi = kullanimSayaciServisi;
        this.organizasyonBaglami = organizasyonBaglami;
    }

    @GetMapping("/v1/organizasyon/ben")
    public OrganizasyonBenYaniti ben() {
        UUID organizasyonId = organizasyonBaglami.getOrganizasyonId();
        Organizasyon organizasyon = organizasyonRepository.findById(organizasyonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizasyon bulunamadi"));
        return new OrganizasyonBenYaniti(organizasyon.getId(), organizasyon.getAd(), organizasyon.getAylikKota(),
                kullanimSayaciServisi.buAyToplam(organizasyonId));
    }

    @GetMapping("/v1/kullanim")
    public List<KullanimGunlukYaniti> kullanim() {
        return kullanimSayaciServisi.buAyGunluk(organizasyonBaglami.getOrganizasyonId()).stream()
                .map(KullanimGunlukYaniti::of).toList();
    }

    @GetMapping("/v1/audit")
    public Page<AuditKaydiYaniti> audit(Pageable pageable) {
        return auditKaydiRepository
                .findByOrganizasyonIdOrderByOlusturulmaDesc(organizasyonBaglami.getOrganizasyonId(), pageable)
                .map(AuditKaydiYaniti::of);
    }

    @GetMapping("/v1/organizasyon/api-anahtarlari")
    public List<ApiAnahtariYaniti> apiAnahtarlariListele() {
        return apiAnahtariRepository.findByOrganizasyonIdOrderByOlusturulmaDesc(organizasyonBaglami.getOrganizasyonId())
                .stream().map(ApiAnahtariYaniti::of).toList();
    }

    @PostMapping("/v1/organizasyon/api-anahtarlari")
    @Transactional
    public ResponseEntity<ApiAnahtariUretimYaniti> apiAnahtariUret() {
        UUID organizasyonId = organizasyonBaglami.getOrganizasyonId();
        UretilenAnahtar uretilen = apiAnahtariServisi.uret(organizasyonId);
        auditKaydiRepository.save(new AuditKaydi(organizasyonId, "API_ANAHTARI_URETILDI", uretilen.id(), null));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiAnahtariUretimYaniti(uretilen.id(), uretilen.duzAnahtar()));
    }

    @PostMapping("/v1/organizasyon/api-anahtarlari/{id}/iptal")
    @Transactional
    public void apiAnahtariIptalEt(@PathVariable UUID id) {
        UUID organizasyonId = organizasyonBaglami.getOrganizasyonId();
        ApiAnahtari anahtar = apiAnahtariRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API anahtari bulunamadi: " + id));
        if (!anahtar.getOrganizasyonId().equals(organizasyonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API anahtari bulunamadi: " + id);
        }
        anahtar.iptalEt();
        apiAnahtariRepository.save(anahtar);
        auditKaydiRepository.save(new AuditKaydi(organizasyonId, "API_ANAHTARI_IPTAL", id, null));
    }
}
