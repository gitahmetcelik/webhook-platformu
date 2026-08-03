package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Bkz {@link OlaySpecifications} — aynı gerekçe (JPQL'deki Instant parametre tipi belirsizliği). */
public final class TeslimatSpecifications {

    private TeslimatSpecifications() {
    }

    public static Specification<Teslimat> filtrele(TeslimatDurumu durum, UUID endpointId, Instant baslangic,
                                                     Instant bitis) {
        return (root, query, cb) -> {
            List<Predicate> kosullar = new ArrayList<>();
            if (durum != null) {
                kosullar.add(cb.equal(root.get("durum"), durum));
            }
            if (endpointId != null) {
                kosullar.add(cb.equal(root.get("endpointId"), endpointId));
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
