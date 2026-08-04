package com.webhookplatformu.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.webhookplatformu.api.dto.OlayOlusturmaYaniti;
import com.webhookplatformu.api.dto.TeslimatDetayYaniti;
import com.webhookplatformu.motor.SaglikSondasiPayload;
import com.webhookplatformu.varlik.DevreDurumu;
import com.webhookplatformu.varlik.Endpoint;
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
import org.springframework.http.ResponseEntity;

/**
 * Faz 5.1 kapi testi - plandaki 10 senaryonun her biri gercek Postgres+RabbitMQ+test-alici'ye
 * karsi, gercek HTTP cagrilariyla dogrulanir (bkz {@link UctanUcaOrtakAyarlar}). Her test kendi
 * izole kiracisini (organizasyon/uygulama/endpoint) olusturur, bu yuzden testler arasinda veri
 * paylasimi yok - sadece test-alici container'i paylasilir (bkz {@code testAliciSifirla()}).
 */
class TeslimatUctanUcaTestleri extends UctanUcaOrtakAyarlar {

    @Autowired
    private GorevGonderici gorevGonderici;

    @BeforeEach
    void testAliciyiSifirla() {
        testAliciSifirla();
    }

    @Test
    void basariliTeslimatGercektenUlasir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("tutar", 100));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));

        TeslimatDetayYaniti detay = teslimatDetayGetir(kiraci.apiAnahtari(), teslimatId);
        assertThat(detay.denemeler()).hasSize(1);
        assertThat(detay.denemeler().get(0).httpDurum()).isEqualTo(200);
    }

    @Test
    void imzaTestAlicidaGecerliDogrulanir() {
        // Bilincli olarak yeniKiraciOlustur (rastgele secret) KULLANMIYOR - test-alici container'i
        // testler arasinda paylasilan TEK WEBHOOK_SECRET'a karsi dogruluyor (bkz SABIT_IMZA_SECRET).
        var organizasyon = new com.webhookplatformu.varlik.Organizasyon("Imza Test Org " + UUID.randomUUID());
        organizasyonRepository.save(organizasyon);
        var uygulama = new com.webhookplatformu.varlik.Uygulama(organizasyon.getId(), "Imza Test App", "test");
        uygulamaRepository.save(uygulama);
        var apiAnahtari = apiAnahtariServisi.uret(organizasyon.getId());
        Endpoint endpoint = new Endpoint(uygulama.getId(), testAliciWebhookUrl(),
                sifrelemeServisi.sifrele(SABIT_IMZA_SECRET), new String[0], RetryProfili.HIZLI);
        endpointRepository.save(endpoint);

        ResponseEntity<String> yanit = olayGonder(uygulama.getId(), apiAnahtari.duzAnahtar(),
                "idem-" + UUID.randomUUID(), "siparis.olusturuldu", Map.of("tutar", 42));
        assertThat(yanit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID teslimatId = govdeyiOlayYanitiOlarakOku(yanit).teslimatIdleri().get(0);

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(apiAnahtari.duzAnahtar(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));

        // testAlici container'i testler arasinda paylasiliyor - baska testlerin (farkli,
        // rastgele secret'li endpoint'lerden) birikmis kayitlari da listede olabilir, onlar bu
        // sabit secret'a karsi hep "gecersiz" cikar. Bu yuzden toplam boyuta degil, SADECE bu
        // secret'la GERCEKTEN dogrulanan kayit sayisina bakiliyor.
        var gecerliImzali = testAliciAlinanlar().stream()
                .filter(kayit -> "gecerli".equals(kayit.get("imza")))
                .toList();
        assertThat(gecerliImzali).hasSize(1);
    }

    @Test
    void ayniIdempotencyKeyIkinciKezAyniOlayiDoner() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        String idempotencyKey = "idem-sabit-" + UUID.randomUUID();

        ResponseEntity<String> ilkYanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(), idempotencyKey,
                "siparis.olusturuldu", Map.of("tutar", 1));
        assertThat(ilkYanit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OlayOlusturmaYaniti ilkGovde = govdeyiOlayYanitiOlarakOku(ilkYanit);

        ResponseEntity<String> ikinciYanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(), idempotencyKey,
                "siparis.olusturuldu", Map.of("tutar", 1));
        assertThat(ikinciYanit.getStatusCode()).isEqualTo(HttpStatus.OK);
        OlayOlusturmaYaniti ikinciGovde = govdeyiOlayYanitiOlarakOku(ikinciYanit);

        assertThat(ikinciGovde.olayId()).isEqualTo(ilkGovde.olayId());
        assertThat(ikinciGovde.teslimatIdleri()).isEqualTo(ilkGovde.teslimatIdleri());
    }

    @Test
    void retryMerdiveniIkiBasarisizlikSonrasiUcuncuDenemedeBasarir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("akis", 2, "dizi-" + UUID.randomUUID(), null);
        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("tutar", 5));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));

        TeslimatDetayYaniti detay = teslimatDetayGetir(kiraci.apiAnahtari(), teslimatId);
        assertThat(detay.denemeler()).hasSize(3);
        assertThat(detay.denemeler().get(0).httpDurum()).isEqualTo(500);
        assertThat(detay.denemeler().get(1).httpDurum()).isEqualTo(500);
        assertThat(detay.denemeler().get(2).httpDurum()).isEqualTo(200);
    }

    @Test
    void retryBudcesiTukenenceDlqyaDuser() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("hata");
        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("tutar", 7));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.DLQ));

        assertThat(teslimatDetayGetir(kiraci.apiAnahtari(), teslimatId).denemeler()).hasSize(3);
    }

    @Test
    void dlqdanYenidenGonderimBasarirsaTeslimatBasariliOlur() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("hata");
        UUID eskiTeslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("tutar", 9));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), eskiTeslimatId).durum())
                        .isEqualTo(TeslimatDurumu.DLQ));

        testAliciModAyarla("ok");
        ResponseEntity<com.webhookplatformu.api.dto.TeslimatOzetiYaniti> yenidenGonderYaniti = restTemplate.exchange(
                "/v1/teslimatlar/" + eskiTeslimatId + "/yeniden-gonder", HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(yetkiliBasliklar(kiraci.apiAnahtari())),
                com.webhookplatformu.api.dto.TeslimatOzetiYaniti.class);
        assertThat(yenidenGonderYaniti.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID yeniTeslimatId = yenidenGonderYaniti.getBody().id();
        assertThat(yenidenGonderYaniti.getBody().anaTeslimatId()).isEqualTo(eskiTeslimatId);

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), yeniTeslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));

        // Eski (DLQ'ya dusmus) teslimat kaydi oldugu gibi kalir - timeline'in dogrulugu icin.
        assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), eskiTeslimatId).durum()).isEqualTo(TeslimatDurumu.DLQ);
    }

    @Test
    void kaliciHataTekDenemedeKesinlesirVeRetryDenenmez() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("404");
        UUID teslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("tutar", 3));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.KALICI_HATA));

        TeslimatDetayYaniti detay = teslimatDetayGetir(kiraci.apiAnahtari(), teslimatId);
        assertThat(detay.denemeler()).hasSize(1);
        assertThat(detay.denemeler().get(0).httpDurum()).isEqualTo(404);
        assertThat(endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId()).ardisikHataSayisi()).isEqualTo(1);
    }

    @Test
    void devreArdisikKaliciHatalardaAcilirSaglikSondasiylaKapanir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI, null, 10_000);
        testAliciModAyarla("404");

        for (int i = 0; i < TEST_DEVRE_ESIGI; i++) {
            tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("i", i));
        }

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId()).devreDurumu())
                        .isEqualTo(DevreDurumu.ACIK));

        UUID devreAcikkenGidenTeslimatId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("son", true));
        assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), devreAcikkenGidenTeslimatId).durum())
                .isEqualTo(TeslimatDurumu.BEKLEMEDE);

        testAliciModAyarla("ok");
        gorevGonderici.gonder("webhook.saglik-sondasi", new SaglikSondasiPayload(),
                new GorevOpsiyonlari("test:saglik-sondasi:" + UUID.randomUUID(), null, null));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId()).devreDurumu())
                        .isEqualTo(DevreDurumu.KAPALI));

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), devreAcikkenGidenTeslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));
    }

    @Test
    void devreElleSifirlanincaBekleyenTeslimatlarDaKuyrugaAlinir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        testAliciModAyarla("404");

        for (int i = 0; i < TEST_DEVRE_ESIGI; i++) {
            tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("i", i));
        }
        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(endpointDetayGetir(kiraci.apiAnahtari(), kiraci.endpointId()).devreDurumu())
                        .isEqualTo(DevreDurumu.ACIK));

        // Devre acikken gelen teslimat motora hic gonderilmez, BEKLEMEDE'de birikir.
        UUID bekleyenId = tekTeslimatliOlayGonder(kiraci, "siparis.olusturuldu", Map.of("bekleyen", true));
        assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), bekleyenId).durum())
                .isEqualTo(TeslimatDurumu.BEKLEMEDE);

        testAliciModAyarla("ok");
        ResponseEntity<String> sifirlaYaniti = restTemplate.exchange(
                "/v1/endpointler/" + kiraci.endpointId() + "/devre-sifirla", HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(yetkiliBasliklar(kiraci.apiAnahtari())), String.class);
        assertThat(sifirlaYaniti.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ASIL IDDIA: elle sifirlama bekleyenleri de kuyruga almali. Almasaydi bu teslimat
        // KALICI OLARAK BEKLEMEDE kalirdi - saglik sondasi yalnizca devresi ACIK endpoint'leri
        // tariyor, elle kapatilan endpoint'e bir daha hic bakmiyor (Faz 5.6'da canli yiginda
        // gercekten gozlemlendi: musteriye 201 denmis event'ler sessizce kayboluyordu).
        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), bekleyenId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));
    }

    @Test
    void baskaOrganizasyonunKaynagina404Doner() {
        Kiraci kiraciA = yeniKiraciOlustur(RetryProfili.HIZLI);
        Kiraci kiraciB = yeniKiraciOlustur(RetryProfili.HIZLI);
        UUID teslimatIdA = tekTeslimatliOlayGonder(kiraciA, "siparis.olusturuldu", Map.of("tutar", 1));

        assertThat(hamIstek(HttpMethod.GET, "/v1/teslimatlar/" + teslimatIdA, kiraciA.apiAnahtari()).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(hamIstek(HttpMethod.GET, "/v1/teslimatlar/" + teslimatIdA, kiraciB.apiAnahtari()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(hamIstek(HttpMethod.GET, "/v1/endpointler/" + kiraciA.endpointId(), kiraciB.apiAnahtari())
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aylikKotaAsilincaYeniOlay429Doner() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI, null, 2);
        testAliciModAyarla("ok");

        assertThat(olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(), "idem-" + UUID.randomUUID(),
                "siparis.olusturuldu", Map.of("i", 1)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(), "idem-" + UUID.randomUUID(),
                "siparis.olusturuldu", Map.of("i", 2)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(), "idem-" + UUID.randomUUID(),
                "siparis.olusturuldu", Map.of("i", 3)).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private UUID tekTeslimatliOlayGonder(Kiraci kiraci, String olayTipi, Map<String, Object> payload) {
        ResponseEntity<String> yanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(),
                "idem-" + UUID.randomUUID(), olayTipi, payload);
        assertThat(yanit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OlayOlusturmaYaniti govde = govdeyiOlayYanitiOlarakOku(yanit);
        assertThat(govde.teslimatIdleri()).hasSize(1);
        return govde.teslimatIdleri().get(0);
    }
}
