package com.webhookplatformu.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.webhookplatformu.api.dto.OlayOlusturmaIstegi;
import com.webhookplatformu.api.dto.OlayOlusturmaYaniti;
import com.webhookplatformu.api.dto.OlayOzetiYaniti;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OlayRepository;
import com.webhookplatformu.depo.OlaySpecifications;
import com.webhookplatformu.depo.OrganizasyonRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.motor.IdempotencyAnahtariUretici;
import com.webhookplatformu.motor.TeslimatPayload;
import com.webhookplatformu.servis.GirisHizSiniricisi;
import com.webhookplatformu.servis.KullanimSayaciServisi;
import com.webhookplatformu.servis.TraceKimligiSaglayici;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Olay;
import com.webhookplatformu.varlik.Organizasyon;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.Uygulama;
import com.webhookplatformu.yapilandirma.OrganizasyonBaglami;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Giriş API'si: müşteri sistemleri event'lerini buraya POST eder. Tek transaction içinde
 * olay yazılır, filtreye uyan aktif endpoint'ler bulunur, her biri için bir teslimat
 * yaratılıp motora gönderilir — motorun outbox'ı (bkz gorev-motoru ADR-0004) sayesinde bu
 * transaction commit olmadan hiçbir teslimat kuyruğa girmez.
 */
@RestController
@RequestMapping("/v1/uygulamalar/{uygulamaId}/olaylar")
public class OlayController {

    private final OlayRepository olayRepository;
    private final UygulamaRepository uygulamaRepository;
    private final OrganizasyonRepository organizasyonRepository;
    private final EndpointRepository endpointRepository;
    private final TeslimatRepository teslimatRepository;
    private final GorevGonderici gorevGonderici;
    private final IdempotencyAnahtariUretici idempotencyAnahtariUretici;
    private final ObjectMapper objectMapper;
    private final OrganizasyonBaglami organizasyonBaglami;
    private final GirisHizSiniricisi girisHizSiniricisi;
    private final KullanimSayaciServisi kullanimSayaciServisi;
    private final TraceKimligiSaglayici traceKimligiSaglayici;

    public OlayController(OlayRepository olayRepository, UygulamaRepository uygulamaRepository,
                           OrganizasyonRepository organizasyonRepository, EndpointRepository endpointRepository,
                           TeslimatRepository teslimatRepository, GorevGonderici gorevGonderici,
                           IdempotencyAnahtariUretici idempotencyAnahtariUretici, ObjectMapper objectMapper,
                           OrganizasyonBaglami organizasyonBaglami, GirisHizSiniricisi girisHizSiniricisi,
                           KullanimSayaciServisi kullanimSayaciServisi,
                           TraceKimligiSaglayici traceKimligiSaglayici) {
        this.olayRepository = olayRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.organizasyonRepository = organizasyonRepository;
        this.endpointRepository = endpointRepository;
        this.teslimatRepository = teslimatRepository;
        this.gorevGonderici = gorevGonderici;
        this.idempotencyAnahtariUretici = idempotencyAnahtariUretici;
        this.objectMapper = objectMapper;
        this.organizasyonBaglami = organizasyonBaglami;
        this.girisHizSiniricisi = girisHizSiniricisi;
        this.kullanimSayaciServisi = kullanimSayaciServisi;
        this.traceKimligiSaglayici = traceKimligiSaglayici;
    }

