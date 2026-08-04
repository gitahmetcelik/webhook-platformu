package com.webhookplatformu.servis;

import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Endpoint sagligini son 24 saatlik teslimat gecmisinden hesaplar (bkz Faz 3.4, Faz 5.3).
 *
 * <p>Skor saklanmiyor, her cagrida canli hesaplaniyor - "son 24 saat" kayan bir pencere oldugu
 * icin saklanmis bir deger zaten aninda bayatlardi (saklanan tek sey uyari bayragi, bkz
 * {@link EndpointSaglikIzleyici}).</p>
 */
@Component
public class EndpointSaglikHesaplayici {

    /** Bu skorun altina dusen endpoint icin uyari uretilir (bkz Faz 5.3). */
    public static final int UYARI_ESIGI = 70;

    /** Bu sureye kadar olan ortalama gecikme tam puan alir. */
    private static final double GECIKME_IYI_MS = 200;
    /** Bu sure ve ustu gecikme sifir puan alir. */
    private static final double GECIKME_KOTU_MS = 5000;

    /** Skorun %70'i basari orani, %30'u gecikme - basarisizlik gecikmeden onemli. */
    private static final double BASARI_AGIRLIGI = 0.7;
    private static final double GECIKME_AGIRLIGI = 0.3;

    private final TeslimatRepository teslimatRepository;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;

    public EndpointSaglikHesaplayici(TeslimatRepository teslimatRepository,
                                      TeslimatDenemesiRepository teslimatDenemesiRepository) {
        this.teslimatRepository = teslimatRepository;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
    }

    /** @param skor 0-100; trafik yoksa (skor hesaplanamiyorsa) alanlar null olur. */
    public record Saglik(Double basariOrani, Double ortalamaGecikmeMs, Integer skor) {

        public boolean uyariGerektirirMi() {
            return skor != null && skor < UYARI_ESIGI;
        }
    }

    public Saglik hesapla(Endpoint endpoint) {
        Instant esik = Instant.now().minus(24, ChronoUnit.HOURS);
        long toplam = teslimatRepository.countByEndpointIdAndOlusturulmaAfter(endpoint.getId(), esik);
        if (toplam == 0) {
            // Hic trafik yok - "kotu" degil, "bilinmiyor". Uyari da uretilmez.
            return new Saglik(null, null, null);
        }

        long basarili = teslimatRepository.countByEndpointIdAndDurumAndOlusturulmaAfter(
                endpoint.getId(), TeslimatDurumu.BASARILI, esik);
        double basariOrani = (basarili * 100.0) / toplam;

        Double ortalamaGecikmeMs = teslimatDenemesiRepository.ortalamaSureMs(endpoint.getId(), esik);
        double gecikmeSkoru = gecikmeSkoruHesapla(ortalamaGecikmeMs);

        int skor = (int) Math.round(BASARI_AGIRLIGI * basariOrani + GECIKME_AGIRLIGI * gecikmeSkoru);
        return new Saglik(basariOrani, ortalamaGecikmeMs, skor);
    }

    /** Geriye donuk uyumluluk: endpoint listesi/detayi Faz 3.4'ten beri sadece orani gosteriyordu. */
    public Double basariOraniSon24Saat(Endpoint endpoint) {
        return hesapla(endpoint).basariOrani();
    }

    /** {@code GECIKME_IYI_MS} ve altinda 100, {@code GECIKME_KOTU_MS} ve ustunde 0, arasi dogrusal. */
    private double gecikmeSkoruHesapla(Double ortalamaMs) {
        if (ortalamaMs == null) {
            // Teslimat var ama hic deneme kaydi yok (orn. hepsi devre acikken BEKLEMEDE'de kaldi) -
            // gecikme hakkinda bilgi yok, bu bileseni notr (tam puan) sayip skoru basari oranina birakiyoruz.
            return 100.0;
        }
        if (ortalamaMs <= GECIKME_IYI_MS) {
            return 100.0;
        }
        if (ortalamaMs >= GECIKME_KOTU_MS) {
            return 0.0;
        }
        return 100.0 * (1.0 - (ortalamaMs - GECIKME_IYI_MS) / (GECIKME_KOTU_MS - GECIKME_IYI_MS));
    }
}
