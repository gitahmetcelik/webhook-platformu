package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeslimatRepository extends JpaRepository<Teslimat, UUID> {

    List<Teslimat> findByOlayId(UUID olayId);

    // DLQ uzlastirma: motorda tamamlanmis (BASARISIZ) olabilecek, hala KUYRUKTA gorunen
    // teslimatlari bulmak icin (bkz TeslimatServisi.dlqUzlastir).
    List<Teslimat> findByDurumAndGorevIdIsNotNull(TeslimatDurumu durum);

    // Devre kapanirken bekleyen backlog'u kuyruga almak icin (bkz Faz 2.4).
    List<Teslimat> findByEndpointIdAndDurum(UUID endpointId, TeslimatDurumu durum);

    // Operasyon endpoint'i icin filtreli/sayfali liste (bkz Faz 2.5). Teslimat-Olay arasinda
    // JPA iliskisi kurulmadigi icin (bilincli tercih, bkz diger entity'ler) ad-hoc ON join.
    @Query("""
            SELECT t FROM Teslimat t JOIN Olay o ON o.id = t.olayId
            WHERE (:durum IS NULL OR t.durum = :durum)
              AND (:endpointId IS NULL OR t.endpointId = :endpointId)
              AND (:olayTipi IS NULL OR o.tip = :olayTipi)
              AND (:baslangic IS NULL OR t.olusturulma >= :baslangic)
              AND (:bitis IS NULL OR t.olusturulma <= :bitis)
            ORDER BY t.olusturulma DESC
            """)
    Page<Teslimat> filtrele(@Param("durum") TeslimatDurumu durum, @Param("endpointId") UUID endpointId,
                             @Param("olayTipi") String olayTipi, @Param("baslangic") Instant baslangic,
                             @Param("bitis") Instant bitis, Pageable pageable);
}
