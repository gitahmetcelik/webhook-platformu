package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.ApiAnahtari;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiAnahtariRepository extends JpaRepository<ApiAnahtari, UUID> {

    Optional<ApiAnahtari> findByAnahtarHash(String anahtarHash);

    List<ApiAnahtari> findByOrganizasyonIdOrderByOlusturulmaDesc(UUID organizasyonId);
}
