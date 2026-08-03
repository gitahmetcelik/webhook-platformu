package com.webhookplatformu.motor;

import com.gorevplatformu.motorcekirdek.GorevBaglami;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OlayRepository;
import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.servis.DevreKesiciYardimcisi;
import com.webhookplatformu.servis.KullanimSayaciServisi;
import com.webhookplatformu.servis.TeslimatGonderimYardimcisi;
import com.webhookplatformu.servis.TeslimatGonderimYardimcisi.Sonuc;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Olay;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bir teslimatı imzalayıp müşterinin endpoint'ine POST eden ortak mantık — gerçek imzalama/
 * HTTP/kayıt işi {@link TeslimatGonderimYardimcisi}'nda, burada sadece motorla ilişkili karar
 * (retry için exception fırlat / fırlatma, durum güncelle, devre kesici sayaçları) var.
 *
 * <p>Motor {@code maxDeneme}'yi runtime'da değil {@code @GorevTipi} anotasyonundan sabit okuyor
 * (bkz gorev-motoru Faz 0.4 bulgusu) — bu yüzden farklı retry profillerinin her biri kendi
 * {@code @GorevTipi} değeriyle ayrı bir alt sınıf olarak var oluyor (bkz
 * {@link TeslimatHizliHandler}, {@link TeslimatStandartHandler}, {@link TeslimatUzunHandler}).
 */
public abstract class TeslimatHandlerTemel implements GorevHandler<TeslimatPayload> {

    private static final Logger log = LoggerFactory.getLogger(TeslimatHandlerTemel.class);

    private final TeslimatRepository teslimatRepository;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;
    private final EndpointRepository endpointRepository;
    private final OlayRepository olayRepository;
    private final TeslimatGonderimYardimcisi gonderimYardimcisi;
    private final DevreKesiciYardimcisi devreKesiciYardimcisi;
    private final KullanimSayaciServisi kullanimSayaciServisi;

    protected TeslimatHandlerTemel(TeslimatRepository teslimatRepository,
                                    TeslimatDenemesiRepository teslimatDenemesiRepository,
                                    EndpointRepository endpointRepository, OlayRepository olayRepository,
                                    TeslimatGonderimYardimcisi gonderimYardimcisi,
                                    DevreKesiciYardimcisi devreKesiciYardimcisi,
                                    KullanimSayaciServisi kullanimSayaciServisi) {
        this.teslimatRepository = teslimatRepository;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
        this.endpointRepository = endpointRepository;
        this.olayRepository = olayRepository;
        this.gonderimYardimcisi = gonderimYardimcisi;
        this.devreKesiciYardimcisi = devreKesiciYardimcisi;
        this.kullanimSayaciServisi = kullanimSayaciServisi;
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
        Sonuc sonuc = gonderimYardimcisi.gonderVeKaydet(teslimat, endpoint, olay.getPayload(), denemeNo);

        return switch (sonuc.tur()) {
            case BASARILI -> {
                teslimat.durumGuncelle(TeslimatDurumu.BASARILI);
                teslimatRepository.save(teslimat);
                endpoint.ardisikHataSifirla();
                endpointRepository.save(endpoint);
                kullanimSayaciServisi.artir(teslimat.getOrganizasyonId(), true);
                yield null;
            }
            case KALICI_HATA -> {
                // Handler exception FIRLATMAZ - motorun bosuna yeniden denemesini engellemek
                // icin bu, motorun bakis acisindan basariyla TAMAMLANMIS bir gorev (bkz Faz 2.2).
                teslimat.durumGuncelle(TeslimatDurumu.KALICI_HATA);
                teslimatRepository.save(teslimat);
                devreKesiciYardimcisi.kaliciBasarisizlikBildir(endpoint);
                kullanimSayaciServisi.artir(teslimat.getOrganizasyonId(), false);
                yield null;
            }
            case GECICI_HATA -> {
                if (sonuc.httpDurum() != null && sonuc.httpDurum() == 429 && sonuc.retryAfterBasligi() != null) {
                    // Motorun backoff hesaplayicisi parametrize degil (bkz gorev-motoru Faz 0.4/0.9
                    // bulgulari) - Retry-After'i motora iletebilecegimiz bir yol yok, sadece logluyoruz.
                    log.info("429 alindi, Retry-After={} (motorun kendi backoff'u kullaniliyor, "
                            + "bu deger sadece bilgi amacli)", sonuc.retryAfterBasligi());
                }
                throw new IllegalStateException("Endpoint gecici hata dondu: HTTP " + sonuc.httpDurum());
            }
        };
    }
}
