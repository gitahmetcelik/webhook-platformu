package com.webhookplatformu.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.webhookplatformu.api.dto.OlayOlusturmaYaniti;
import com.webhookplatformu.api.dto.TeslimatOzetiYaniti;
import com.webhookplatformu.varlik.RetryProfili;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Faz 5.2 kapi testi. Grafana dashboard'u ({@code gozlemlenebilirlik/grafana-dashboard.json})
 * metrik ISIMLERINE gore yazildigi icin bu testler o isimlerin Prometheus ciktisinda gercekten
 * boyle gorundugunu dogruluyor - Micrometer'in nokta->alt-cizgi donusumu, counter'a {@code _total},
 * timer'a {@code _seconds_bucket} eklemesi gibi kurallar varsayimla birakilmiyor.
 *
 * <p>{@code @AutoConfigureObservability} ZORUNLU: Spring Boot testlerde metrik/trace export'unu
 * varsayilan olarak kapatiyor ({@code management.defaults.metrics.export.enabled=false}), bu da
 * PrometheusMeterRegistry'nin hic olusmamasina ve {@code /actuator/prometheus}'un 404 donmesine
 * yol aciyor - uygulama yapilandirmasi dogru olsa bile (gercekten calistirilinca bulundu:
 * /actuator kok linkleri sadece health+info gosteriyordu).</p>
 */
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
class GozlemlenebilirlikTestleri extends UctanUcaOrtakAyarlar {

    @BeforeEach
    void testAliciyiSifirla() {
        testAliciSifirla();
    }

    @Test
    void ayniOlaydanDoganTumTeslimatlarAyniTraceIdyiTasir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        endpointEkle(kiraci, RetryProfili.HIZLI);

        ResponseEntity<String> yanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(),
                "idem-" + UUID.randomUUID(), "siparis.olusturuldu", Map.of("tutar", 10));
        assertThat(yanit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OlayOlusturmaYaniti govde = govdeyiOlayYanitiOlarakOku(yanit);

        // Tek olay -> iki endpoint -> iki teslimat.
        assertThat(govde.teslimatIdleri()).hasSize(2);

        List<TeslimatOzetiYaniti> teslimatlar = govde.teslimatIdleri().stream()
                .map(id -> teslimatOzetiGetir(kiraci.apiAnahtari(), id))
                .toList();

        String ilkTrace = teslimatlar.get(0).traceId();
        assertThat(ilkTrace).isNotBlank();
        // Asil iddia: motorun gorev-basina trace'i degil, GIRIS ISTEGININ trace'i tasiniyor -
        // bridge olmadan bu iki teslimat farkli (rastgele) id alirdi (bkz Faz 5.2 bulgusu).
        assertThat(teslimatlar).allSatisfy(t -> assertThat(t.traceId()).isEqualTo(ilkTrace));
    }

    @Test
    void olayVeTeslimatAyniTraceIdyiPaylasir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);

        ResponseEntity<String> yanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(),
                "idem-" + UUID.randomUUID(), "siparis.olusturuldu", Map.of("tutar", 11));
        OlayOlusturmaYaniti govde = govdeyiOlayYanitiOlarakOku(yanit);
        UUID teslimatId = govde.teslimatIdleri().get(0);

        String teslimatTrace = teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).traceId();

        // Olay listesinden ayni olayin trace'ini oku.
        ResponseEntity<String> olaylarYaniti = hamIstek(org.springframework.http.HttpMethod.GET,
                "/v1/uygulamalar/" + kiraci.uygulamaId() + "/olaylar", kiraci.apiAnahtari());
        assertThat(olaylarYaniti.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(olaylarYaniti.getBody()).contains("\"traceId\":\"" + teslimatTrace + "\"");
    }

    @Test
    void prometheusCiktisiDashboardunBekledigiMetrikleriIcerir() {
        Kiraci kiraci = yeniKiraciOlustur(RetryProfili.HIZLI);
        ResponseEntity<String> yanit = olayGonder(kiraci.uygulamaId(), kiraci.apiAnahtari(),
                "idem-" + UUID.randomUUID(), "siparis.olusturuldu", Map.of("tutar", 12));
        UUID teslimatId = govdeyiOlayYanitiOlarakOku(yanit).teslimatIdleri().get(0);

        Awaitility.await().atMost(BEKLEME_ZAMAN_ASIMI).pollInterval(BEKLEME_ARALIGI).untilAsserted(() ->
                assertThat(teslimatOzetiGetir(kiraci.apiAnahtari(), teslimatId).durum())
                        .isEqualTo(TeslimatDurumu.BASARILI));

        String cikti = prometheusCiktisi();

        // grafana-dashboard.json'daki sorgularin dayandigi tam isimler:
        assertThat(cikti).contains("webhook_teslimat_sonuc_total");
        assertThat(cikti).contains("sonuc=\"basarili\"");
        assertThat(cikti).contains("webhook_teslimat_suresi_seconds_bucket");
        assertThat(cikti).contains("webhook_devre_acik");
        // Motorun kendi metrikleri de ayni scrape'te gorunmeli (dashboard ikisini birlikte cizer).
        assertThat(cikti).contains("gorev_kuyruk_derinlik");
    }
}
