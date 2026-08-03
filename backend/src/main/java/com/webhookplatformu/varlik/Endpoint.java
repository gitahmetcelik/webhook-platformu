package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "endpoint", schema = "webhook")
public class Endpoint {

    /** Devre kesici eşiği — bu kadar ardışık kalıcı hatadan sonra devre açılır (bkz Faz 2.4). */
    public static final int DEVRE_ESIGI = 20;

    @Id
    private UUID id;

    @Column(name = "uygulama_id", nullable = false)
    private UUID uygulamaId;

    @Column(nullable = false)
    private String url;

    /** AES/GCM ile şifreli (bkz {@code SifrelemeServisi}) — asla düz metin olarak saklanmaz. */
    @Column(name = "imza_secret", nullable = false)
    private String imzaSecret;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "olay_filtresi", nullable = false, columnDefinition = "text[]")
    private String[] olayFiltresi;

    @Enumerated(EnumType.STRING)
    @Column(name = "devre_durumu", nullable = false)
    private DevreDurumu devreDurumu;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_profili", nullable = false)
    private RetryProfili retryProfili;

    @Column(name = "ardisik_hata_sayisi", nullable = false)
    private int ardisikHataSayisi;

    @Column(nullable = false)
    private Instant olusturulma;

    protected Endpoint() {
    }

    public Endpoint(UUID uygulamaId, String url, String imzaSecretSifreli, String[] olayFiltresi,
                     RetryProfili retryProfili) {
        this.id = UUID.randomUUID();
        this.uygulamaId = uygulamaId;
        this.url = url;
        this.imzaSecret = imzaSecretSifreli;
        this.olayFiltresi = olayFiltresi;
        this.devreDurumu = DevreDurumu.KAPALI;
        this.retryProfili = retryProfili;
        this.ardisikHataSayisi = 0;
        this.olusturulma = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUygulamaId() {
        return uygulamaId;
    }

    public String getUrl() {
        return url;
    }

    public String getImzaSecret() {
        return imzaSecret;
    }

    public String[] getOlayFiltresi() {
        return olayFiltresi;
    }

    public DevreDurumu getDevreDurumu() {
        return devreDurumu;
    }

    public RetryProfili getRetryProfili() {
        return retryProfili;
    }

    public int getArdisikHataSayisi() {
        return ardisikHataSayisi;
    }

    public Instant getOlusturulma() {
        return olusturulma;
    }

    /** Faz 3.4 düzenleme formu — secret bilerek dışarıda tutuluyor, ayrı bir akışla döner. */
    public void guncelle(String url, String[] olayFiltresi, RetryProfili retryProfili) {
        this.url = url;
        this.olayFiltresi = olayFiltresi;
        this.retryProfili = retryProfili;
    }

    /** Boş filtre = tüm event tiplerine abone. Faz 2+'da glob eşleşmesi eklenecek. */
    public boolean olayTipineAboneMi(String olayTipi) {
        if (olayFiltresi == null || olayFiltresi.length == 0) {
            return true;
        }
        for (String filtre : olayFiltresi) {
            if (filtre.equals(olayTipi)) {
                return true;
            }
        }
        return false;
    }

    public boolean devreAcikMi() {
        return devreDurumu == DevreDurumu.ACIK;
    }

    /** Kalıcı hatadan sonra çağrılır — eşiği aşarsa devreyi açar. @return devre yeni açıldıysa true. */
    public boolean ardisikHataArtir() {
        this.ardisikHataSayisi++;
        if (this.ardisikHataSayisi >= DEVRE_ESIGI && devreDurumu == DevreDurumu.KAPALI) {
            this.devreDurumu = DevreDurumu.ACIK;
            return true;
        }
        return false;
    }

    public void ardisikHataSifirla() {
        this.ardisikHataSayisi = 0;
        this.devreDurumu = DevreDurumu.KAPALI;
    }

    /** Sağlık sondası bir deneme göndermeden önce devreyi yarı-açığa çeker. */
    public void yariAcikOlarakIsaretle() {
        this.devreDurumu = DevreDurumu.YARI_ACIK;
    }

    /** Devreyi elle (operasyon endpoint'i) veya sağlık sondası başarısızlığında yeniden açar. */
    public void devreyiAc() {
        this.devreDurumu = DevreDurumu.ACIK;
    }

    /** Elle sıfırlama (bkz {@code POST /v1/endpointler/{id}/devre-sifirla}). */
    public void devreyiKapat() {
        this.devreDurumu = DevreDurumu.KAPALI;
        this.ardisikHataSayisi = 0;
    }
}
