package com.webhookplatformu.api;

import com.webhookplatformu.api.dto.EndpointOlusturmaIstegi;
import com.webhookplatformu.api.dto.EndpointOlusturmaYaniti;
import com.webhookplatformu.api.dto.EndpointYaniti;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.guvenlik.SecretUretici;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.servis.EndpointSaglikHesaplayici;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.RetryProfili;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/uygulamalar/{uygulamaId}/endpointler")
public class UygulamaEndpointController {

    private final EndpointRepository endpointRepository;
    private final SecretUretici secretUretici;
    private final SifrelemeServisi sifrelemeServisi;
    private final EndpointSaglikHesaplayici saglikHesaplayici;

    public UygulamaEndpointController(EndpointRepository endpointRepository, SecretUretici secretUretici,
                                       SifrelemeServisi sifrelemeServisi, EndpointSaglikHesaplayici saglikHesaplayici) {
        this.endpointRepository = endpointRepository;
        this.secretUretici = secretUretici;
        this.sifrelemeServisi = sifrelemeServisi;
        this.saglikHesaplayici = saglikHesaplayici;
    }

    @GetMapping
    public List<EndpointYaniti> listele(@PathVariable UUID uygulamaId) {
        return endpointRepository.findByUygulamaId(uygulamaId).stream()
                .map(endpoint -> EndpointYaniti.of(endpoint, saglikHesaplayici.basariOraniSon24Saat(endpoint)))
                .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<EndpointOlusturmaYaniti> olustur(@PathVariable UUID uygulamaId,
                                                             @Valid @RequestBody EndpointOlusturmaIstegi istek) {
        String duzSecret = secretUretici.uret();
        RetryProfili profil = istek.retryProfili() != null ? istek.retryProfili() : RetryProfili.STANDART;
        String[] filtre = istek.olayFiltresi() != null ? istek.olayFiltresi() : new String[0];
        Endpoint endpoint = new Endpoint(uygulamaId, istek.url(), sifrelemeServisi.sifrele(duzSecret), filtre, profil);
        endpointRepository.save(endpoint);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EndpointOlusturmaYaniti(endpoint.getId(), endpoint.getUrl(), duzSecret));
    }
}
