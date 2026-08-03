package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.Organizasyon;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizasyonRepository extends JpaRepository<Organizasyon, UUID> {
}
