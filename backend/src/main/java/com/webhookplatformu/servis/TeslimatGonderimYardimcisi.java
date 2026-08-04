package com.webhookplatformu.servis;

import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.guvenlik.HmacImzalayici;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDenemesi;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bir teslimatı imzalayıp gerçekten HTTP POST eden, denemeyi kaydeden ortak mantık.
 * {@link com.webhookplatformu.motor.TeslimatHandlerTemel} (motor üzerinden retry ile) ve
 * sağlık sondası (Faz 2.4, tek seferlik deneme) ikisi de bunu kullanır — imzalama/HTTP/kayıt
 * mantığının iki yerde ayrışması istenmiyor.
 */
@Component
public class TeslimatGonderimYardimcisi {

    private static final Duration HTTP_ZAMAN_ASIMI = Duration.ofSeconds(10);
    private static final Set<Integer> KALICI_HATA_KODLARI = Set.of(400, 401, 403, 404, 410, 422);

    public enum SonucTuru { BASARILI, KALICI_HATA, GECICI_HATA }

    public record Sonuc(SonucTuru tur, Integer httpDurum, String retryAfterBasligi) {
    }

    private final SifrelemeServisi sifrelemeServisi;
    private final HmacImzalayici hmacImzalayici;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;
    private final TeslimatMetrikleri teslimatMetrikleri;
    private final HttpClient httpClient;

    public TeslimatGonderimYardimcisi(SifrelemeServisi sifrelemeServisi, HmacImzalayici hmacImzalayici,
                                       TeslimatDenemesiRepository teslimatDenemesiRepository,
                                       TeslimatMetrikleri teslimatMetrikleri) {
        this.sifrelemeServisi = sifrelemeServisi;
        this.hmacImzalayici = hmacImzalayici;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
        this.teslimatMetrikleri = teslimatMetrikleri;
        // HTTP_1_1'e SABITLENMIS bilincli: varsayilan (HTTP_2) JDK istemcisi her yeni baglantida
        // once bir h2c (cleartext HTTP/2) yukseltme deniyor - musteri endpoint'leri (neredeyse
        // hicbiri HTTP/2 sunmaz) bu yukseltmeyi 101 ile onaylamayinca istemci govdesiz/bozuk bir
        // "yoklama" istegi gonderip ardindan gercek istegi AYRI bir fiziksel HTTP isteginde
        // tekrar yolluyor - bizim tarafimizdan TEK bir deneme olarak gorunse de alici tarafta
        // birden fazla fiziksel istek olarak goruluyor (Faz 5.1 Testcontainers testinde imza
        // dogrulama senaryosunda gercekten yakalandi - govde bos geldigi icin HMAC uyusmuyordu).
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(HTTP_ZAMAN_ASIMI).build();
    }

    public Sonuc gonderVeKaydet(Teslimat teslimat, Endpoint endpoint, String payloadJson, int denemeNo)
            throws IOException, InterruptedException {
        TeslimatDenemesi deneme = new TeslimatDenemesi(teslimat.getId(), denemeNo);
        String secret = sifrelemeServisi.cozumle(endpoint.getImzaSecret());
        byte[] govde = payloadJson.getBytes(StandardCharsets.UTF_8);
        HmacImzalayici.ImzaBasliklari imza = hmacImzalayici.imzala(secret, govde);

        HttpRequest istek = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .timeout(HTTP_ZAMAN_ASIMI)
                .header("Content-Type", "application/json")
                .header("X-Webhook-Id", imza.webhookId())
                .header("X-Webhook-Timestamp", imza.zamanDamgasi())
                .header("X-Webhook-Signature", imza.imza())
                .POST(HttpRequest.BodyPublishers.ofByteArray(govde))
                .build();

        long baslangicMs = System.currentTimeMillis();
        try {
            HttpResponse<String> yanit = httpClient.send(istek, HttpResponse.BodyHandlers.ofString());
            int sureMs = (int) (System.currentTimeMillis() - baslangicMs);
            int durumKodu = yanit.statusCode();

            if (durumKodu >= 200 && durumKodu < 300) {
                deneme.basariliSonucla(durumKodu, yanit.body(), sureMs);
                teslimatDenemesiRepository.save(deneme);
                teslimatMetrikleri.denemeSuresiKaydet("basarili", Duration.ofMillis(sureMs));
                return new Sonuc(SonucTuru.BASARILI, durumKodu, null);
            }

            deneme.hataliSonucla("HTTP " + durumKodu, durumKodu, sureMs);
            teslimatDenemesiRepository.save(deneme);

            if (KALICI_HATA_KODLARI.contains(durumKodu)) {
                teslimatMetrikleri.denemeSuresiKaydet("kalici_hata", Duration.ofMillis(sureMs));
                return new Sonuc(SonucTuru.KALICI_HATA, durumKodu, null);
            }
            teslimatMetrikleri.denemeSuresiKaydet("gecici_hata", Duration.ofMillis(sureMs));
            String retryAfter = yanit.headers().firstValue("Retry-After").orElse(null);
            return new Sonuc(SonucTuru.GECICI_HATA, durumKodu, retryAfter);
        } catch (IOException e) {
            int sureMs = (int) (System.currentTimeMillis() - baslangicMs);
            deneme.hataliSonucla(e.getMessage(), null, sureMs);
            teslimatDenemesiRepository.save(deneme);
            teslimatMetrikleri.denemeSuresiKaydet("gecici_hata", Duration.ofMillis(sureMs));
            return new Sonuc(SonucTuru.GECICI_HATA, null, null);
        }
    }
}
