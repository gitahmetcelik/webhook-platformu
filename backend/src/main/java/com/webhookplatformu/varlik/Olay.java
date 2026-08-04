package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "olay", schema = "webhook")
public class Olay {

    @Id
    private UUID id;

    @Column(name = "uygulama_id", nullable = false)
    private UUID uygulamaId;

    /** Denormalize edildi (bkz Faz 4 planlama notu) - org-scoped sorgular icin JOIN gerektirmez. */
    @Column(name = "organizasyon_id", nullable = false)
    private UUID organizasyonId;

    @Column(nullable = false)
    private String tip;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "dis_kaynak_id", nullable = false)
    private String disKaynakId;

    @Column(nullable = false)
    private Instant olusturulma;

    /** Giris istegine ait trace id - ondan dogan tum teslimatlara da ayni deger yazilir (bkz Faz 5.2). */
    @Column(name = "trace_id")
    private String traceId;

    protected Olay() {
    }

    public Olay(UUID uygulamaId, UUID organizasyonId, String tip, String payload, String disKaynakId,
                 String traceId) {
        this.id = UUID.randomUUID();
        this.uygulamaId = uygulamaId;
        this.organizasyonId = organizasyonId;
        this.tip = tip;
        this.payload = payload;
        this.disKaynakId = disKaynakId;
        this.olusturulma = Instant.now();
        this.traceId = traceId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUygulamaId() {
        return uygulamaId;
    }

    public UUID getOrganizasyonId() {
        return organizasyonId;
    }

    public String getTip() {
        return tip;
    }

    public String getPayload() {
        return payload;
    }

    public String getDisKaynakId() {
        return disKaynakId;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }

    public String getTraceId() {
        return traceId;
    }
}
