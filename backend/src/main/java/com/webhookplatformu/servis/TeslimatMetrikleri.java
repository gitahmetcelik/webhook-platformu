package com.webhookplatformu.servis;

import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.varlik.DevreDurumu;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Urun seviyesi Prometheus metrikleri (bkz Faz 5.2). Motorun kendi metrikleri
 * ({@code gorev.sonuc}, {@code gorev.isleme.suresi}, {@code gorev.kuyruk.derinlik},
 * {@code gorev.yeniden_deneme}) GOREV seviyesinde - burada olculen sey URUN seviyesindeki
 * teslimat: bir teslimat motorda birden fazla gorev denemesi yasayabilir, "teslimat basarili
 * oldu mu" sorusu motorun "gorev tamamlandi mi" sorusundan farkli (orn. kalici hata motor
 * icin BASARIYLA TAMAMLANMIS bir gorev, urun icin basarisiz bir teslimattir - bkz
 * {@code TeslimatHandlerTemel}).
 */
@Component
public class TeslimatMetrikleri {

    private final MeterRegistry meterRegistry;

    public TeslimatMetrikleri(MeterRegistry meterRegistry, EndpointRepository endpointRepository) {
        this.meterRegistry = meterRegistry;

        // Canli okunuyor - Prometheus scrape ettigi anda sorgulanir, ayri bir polling thread'i yok
        // (motorun kuyruk derinligi gauge'lariyla ayni desen).
        Gauge.builder("webhook.devre.acik", endpointRepository,
                        depo -> depo.countByDevreDurumu(DevreDurumu.ACIK))
                .description("Devresi acik (teslimat kabul etmeyen) endpoint sayisi")
                .register(meterRegistry);
    }

    /**
     * @param sonuc {@code basarili} | {@code kalici_hata} | {@code gecici_hata} | {@code dlq}
     */
    public void teslimatSonuclandi(String sonuc) {
        meterRegistry.counter("webhook.teslimat.sonuc", "sonuc", sonuc).increment();
    }

    /** Tek bir HTTP teslimat denemesinin suresi - p50/p95 icin histogram olarak yayinlanir. */
    public void denemeSuresiKaydet(String sonuc, Duration sure) {
        Timer.builder("webhook.teslimat.suresi")
                .tag("sonuc", sonuc)
                .description("Musteri endpoint'ine yapilan tek bir HTTP teslimat denemesinin suresi")
                // publishPercentileHistogram: p50/p95'in Prometheus tarafinda (histogram_quantile
                // ile) hesaplanabilmesi icin bucket'lar yayinlanir - istemci tarafinda hesaplanan
                // publishPercentiles'in aksine bu degerler instance'lar arasinda toplanabilir.
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(sure);
    }
}
