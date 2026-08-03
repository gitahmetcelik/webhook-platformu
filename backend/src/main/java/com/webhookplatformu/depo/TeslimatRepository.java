package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Teslimat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeslimatRepository extends JpaRepository<Teslimat, UUID> {

    List<Teslimat> findByOlayId(UUID olayId);
}
