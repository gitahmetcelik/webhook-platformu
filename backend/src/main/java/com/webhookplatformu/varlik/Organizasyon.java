package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizasyon", schema = "webhook")
public class Organizasyon {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String ad;

    @Column(nullable = false)
    private Instant olusturulma;

    protected Organizasyon() {
    }

    public Organizasyon(String ad) {
        this.id = UUID.randomUUID();
        this.ad = ad;
        this.olusturulma = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getAd() {
        return ad;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }
}
