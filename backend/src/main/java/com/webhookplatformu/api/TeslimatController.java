package com.webhookplatformu.api;

import com.gorevplatformu.motorspringstarter.GorevYonetimServisi;
import com.webhookplatformu.api.dto.MotorGorevOzetiYaniti;
import com.webhookplatformu.api.dto.TeslimatDenemesiYaniti;
import com.webhookplatformu.api.dto.TeslimatDetayYaniti;
import com.webhookplatformu.api.dto.TeslimatOzetiYaniti;
import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.servis.TeslimatServisi;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/v1/teslimatlar")
public class TeslimatController {

    private final TeslimatRepository teslimatRepository;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;
    private final GorevYonetimServisi gorevYonetimServisi;
    private final TeslimatServisi teslimatServisi;

    public TeslimatController(TeslimatRepository teslimatRepository,
                               TeslimatDenemesiRepository teslimatDenemesiRepository,
                               GorevYonetimServisi gorevYonetimServisi, TeslimatServisi teslimatServisi) {
        this.teslimatRepository = teslimatRepository;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
        this.gorevYonetimServisi = gorevYonetimServisi;
        this.teslimatServisi = teslimatServisi;
    }

    @GetMapping
    public Page<TeslimatOzetiYaniti> listele(@RequestParam(required = false) TeslimatDurumu durum,
                                              @RequestParam(required = false) UUID endpointId,
                                              @RequestParam(required = false) String olayTipi,
                                              @RequestParam(required = false) Instant baslangic,
                                              @RequestParam(required = false) Instant bitis,
                                              Pageable pageable) {
        return teslimatRepository.filtrele(durum, endpointId, olayTipi, baslangic, bitis, pageable)
                .map(TeslimatOzetiYaniti::of);
    }

    @GetMapping("/{id}")
    public TeslimatDetayYaniti detay(@PathVariable UUID id) {
        Teslimat teslimat = teslimatRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teslimat bulunamadi: " + id));

        List<TeslimatDenemesiYaniti> denemeler = teslimatDenemesiRepository.findByTeslimatIdOrderByDenemeNo(id)
                .stream().map(TeslimatDenemesiYaniti::of).toList();

        MotorGorevOzetiYaniti motorOzeti = null;
        if (teslimat.getGorevId() != null) {
            motorOzeti = gorevYonetimServisi.ozet(teslimat.getGorevId()).map(MotorGorevOzetiYaniti::of).orElse(null);
        }

        return new TeslimatDetayYaniti(TeslimatOzetiYaniti.of(teslimat), denemeler, motorOzeti);
    }

    @PostMapping("/{id}/yeniden-gonder")
    public ResponseEntity<TeslimatOzetiYaniti> yenidenGonder(@PathVariable UUID id) {
        Teslimat yeniTeslimat = teslimatServisi.yenidenGonder(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(TeslimatOzetiYaniti.of(yeniTeslimat));
    }
}
