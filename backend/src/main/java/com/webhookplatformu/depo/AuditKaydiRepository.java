package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.AuditKaydi;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditKaydiRepository extends JpaRepository<AuditKaydi, UUID> {

    Page<AuditKaydi> findByOrganizasyonIdOrderByOlusturulmaDesc(UUID organizasyonId, Pageable pageable);
}
