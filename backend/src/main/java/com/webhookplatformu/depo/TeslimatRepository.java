package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeslimatRepository extends JpaRepository<Teslimat, UUID>, JpaSpecificationExecutor<Teslimat> {

    List<Teslimat> findByOlayId(UUID olayId);

    // DLQ uzlastirma: motorda tamamlanmis (BASARISIZ) olabilecek, hala KUYRUKTA gorunen
    // teslimatlari bulmak icin (bkz TeslimatServisi.dlqUzlastir).
    List<Teslimat> findByDurumAndGorevIdIsNotNull(TeslimatDurumu durum);

    // Devre kapanirken bekleyen backlog'u kuyruga almak icin (bkz Faz 2.4).
    List<Teslimat> findByEndpointIdAndDurum(UUID endpointId, TeslimatDurumu durum);

    // Endpoint listesinde basit bir "son 24 saat basari orani" gostermek icin (bkz Faz 3.4).
    // Tam bir sparkline (zaman-dilimli grafik) yerine bilincli olarak tek bir oran.
    long countByEndpointIdAndOlusturulmaAfter(UUID endpointId, Instant zaman);

    long countByEndpointIdAndDurumAndOlusturulmaAfter(UUID endpointId, TeslimatDurumu durum, Instant zaman);

    // Kota kontrolu (bkz Faz 4.2) - kullanim_sayaci sadece TAMAMLANMIS (terminal durumdaki)
    // teslimatlari sayiyor, motor worker'i asenkron calistigi icin ingestion aninda henuz
    // guncellenmemis olabilir (yaris durumu). Kota kontrolu bu yuzden dogrudan bu canli
    // sayima dayaniyor - gecikme yok, teslimat OLUSTURULDUGU anda sayiliyor.
    long countByOrganizasyonIdAndOlusturulmaAfter(UUID organizasyonId, Instant zaman);

    // Operasyon endpoint'i icin filtreli/sayfali liste TeslimatSpecifications.filtrele() ile
    // JpaSpecificationExecutor.findAll(spec, pageable) uzerinden yapiliyor (bkz o sinif -
    // eski JPQL (:param IS NULL OR ...) deseni Instant parametrelerinde Postgres hatasi
    // veriyordu, Faz 3.6 kapi testinde gercekten calistirilinca bulundu).
}
