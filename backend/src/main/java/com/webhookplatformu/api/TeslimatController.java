package com.webhookplatformu.api;

import com.gorevplatformu.motorspringstarter.GorevYonetimServisi;
import com.webhookplatformu.api.dto.MotorGorevOzetiYaniti;
import com.webhookplatformu.api.dto.TeslimatDenemesiYaniti;
import com.webhookplatformu.api.dto.TeslimatDetayYaniti;
import com.webhookplatformu.api.dto.TeslimatOzetiYaniti;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OlayRepository;
import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.depo.TeslimatSpecifications;
import com.webhookplatformu.servis.TeslimatServisi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Olay;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import com.webhookplatformu.yapilandirma.OrganizasyonBaglami;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/teslimatlar")
public class TeslimatController {

    private final TeslimatRepository teslimatRepository;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;
    private final OlayRepository olayRepository;
    private final EndpointRepository endpointRepository;
    private final GorevYonetimServisi gorevYonetimServisi;
    private final TeslimatServisi teslimatServisi;
    private final OrganizasyonBaglami organizasyonBaglami;

    public TeslimatController(TeslimatRepository teslimatRepository,
                               TeslimatDenemesiRepository teslimatDenemesiRepository,
                               OlayRepository olayRepository, EndpointRepository endpointRepository,
                               GorevYonetimServisi gorevYonetimServisi, TeslimatServisi teslimatServisi,
                               OrganizasyonBaglami organizasyonBaglami) {
        this.teslimatRepository = teslimatRepository;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
        this.olayRepository = olayRepository;
        this.endpointRepository = endpointRepository;
        this.gorevYonetimServisi = gorevYonetimServisi;
        this.teslimatServisi = teslimatServisi;
        this.organizasyonBaglami = organizasyonBaglami;
    }

    @GetMapping
    public Page<TeslimatOzetiYaniti> listele(@RequestParam(required = false) TeslimatDurumu durum,
                                              @RequestParam(required = false) UUID endpointId,
                                              @RequestParam(required = false) Instant baslangic,
                                              @RequestParam(required = false) Instant bitis,
                                              Pageable pageable) {
        UUID organizasyonId = organizasyonBaglami.getOrganizasyonId();
        return teslimatRepository.findAll(
                        TeslimatSpecifications.filtrele(organizasyonId, durum, endpointId, baslangic, bitis), pageable)
                .map(TeslimatOzetiYaniti::of);
    }

    @GetMapping("/{id}")
    public TeslimatDetayYaniti detay(@PathVariable UUID id) {
        Teslimat teslimat = teslimatDogrula(id);
        Olay olay = olayRepository.findById(teslimat.getOlayId()).orElseThrow();
        Endpoint endpoint = endpointRepository.findById(teslimat.getEndpointId()).orElseThrow();

        List<TeslimatDenemesiYaniti> denemeler = teslimatDenemesiRepository.findByTeslimatIdOrderByDenemeNo(id)
                .stream().map(TeslimatDenemesiYaniti::of).toList();

        MotorGorevOzetiYaniti motorOzeti = null;
        if (teslimat.getGorevId() != null) {
            motorOzeti = gorevYonetimServisi.ozet(teslimat.getGorevId()).map(MotorGorevOzetiYaniti::of).orElse(null);
        }

        return new TeslimatDetayYaniti(TeslimatOzetiYaniti.of(teslimat), olay.getTip(), olay.getPayload(),
                endpoint.getUrl(), denemeler, motorOzeti);
    }

    @PostMapping("/{id}/yeniden-gonder")
    public ResponseEntity<TeslimatOzetiYaniti> yenidenGonder(@PathVariable UUID id) {
        teslimatDogrula(id);
        Teslimat yeniTeslimat = teslimatServisi.yenidenGonder(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(TeslimatOzetiYaniti.of(yeniTeslimat));
    }

    private Teslimat teslimatDogrula(UUID id) {
        Teslimat teslimat = teslimatRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teslimat bulunamadi: " + id));
        if (!teslimat.getOrganizasyonId().equals(organizasyonBaglami.getOrganizasyonId())) {
            // 403 degil 404 - varlik sizdirmamak icin (bkz Faz 4.6 kapi testi 2. adim).
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teslimat bulunamadi: " + id);
        }
        return teslimat;
    }
}
