package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Olay;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlayRepository extends JpaRepository<Olay, UUID> {

    // Giriş katmanı idempotency'si: (uygulama_id, dis_kaynak_id) UNIQUE (bkz V6 migration).
    Optional<Olay> findByUygulamaIdAndDisKaynakId(UUID uygulamaId, String disKaynakId);
}
