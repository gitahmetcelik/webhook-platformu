package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_anahtari", schema = "webhook")
public class ApiAnahtari {

    @Id
    private UUID id;

    @Column(name = "organizasyon_id", nullable = false)
    private UUID organizasyonId;

    /** SHA-256 hex digest - anahtar zaten yuksek entropili rastgele bir deger oldugu icin
     * (kullanici secmiyor), yavas parola-hash'leme (Argon2/BCrypt) yerine SHA-256 yeterli
     * (GitHub/Stripe API anahtarlari da ayni sekilde hash'lenir). */
    @Column(name = "anahtar_hash", nullable = false)
    private String anahtarHash;

    /** Goruntulemede kullanilan onek+son4 - orn "whsk_live_ab12...wxyz". */
    @Column(name = "anahtar_onek", nullable = false)
    private String anahtarOnek;

    @Column(nullable = false)
    private Instant olusturulma;

    @Column(name = "iptal_edilme")
    private Instant iptalEdilme;

    protected ApiAnahtari() {
    }

    public ApiAnahtari(UUID organizasyonId, String anahtarHash, String anahtarOnek) {
        this.id = UUID.randomUUID();
        this.organizasyonId = organizasyonId;
        this.anahtarHash = anahtarHash;
        this.anahtarOnek = anahtarOnek;
        this.olusturulma = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizasyonId() {
        return organizasyonId;
    }

    public String getAnahtarHash() {
        return anahtarHash;
    }

    public String getAnahtarOnek() {
        return anahtarOnek;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }

    public Instant getIptalEdilme() {
        return iptalEdilme;
    }

    public boolean gecerliMi() {
        return iptalEdilme == null;
    }

    public void iptalEt() {
        this.iptalEdilme = Instant.now();
    }
}
