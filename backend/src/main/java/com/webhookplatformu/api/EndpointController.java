package com.webhookplatformu.api;

import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.Endpoint;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/endpointler")
public class EndpointController {

    private final EndpointRepository endpointRepository;
    private final AuditKaydiRepository auditKaydiRepository;

    public EndpointController(EndpointRepository endpointRepository, AuditKaydiRepository auditKaydiRepository) {
        this.endpointRepository = endpointRepository;
        this.auditKaydiRepository = auditKaydiRepository;
    }

    @PostMapping("/{id}/devre-sifirla")
    @Transactional
    public void devreSifirla(@PathVariable UUID id) {
        Endpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint bulunamadi: " + id));
        endpoint.devreyiKapat();
        endpointRepository.save(endpoint);
        auditKaydiRepository.save(new AuditKaydi("DEVRE_ELLE_SIFIRLANDI", endpoint.getId(), null));
    }
}
