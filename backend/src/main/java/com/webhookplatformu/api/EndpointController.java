package com.webhookplatformu.api;

import com.webhookplatformu.api.dto.EndpointOlusturmaIstegi;
import com.webhookplatformu.api.dto.EndpointYaniti;
import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.servis.EndpointSaglikHesaplayici;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.RetryProfili;
import jakarta.validation.Valid;
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

    private final EndpointRepository endpointRepository;
    private final AuditKaydiRepository auditKaydiRepository;
    private final EndpointSaglikHesaplayici saglikHesaplayici;

    public EndpointController(EndpointRepository endpointRepository, AuditKaydiRepository auditKaydiRepository,
                               EndpointSaglikHesaplayici saglikHesaplayici) {
        this.endpointRepository = endpointRepository;
        this.auditKaydiRepository = auditKaydiRepository;
        this.saglikHesaplayici = saglikHesaplayici;
    }

    @GetMapping("/{id}")
    public EndpointYaniti detay(@PathVariable UUID id) {
        Endpoint endpoint = bul(id);
        return EndpointYaniti.of(endpoint, saglikHesaplayici.basariOraniSon24Saat(endpoint));
    }

    @PatchMapping("/{id}")
    @Transactional
    public EndpointYaniti guncelle(@PathVariable UUID id, @Valid @RequestBody EndpointOlusturmaIstegi istek) {
        Endpoint endpoint = bul(id);
        RetryProfili profil = istek.retryProfili() != null ? istek.retryProfili() : endpoint.getRetryProfili();
        String[] filtre = istek.olayFiltresi() != null ? istek.olayFiltresi() : endpoint.getOlayFiltresi();
        endpoint.guncelle(istek.url(), filtre, profil);
        endpointRepository.save(endpoint);
        return EndpointYaniti.of(endpoint, saglikHesaplayici.basariOraniSon24Saat(endpoint));
    }

    @PostMapping("/{id}/devre-sifirla")
    @Transactional
    public void devreSifirla(@PathVariable UUID id) {
        Endpoint endpoint = bul(id);
        endpoint.devreyiKapat();
        endpointRepository.save(endpoint);
        auditKaydiRepository.save(new AuditKaydi("DEVRE_ELLE_SIFIRLANDI", endpoint.getId(), null));
    }

    private Endpoint bul(UUID id) {
        return endpointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint bulunamadi: " + id));
    }
}
