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

    /** Denormalize edildi (bkz Faz 4 planlama notu) - org-scoped sorgular icin JOIN gerektirmez. */
    @Column(name = "organizasyon_id", nullable = false)
    private UUID organizasyonId;

    /**
     * Motordaki görevin UUID'si — ürün ile motor arasındaki tek bağ. Devre açıkken
     * ({@link TeslimatDurumu#BEKLEMEDE}) teslimat hiç motora gönderilmediği için null olabilir;
     * devre kapanıp kuyruğa alınınca {@link #gorevGonderildi(UUID)} ile sonradan doldurulur.
     */
    @Column(name = "gorev_id")
    private UUID gorevId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeslimatDurumu durum;

    /** Bu teslimat bir yeniden-gönderimse, hangi teslimattan doğduğu (bkz Faz 2.3). */
    @Column(name = "ana_teslimat_id")
    private UUID anaTeslimatId;

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
    public Teslimat(UUID id, UUID olayId, UUID endpointId, UUID organizasyonId, UUID gorevId) {
        this(id, olayId, endpointId, organizasyonId, gorevId, null);
    }

    /** Bir DLQ yeniden-gönderiminden doğan teslimat için — {@code anaTeslimatId} eskiye bağlar. */
    public Teslimat(UUID id, UUID olayId, UUID endpointId, UUID organizasyonId, UUID gorevId, UUID anaTeslimatId) {
        this.id = id;
        this.olayId = olayId;
        this.endpointId = endpointId;
        this.organizasyonId = organizasyonId;
        this.gorevId = gorevId;
        this.anaTeslimatId = anaTeslimatId;
        this.durum = TeslimatDurumu.KUYRUKTA;
        Instant simdi = Instant.now();
        this.olusturulma = simdi;
        this.guncellenme = simdi;
    }

    /** Devre açıkken oluşturulan, motora hiç gönderilmemiş bir teslimat. */
    public static Teslimat beklemede(UUID id, UUID olayId, UUID endpointId, UUID organizasyonId) {
        Teslimat teslimat = new Teslimat();
        teslimat.id = id;
        teslimat.olayId = olayId;
        teslimat.endpointId = endpointId;
        teslimat.organizasyonId = organizasyonId;
        teslimat.durum = TeslimatDurumu.BEKLEMEDE;
        Instant simdi = Instant.now();
        teslimat.olusturulma = simdi;
        teslimat.guncellenme = simdi;
        return teslimat;
    }

    /** Devre kapanıp {@code BEKLEMEDE} bir teslimat kuyruğa alınırken çağrılır. */
    public void gorevGonderildi(UUID gorevId) {
        this.gorevId = gorevId;
        durumGuncelle(TeslimatDurumu.KUYRUKTA);
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

    public UUID getOrganizasyonId() {
        return organizasyonId;
    }

    public UUID getGorevId() {
        return gorevId;
    }

    public TeslimatDurumu getDurum() {
        return durum;
    }

    public UUID getAnaTeslimatId() {
        return anaTeslimatId;
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
