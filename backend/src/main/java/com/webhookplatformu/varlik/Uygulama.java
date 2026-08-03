package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uygulama", schema = "webhook")
public class Uygulama {

    @Id
    private UUID id;

    @Column(name = "organizasyon_id", nullable = false)
    private UUID organizasyonId;

    @Column(nullable = false)
    private String ad;

    @Column(nullable = false)
    private String ortam;

    @Column(nullable = false)
    private Instant olusturulma;

    protected Uygulama() {
    }

    public Uygulama(UUID organizasyonId, String ad, String ortam) {
        this.id = UUID.randomUUID();
        this.organizasyonId = organizasyonId;
        this.ad = ad;
        this.ortam = ortam;
        this.olusturulma = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizasyonId() {
        return organizasyonId;
    }

    public String getAd() {
        return ad;
    }

    public String getOrtam() {
        return ortam;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }
}
