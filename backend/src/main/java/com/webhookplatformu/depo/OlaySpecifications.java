package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Olay;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Dinamik/opsiyonel filtreler için Specification kullanılıyor — JPQL'deki
 * {@code (:param IS NULL OR alan = :param)} deseni, {@code Instant} (timestamptz)
 * parametrelerinde Postgres'in "could not determine data type of parameter" hatasına yol
 * açıyordu (Faz 3.6 kapı testinde gerçekten çalıştırılınca bulundu — String/UUID
 * parametrelerinde sorun yoktu, sadece tarih parametrelerinde). Specification, boş
 * filtreler için o koşulu SORGUYA HİÇ EKLEMEZ, bu yüzden parametre-tipi belirsizliği hiç
 * oluşmuyor.
 */
public final class OlaySpecifications {

    private OlaySpecifications() {
    }

    public static Specification<Olay> filtrele(UUID uygulamaId, String tip, Instant baslangic, Instant bitis) {
        return (root, query, cb) -> {
            List<Predicate> kosullar = new ArrayList<>();
            if (uygulamaId != null) {
                kosullar.add(cb.equal(root.get("uygulamaId"), uygulamaId));
            }
            if (tip != null) {
                kosullar.add(cb.equal(root.get("tip"), tip));
            }
            if (baslangic != null) {
                kosullar.add(cb.greaterThanOrEqualTo(root.get("olusturulma"), baslangic));
            }
            if (bitis != null) {
                kosullar.add(cb.lessThanOrEqualTo(root.get("olusturulma"), bitis));
            }
            return cb.and(kosullar.toArray(new Predicate[0]));
        };
    }
}
