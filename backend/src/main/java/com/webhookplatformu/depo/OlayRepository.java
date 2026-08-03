package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Olay;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OlayRepository extends JpaRepository<Olay, UUID>, JpaSpecificationExecutor<Olay> {

    // Giriş katmanı idempotency'si: (uygulama_id, dis_kaynak_id) UNIQUE (bkz V6 migration).
    Optional<Olay> findByUygulamaIdAndDisKaynakId(UUID uygulamaId, String disKaynakId);

    // Faz 3.2 olay akışı sayfası için filtreli/sayfalı listeleme OlaySpecifications.filtrele()
    // ile JpaSpecificationExecutor.findAll(spec, pageable) üzerinden yapılıyor (bkz o sınıf —
    // eski JPQL (:param IS NULL OR ...) deseni Instant parametrelerinde Postgres hatası
    // veriyordu). Durum filtresi burada yok (Olay'ın kendi kolonu değil, teslimatlarından
    // türetiliyor) - MVP'de basitlik için bilinçli olarak atlandı.
}
