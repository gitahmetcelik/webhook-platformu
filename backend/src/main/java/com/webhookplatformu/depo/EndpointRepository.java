package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.DevreDurumu;
import com.webhookplatformu.varlik.Endpoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    List<Endpoint> findByUygulamaId(UUID uygulamaId);

    List<Endpoint> findByDevreDurumu(DevreDurumu devreDurumu);
}
