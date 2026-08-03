package com.webhookplatformu.varlik;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Motorun {@code gorev_denemeleri} tablosunu kopyalamaz — HTTP'ye özgü alanları tutar
 * (status kodu, yanıt gövdesi ilk 4KB, süre). UI/API bunu okur, motor şemasını değil.
 */
@Entity
@Table(name = "teslimat_denemesi", schema = "webhook")
public class TeslimatDenemesi {

    static final int YANIT_GOVDESI_MAKS_UZUNLUK = 4096;

    @Id
    private UUID id;

    @Column(name = "teslimat_id", nullable = false)
    private UUID teslimatId;

    @Column(name = "deneme_no", nullable = false)
    private Integer denemeNo;

    @Column(name = "istek_zamani", nullable = false)
    private Instant istekZamani;

    @Column(name = "sure_ms")
    private Integer sureMs;

    @Column(name = "http_durum")
    private Integer httpDurum;

    @Column(name = "yanit_govdesi")
    private String yanitGovdesi;

    private String hata;

    protected TeslimatDenemesi() {
    }

    public TeslimatDenemesi(UUID teslimatId, Integer denemeNo) {
        this.id = UUID.randomUUID();
        this.teslimatId = teslimatId;
        this.denemeNo = denemeNo;
        this.istekZamani = Instant.now();
    }

    public void basariliSonucla(int httpDurum, String yanitGovdesi, int sureMs) {
        this.httpDurum = httpDurum;
        this.yanitGovdesi = kirp(yanitGovdesi);
        this.sureMs = sureMs;
    }

    public void hataliSonucla(String hataMesaji, Integer httpDurum, int sureMs) {
        this.httpDurum = httpDurum;
        this.hata = hataMesaji;
        this.sureMs = sureMs;
    }

    private static String kirp(String metin) {
        if (metin == null || metin.length() <= YANIT_GOVDESI_MAKS_UZUNLUK) {
            return metin;
        }
        return metin.substring(0, YANIT_GOVDESI_MAKS_UZUNLUK);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeslimatId() {
        return teslimatId;
    }

    public Integer getDenemeNo() {
        return denemeNo;
    }

    public Instant getIstekZamani() {
        return istekZamani;
    }

    public Integer getSureMs() {
        return sureMs;
    }

    public Integer getHttpDurum() {
        return httpDurum;
    }

    public String getYanitGovdesi() {
        return yanitGovdesi;
    }

    public String getHata() {
        return hata;
    }
}
