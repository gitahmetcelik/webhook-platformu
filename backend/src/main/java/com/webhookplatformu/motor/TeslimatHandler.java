package com.webhookplatformu.motor;

import com.gorevplatformu.motorcekirdek.GorevBaglami;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevTipi;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OlayRepository;
import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.guvenlik.HmacImzalayici;
import com.webhookplatformu.guvenlik.SifrelemeServisi;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Olay;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDenemesi;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Bir teslimatı imzalayıp müşterinin endpoint'ine POST eder. 2xx dışı yanıt veya
 * bağlantı/timeout hatasında exception fırlatır — retry kararı tamamen motora ait
 * (bkz {@code @GorevTipi(maxDeneme=...)}), burada elle retry/backoff yapılmaz.
 */
@Component
@GorevTipi(value = "webhook.teslimat", maxDeneme = 5, timeoutSaniye = 15)
public class TeslimatHandler implements GorevHandler<TeslimatPayload> {

    private static final Duration HTTP_ZAMAN_ASIMI = Duration.ofSeconds(10);

    private final TeslimatRepository teslimatRepository;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;
    private final EndpointRepository endpointRepository;
    private final OlayRepository olayRepository;
    private final SifrelemeServisi sifrelemeServisi;
    private final HmacImzalayici hmacImzalayici;
    private final HttpClient httpClient;

    public TeslimatHandler(TeslimatRepository teslimatRepository,
                            TeslimatDenemesiRepository teslimatDenemesiRepository,
                            EndpointRepository endpointRepository, OlayRepository olayRepository,
                            SifrelemeServisi sifrelemeServisi, HmacImzalayici hmacImzalayici) {
        this.teslimatRepository = teslimatRepository;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
        this.endpointRepository = endpointRepository;
        this.olayRepository = olayRepository;
        this.sifrelemeServisi = sifrelemeServisi;
        this.hmacImzalayici = hmacImzalayici;
        this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_ZAMAN_ASIMI).build();
    }

    @Override
    public Class<TeslimatPayload> payloadTipi() {
        return TeslimatPayload.class;
    }

    @Override
    public Object calistir(TeslimatPayload payload, GorevBaglami baglam) throws IOException, InterruptedException {
        Teslimat teslimat = teslimatRepository.findById(payload.teslimatId())
                .orElseThrow(() -> new IllegalStateException("Teslimat bulunamadi: " + payload.teslimatId()));
        Endpoint endpoint = endpointRepository.findById(teslimat.getEndpointId())
                .orElseThrow(() -> new IllegalStateException("Endpoint bulunamadi: " + teslimat.getEndpointId()));
        Olay olay = olayRepository.findById(teslimat.getOlayId())
                .orElseThrow(() -> new IllegalStateException("Olay bulunamadi: " + teslimat.getOlayId()));

        int denemeNo = (int) teslimatDenemesiRepository.countByTeslimatId(teslimat.getId()) + 1;
        TeslimatDenemesi deneme = new TeslimatDenemesi(teslimat.getId(), denemeNo);

        String secret = sifrelemeServisi.cozumle(endpoint.getImzaSecret());
        byte[] govde = olay.getPayload().getBytes(StandardCharsets.UTF_8);
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

            if (yanit.statusCode() >= 200 && yanit.statusCode() < 300) {
                deneme.basariliSonucla(yanit.statusCode(), yanit.body(), sureMs);
                teslimatDenemesiRepository.save(deneme);
                teslimat.durumGuncelle(TeslimatDurumu.BASARILI);
                teslimatRepository.save(teslimat);
                return null;
            }

            deneme.hataliSonucla("Beklenmeyen HTTP durumu", yanit.statusCode(), sureMs);
            teslimatDenemesiRepository.save(deneme);
            throw new IllegalStateException("Endpoint basarisiz yanit dondu: HTTP " + yanit.statusCode());
        } catch (IOException e) {
            int sureMs = (int) (System.currentTimeMillis() - baslangicMs);
            deneme.hataliSonucla(e.getMessage(), null, sureMs);
            teslimatDenemesiRepository.save(deneme);
            throw e;
        }
    }
}
