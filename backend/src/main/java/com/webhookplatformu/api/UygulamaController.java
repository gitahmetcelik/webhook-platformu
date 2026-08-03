package com.webhookplatformu.api;

import com.webhookplatformu.api.dto.UygulamaYaniti;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.yapilandirma.OrganizasyonBaglami;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/uygulamalar")
public class UygulamaController {

    private final UygulamaRepository uygulamaRepository;
    private final OrganizasyonBaglami organizasyonBaglami;

    public UygulamaController(UygulamaRepository uygulamaRepository, OrganizasyonBaglami organizasyonBaglami) {
        this.uygulamaRepository = uygulamaRepository;
        this.organizasyonBaglami = organizasyonBaglami;
    }

    @GetMapping
    public List<UygulamaYaniti> listele() {
        return uygulamaRepository.findByOrganizasyonId(organizasyonBaglami.getOrganizasyonId()).stream()
                .map(UygulamaYaniti::of).toList();
    }
}
