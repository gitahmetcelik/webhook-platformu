package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.TeslimatDenemesi;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeslimatDenemesiRepository extends JpaRepository<TeslimatDenemesi, UUID> {

    List<TeslimatDenemesi> findByTeslimatIdOrderByDenemeNo(UUID teslimatId);

    long countByTeslimatId(UUID teslimatId);
}
