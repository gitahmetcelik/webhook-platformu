package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Uygulama;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UygulamaRepository extends JpaRepository<Uygulama, UUID> {

    List<Uygulama> findByOrganizasyonId(UUID organizasyonId);
}
