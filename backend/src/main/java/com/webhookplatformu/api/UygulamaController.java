package com.webhookplatformu.api;

import com.webhookplatformu.api.dto.UygulamaYaniti;
import com.webhookplatformu.depo.UygulamaRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bu fazda gerçek çok-kiracılık/auth yok (bkz Faz 4) — dashboard tek organizasyon
 * varsayımıyla ilk uygulamayı context olarak kullanıyor.
 */
@RestController
@RequestMapping("/v1/uygulamalar")
public class UygulamaController {

    private final UygulamaRepository uygulamaRepository;

    public UygulamaController(UygulamaRepository uygulamaRepository) {
        this.uygulamaRepository = uygulamaRepository;
    }

    @GetMapping
    public List<UygulamaYaniti> listele() {
        return uygulamaRepository.findAll().stream().map(UygulamaYaniti::of).toList();
    }
}