    @GetMapping
    public Page<OlayOzetiYaniti> listele(@PathVariable UUID uygulamaId,
                                          @RequestParam(required = false) String tip,
                                          @RequestParam(required = false) Instant baslangic,
                                          @RequestParam(required = false) Instant bitis,
                                          Pageable pageable) {
        uygulamaDogrula(uygulamaId);
        return olayRepository.findAll(OlaySpecifications.filtrele(uygulamaId, tip, baslangic, bitis), pageable)
                .map(olay -> OlayOzetiYaniti.of(olay, teslimatRepository.findByOlayId(olay.getId())));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<OlayOlusturmaYaniti> olayOlustur(
            @PathVariable UUID uygulamaId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OlayOlusturmaIstegi istek) {

        Uygulama uygulama = uygulamaDogrula(uygulamaId);
        UUID organizasyonId = uygulama.getOrganizasyonId();

        if (!girisHizSiniricisi.izinVer(organizasyonId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Giris hiz siniri asildi");
        }
        Organizasyon organizasyon = organizasyonRepository.findById(organizasyonId).orElseThrow();
        Instant ayBaslangici = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
                .atZone(java.time.ZoneOffset.UTC).withDayOfMonth(1).toInstant();
        if (teslimatRepository.countByOrganizasyonIdAndOlusturulmaAfter(organizasyonId, ayBaslangici)
                >= organizasyon.getAylikKota()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Aylik teslimat kotasi asildi");
        }

        Optional<Olay> mevcutOlay = olayRepository.findByUygulamaIdAndDisKaynakId(uygulamaId, idempotencyKey);
        if (mevcutOlay.isPresent()) {
            List<Teslimat> mevcutTeslimatlar = teslimatRepository.findByOlayId(mevcutOlay.get().getId());
            return ResponseEntity.ok(new OlayOlusturmaYaniti(mevcutOlay.get().getId(), mevcutTeslimatlar.size(),
                    mevcutTeslimatlar.stream().map(Teslimat::getId).toList()));
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(istek.payload());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload serilestirilemedi", e);
        }

        // Tek trace id: olaya ve ondan dogan TUM teslimatlara ayni deger yaziliyor (bkz Faz 5.2) -
        // "bu event'e ne oldu" sorusu tek bir id ile hem UI'da hem loglarda cevaplanabilsin diye.
        String traceId = traceKimligiSaglayici.mevcutTraceId();

        Olay olay = new Olay(uygulamaId, organizasyonId, istek.tip(), payloadJson, idempotencyKey, traceId);
        olayRepository.save(olay);

        List<Endpoint> aboneEndpointler = endpointRepository.findByUygulamaId(uygulamaId).stream()
                .filter(endpoint -> endpoint.olayTipineAboneMi(istek.tip()))
                .toList();

        List<UUID> olusanTeslimatIdleri = new ArrayList<>();
        for (Endpoint endpoint : aboneEndpointler) {
            UUID teslimatId = UUID.randomUUID();
            olusanTeslimatIdleri.add(teslimatId);
            if (endpoint.devreAcikMi()) {
                // Devre acik: motora hic gonderilmez, sagli sondasi devreyi kapatana kadar bekler.
                teslimatRepository.save(
                        Teslimat.beklemede(teslimatId, olay.getId(), endpoint.getId(), organizasyonId, traceId));
                continue;
            }
            String motorIdempotencyAnahtari = idempotencyAnahtariUretici.uret(organizasyonId, teslimatId);
            // Endpoint hiz siniri varsa siradaki gonderimin en erken ne zaman olabilecegini
            // hesapla, motorun kendi planlananZaman zamanlamasini kullan (bkz Faz 4.3).
            Instant planlananZaman = endpoint.siradakiPlanlamayiHesaplaVeIlerlet();
            endpointRepository.save(endpoint);
            UUID gorevId = gorevGonderici.gonder(endpoint.getRetryProfili().getGorevTipi(),
                    new TeslimatPayload(teslimatId), new GorevOpsiyonlari(motorIdempotencyAnahtari, null, planlananZaman));
            teslimatRepository.save(
                    new Teslimat(teslimatId, olay.getId(), endpoint.getId(), organizasyonId, gorevId, traceId));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OlayOlusturmaYaniti(olay.getId(), aboneEndpointler.size(), olusanTeslimatIdleri));
    }

    private Uygulama uygulamaDogrula(UUID uygulamaId) {
        Uygulama uygulama = uygulamaRepository.findById(uygulamaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Uygulama bulunamadi: " + uygulamaId));
        if (!uygulama.getOrganizasyonId().equals(organizasyonBaglami.getOrganizasyonId())) {
            // 403 degil 404 - varlik sizdirmamak icin (bkz Faz 4.6 kapi testi 2. adim).
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uygulama bulunamadi: " + uygulamaId);
        }
        return uygulama;
    }
}
