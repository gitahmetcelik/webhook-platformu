package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_kaydi", schema = "webhook")
public class AuditKaydi {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String tur;

    @Column(name = "hedef_id", nullable = false)
    private UUID hedefId;

    private String detay;

    @Column(nullable = false)
    private Instant olusturulma;

    protected AuditKaydi() {
    }

    public AuditKaydi(String tur, UUID hedefId, String detay) {
        this.id = UUID.randomUUID();
        this.tur = tur;
        this.hedefId = hedefId;
        this.detay = detay;
        this.olusturulma = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTur() {
        return tur;
    }

    public UUID getHedefId() {
        return hedefId;
    }

    public String getDetay() {
        return detay;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }
}
