package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teslimat", schema = "webhook")
public class Teslimat {

    @Id
    private UUID id;

    @Column(name = "olay_id", nullable = false)
    private UUID olayId;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    /** Motordaki görevin UUID'si — ürün ile motor arasındaki tek bağ. */
    @Column(name = "gorev_id", nullable = false)
    private UUID gorevId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeslimatDurumu durum;

    @Column(nullable = false)
    private Instant olusturulma;

    @Column(nullable = false)
    private Instant guncellenme;

    protected Teslimat() {
    }

    /**
     * {@code id} dışarıdan verilir çünkü motor idempotency anahtarı
     * ({@code org:{orgId}:teslimat:{teslimatId}}) teslimat kaydı DB'ye yazılmadan ÖNCE,
     * {@code gorevGonderici.gonder()} çağrılırken zaten bilinmesi gereken bir değer — teslimat
     * id'si önce üretilip motora geçiliyor, dönen gorevId ile birlikte buraya veriliyor.
     */
    public Teslimat(UUID id, UUID olayId, UUID endpointId, UUID gorevId) {
        this.id = id;
        this.olayId = olayId;
        this.endpointId = endpointId;
        this.gorevId = gorevId;
        this.durum = TeslimatDurumu.KUYRUKTA;
        Instant simdi = Instant.now();
        this.olusturulma = simdi;
        this.guncellenme = simdi;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOlayId() {
        return olayId;
    }

    public UUID getEndpointId() {
        return endpointId;
    }

    public UUID getGorevId() {
        return gorevId;
    }

    public TeslimatDurumu getDurum() {
        return durum;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }

    public Instant getGuncellenme() {
        return guncellenme;
    }

    public void durumGuncelle(TeslimatDurumu yeniDurum) {
        this.durum = yeniDurum;
        this.guncellenme = Instant.now();
    }
}
