package com.webhookplatformu.depo;

import com.webhookplatformu.varlik.AuditKaydi;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditKaydiRepository extends JpaRepository<AuditKaydi, UUID> {
}
