package com.webhookplatformu.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.webhookplatformu.api.dto.EndpointYaniti;
import com.webhookplatformu.servis.EndpointSaglikHesaplayici;
import com.webhookplatformu.servis.EndpointSaglikIzleyici;
import com.webhookplatformu.varlik.RetryProfili;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/** Faz 5.3 kapi testi: saglik skoru ve esik altina dusunce uretilen uyari. */
class SaglikSkoruTestleri extends UctanUcaOrtakAyarlar {

    @Autowired
    private EndpointSaglikIzleyici saglikIzleyici;

    @BeforeEach
    void testAliciyiSifirla() {
        testAliciSifirla();
    }

    @Test
    void trafikYokkenSkorBilinmiyorOlarakDoner() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);

        EndpointYaniti endpoint = endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId());

        // Hic teslimat yok - skor "kotu" degil "bilinmiyor" (null), uyari da uretilmemeli.
        assertThat(endpoint.saglikSkoru()).isNull();
        assertThat(endpoint.basariOraniSon24Saat()).isNull();
        assertThat(endpoint.saglikUyarisiAktif()).isFalse();
    }

    @Test
    void basariliTrafikYuksekSkorUretir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        UUID teslimatId = basariliTeslimatBekle(kiraci);
        assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                .isEqualTo(TeslimatDurumu.BASARILI);

        EndpointYaniti endpoint = endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId());

        assertThat(endpoint.basariOraniSon24Saat()).isEqualTo(100.0);
        // Yerel bir container'a teslimat hizli - basari 100 + gecikme yuksek puan => esigin ustu.
        assertThat(endpoint.saglikSkoru()).isNotNull().isGreaterThanOrEqualTo(
                EndpointSaglikHesaplayici.UYARI_ESIGI);
        assertThat(endpoint.ortalamaGecikmeMs()).isNotNull().isPositive();
    }

    @Test
    void skorEsikAltinaDusunceUyariVeAuditKaydiUretilir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("404");

        // Kalici hata: tek denemede KALICI_HATA, retry yok - basari orani 0 olur.
        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("i", 1));
        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.KALICI_HATA));

        EndpointYaniti dusukSkorlu = endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId());
        assertThat(dusukSkorlu.basariOraniSon24Saat()).isZero();
        assertThat(dusukSkorlu.saglikSkoru()).isNotNull().isLessThan(EndpointSaglikHesaplayici.UYARI_ESIGI);

        // Periyodik dongüyu beklemeden dogrudan tetikle (dongü de ayni metodu cagiriyor).
        saglikIzleyici.kontrolEt(endpointRepository.findById(kiraci.endpointId()).orElseThrow());

        assertThat(endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId()).saglikUyarisiAktif()).isTrue();
        assertThat(auditKayitlariniOku(kiraci)).contains("SAGLIK_SKORU_DUSTU");
    }

    @Test
    void uyariAyniDurumdaTekrarTekrarUretilmez() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("404");

        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("i", 1));
        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.KALICI_HATA));

        // Ucce kadar cagir - yalnizca ILK cagri gecis oldugu icin tek kayit olusmali.
        for (int i = 0; i < 3; i++) {
            saglikIzleyici.kontrolEt(endpointRepository.findById(kiraci.endpointId()).orElseThrow());
        }

        String auditGovdesi = auditKayitlariniOku(kiraci);
        int kayitSayisi = auditGovdesi.split("SAGLIK_SKORU_DUSTU", -1).length - 1;
        assertThat(kayitSayisi).isEqualTo(1);
    }

    private String auditKayitlariniOku(Kiraci kiraci) {
        var yanit = hamIstek(HttpMethod.GET, "/v1/audit?size=100", kiraci.apiAnahtari());
        assertThat(yanit.getStatusCode()).isEqualTo(HttpStatus.OK);
        return yanit.getBody();
    }

    private UUID basariliTeslimatBekle(Kiraci kiraci) {
        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("tutar", 1));
        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));
        return teslimatId;
    }

    private UUID tekTeslimatliOlayGonder(Kiraci kiraci, String olayTipi, Map<String, Object> payload) {
        var yanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(), "idem-" + UUID.randomUUID(),
                olayTipi, payload);
        assertThat(yanit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return govdeyiOlayYanitiOlarakOku(yanit).teslimatIdleri().get(0);
    }
}
