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

    /**
     * Devre kesici eşiğinin varsayılanı — bu kadar ardışık kalıcı hatadan sonra devre açılır
     * (bkz Faz 2.4). Gerçek eşik {@code webhook.devre-esigi} ile değiştirilebilir ve
     * {@link #ardisikHataArtir(int)}'a parametre olarak geçirilir (bkz {@code
     * DevreKesiciYardimcisi}) — demo/test senaryolarında 20 başarısız teslimat üretmek
     * pratik olmadığı için.
     */
    public static final int VARSAYILAN_DEVRE_ESIGI = 20;

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

    /** Rotasyon sirasinda bir onceki secret - 24 saatlik pencerede imza dogrulamada hala kabul edilir. */
    @Column(name = "eski_imza_secret")
    private String eskiImzaSecret;

    @Column(name = "eski_secret_gecerlilik_bitis")
    private Instant eskiSecretGecerlilikBitis;

    /** Saniyede en fazla kac teslimat gonderilecegi (bkz Faz 4.3) - null = sinirsiz. */
    @Column(name = "hiz_siniri_sn")
    private Integer hizSiniriSn;

    /** Motora en son planlanan gonderim zamani - siradaki teslimatin ne zaman planlanacagini hesaplamak icin. */
    @Column(name = "son_planlanan_zaman")
    private Instant sonPlanlananZaman;

    /**
     * Saglik skoru esigin altina dustugunde true olur (bkz Faz 5.3). Skorun kendisi
     * saklanmiyor - bu bayrak sadece "uyari zaten uretildi mi" sorusunu cevapliyor, boylece
     * periyodik kontrol ayni uyariyi tekrar tekrar yazmiyor.
     */
    @Column(name = "saglik_uyarisi_aktif", nullable = false)
    private boolean saglikUyarisiAktif;

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

    public String getEskiImzaSecret() {
        return eskiImzaSecret;
    }

    public Instant getEskiSecretGecerlilikBitis() {
        return eskiSecretGecerlilikBitis;
    }

    public Integer getHizSiniriSn() {
        return hizSiniriSn;
    }

    public Instant getSonPlanlananZaman() {
        return sonPlanlananZaman;
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
    public void guncelle(String url, String[] olayFiltresi, RetryProfili retryProfili, Integer hizSiniriSn) {
        this.url = url;
        this.olayFiltresi = olayFiltresi;
        this.retryProfili = retryProfili;
        this.hizSiniriSn = hizSiniriSn;
    }

    /**
     * Yeni secret'i aktif eder, eskisini {@code graceSaat} saat boyunca imza dogrulamada
     * kabul edilir tutar (bkz Faz 4.1). Biz gonderen taraf oldugumuz icin YENI teslimatlar
     * hemen yeni secret ile imzalanir - "eski gecerli kalir" sadece dogrulama penceresi icin.
     */
    public void secretRotasyonuBaslat(String yeniImzaSecretSifreli, long graceSaat) {
        this.eskiImzaSecret = this.imzaSecret;
        this.eskiSecretGecerlilikBitis = Instant.now().plusSeconds(graceSaat * 3600);
        this.imzaSecret = yeniImzaSecretSifreli;
    }

    public boolean eskiSecretGecerliMi() {
        return eskiImzaSecret != null && eskiSecretGecerlilikBitis != null
                && Instant.now().isBefore(eskiSecretGecerlilikBitis);
    }

    /** Hiz siniri varsa siradaki teslimatin planlanacagi en erken zamani hesaplayip ilerletir. */
    public Instant siradakiPlanlamayiHesaplaVeIlerlet() {
        if (hizSiniriSn == null || hizSiniriSn <= 0) {
            return null;
        }
        Instant simdi = Instant.now();
        long araMs = 1000L / hizSiniriSn;
        Instant enErken = sonPlanlananZaman == null ? simdi : sonPlanlananZaman.plusMillis(araMs);
        Instant planlanan = enErken.isAfter(simdi) ? enErken : simdi;
        this.sonPlanlananZaman = planlanan;
        return planlanan.equals(simdi) ? null : planlanan;
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
    public boolean ardisikHataArtir(int devreEsigi) {
        this.ardisikHataSayisi++;
        if (this.ardisikHataSayisi >= devreEsigi && devreDurumu == DevreDurumu.KAPALI) {
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

    public boolean isSaglikUyarisiAktif() {
        return saglikUyarisiAktif;
    }

    /**
     * Uyari durumunu gunceller. @return durum GERCEKTEN degistiyse true — cagiran taraf
     * yalnizca gecis aninda audit kaydi yazsin diye (bkz {@code EndpointSaglikIzleyici}).
     */
    public boolean saglikUyarisiniGuncelle(boolean yeniDurum) {
        if (this.saglikUyarisiAktif == yeniDurum) {
            return false;
        }
        this.saglikUyarisiAktif = yeniDurum;
        return true;
    }
}
