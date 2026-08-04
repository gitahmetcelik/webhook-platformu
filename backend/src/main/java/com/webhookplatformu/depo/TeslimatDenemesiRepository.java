package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.TeslimatDenemesi;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeslimatDenemesiRepository extends JpaRepository<TeslimatDenemesi, UUID> {

    List<TeslimatDenemesi> findByTeslimatIdOrderByDenemeNo(UUID teslimatId);

    long countByTeslimatId(UUID teslimatId);

    /**
     * Saglik skorunun gecikme bileseni icin (bkz Faz 5.3). {@code teslimat_denemesi}'nde
     * endpoint_id yok (teslimata bagli), bu yuzden alt sorgu ile teslimat uzerinden gidiliyor.
     * Hic deneme yoksa {@code null} doner.
     */
    @Query("""
            SELECT AVG(d.sureMs) FROM TeslimatDenemesi d
            WHERE d.teslimatId IN (
                SELECT t.id FROM Teslimat t WHERE t.endpointId = :endpointId AND t.olusturulma > :zaman)
            """)
    Double ortalamaSureMs(@Param("endpointId") UUID endpointId, @Param("zaman") Instant zaman);
}
