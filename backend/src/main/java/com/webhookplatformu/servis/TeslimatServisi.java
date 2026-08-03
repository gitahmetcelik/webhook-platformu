package com.webhookplatformu.servis;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.gorevplatformu.motorspringstarter.GorevOzeti;
import com.gorevplatformu.motorspringstarter.GorevYonetimServisi;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.motor.IdempotencyAnahtariUretici;
import com.webhookplatformu.motor.TeslimatPayload;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import com.webhookplatformu.varlik.Uygulama;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teslimat yaşam döngüsünün motorla kesiştiği iki nokta: (1) motorda retry bütçesi tükenip
 * DLQ'ya düşen görevleri kendi {@code teslimat} tablomuza yansıtmak, (2) yeniden-gönderim
 * (yeni teslimat satırı üretip motora gönderme).
 */
@Component
public class TeslimatServisi {

    private final TeslimatRepository teslimatRepository;
    private final EndpointRepository endpointRepository;
    private final UygulamaRepository uygulamaRepository;
    private final GorevYonetimServisi gorevYonetimServisi;
    private final GorevGonderici gorevGonderici;
    private final IdempotencyAnahtariUretici idempotencyAnahtariUretici;
    private final DevreKesiciYardimcisi devreKesiciYardimcisi;
    private final KullanimSayaciServisi kullanimSayaciServisi;

    public TeslimatServisi(TeslimatRepository teslimatRepository, EndpointRepository endpointRepository,
                            UygulamaRepository uygulamaRepository, GorevYonetimServisi gorevYonetimServisi,
                            GorevGonderici gorevGonderici, IdempotencyAnahtariUretici idempotencyAnahtariUretici,
                            DevreKesiciYardimcisi devreKesiciYardimcisi, KullanimSayaciServisi kullanimSayaciServisi) {
        this.teslimatRepository = teslimatRepository;
        this.endpointRepository = endpointRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.gorevYonetimServisi = gorevYonetimServisi;
        this.gorevGonderici = gorevGonderici;
        this.idempotencyAnahtariUretici = idempotencyAnahtariUretici;
        this.devreKesiciYardimcisi = devreKesiciYardimcisi;
        this.kullanimSayaciServisi = kullanimSayaciServisi;
    }

    /**
     * Motorun kendisi bir "görev DLQ'ya düştü" olayı yayınlamıyor (starter'da böyle bir
     * event/listener yok) — bu yüzden periyodik uzlaştırma yapılıyor:
     * {@code GorevYonetimServisi.ozet()} (Faz 0.5'te motora eklenen küçük sorgu yüzeyi) ile
     * hâlâ KUYRUKTA görünen teslimatların motordaki gerçek durumu kontrol ediliyor.
     * Birden fazla instance'ta aynı anda çalışması zararsız — durum güncellemesi idempotent
     * (aynı değeri tekrar yazmak no-op), bu yüzden ShedLock'a gerek görülmedi.
     */
    @Scheduled(fixedDelayString = "PT3S")
    @Transactional
    public void dlqUzlastir() {
        List<Teslimat> kuyruktakiler = teslimatRepository.findByDurumAndGorevIdIsNotNull(TeslimatDurumu.KUYRUKTA);
        for (Teslimat teslimat : kuyruktakiler) {
            Optional<GorevOzeti> ozet = gorevYonetimServisi.ozet(teslimat.getGorevId());
            if (ozet.isEmpty()) {
                continue;
            }
            if (ozet.get().durum() == GorevDurumu.BASARISIZ) {
                teslimat.durumGuncelle(TeslimatDurumu.DLQ);
                teslimatRepository.save(teslimat);

                Endpoint endpoint = endpointRepository.findById(teslimat.getEndpointId()).orElseThrow();
                devreKesiciYardimcisi.kaliciBasarisizlikBildir(endpoint);
                kullanimSayaciServisi.artir(teslimat.getOrganizasyonId(), false);
            }
        }
    }

    /**
     * DLQ'dan veya kalıcı hatadan yeniden gönderim: motorun {@code OluMektupKutusuServisi.yenidenGonder}
     * metodunu DOĞRUDAN çağırmıyoruz — plan gereği yeni bir teslimat satırı üretmek timeline'ı
     * doğru tutuyor (eski teslimat kaydı olduğu gibi kalır, {@code anaTeslimatId} ile yenisine bağlanır).
     */
    @Transactional
    public Teslimat yenidenGonder(UUID teslimatId) {
        Teslimat eskiTeslimat = teslimatRepository.findById(teslimatId)
                .orElseThrow(() -> new IllegalArgumentException("Teslimat bulunamadi: " + teslimatId));
        if (eskiTeslimat.getDurum() != TeslimatDurumu.DLQ && eskiTeslimat.getDurum() != TeslimatDurumu.KALICI_HATA) {
            throw new IllegalStateException(
                    "Bu durumdaki (" + eskiTeslimat.getDurum() + ") bir teslimat yeniden gonderilemez: " + teslimatId);
        }

        Endpoint endpoint = endpointRepository.findById(eskiTeslimat.getEndpointId()).orElseThrow();
        Uygulama uygulama = uygulamaRepository.findById(endpoint.getUygulamaId()).orElseThrow();

        UUID yeniTeslimatId = UUID.randomUUID();
        String idempotencyAnahtari = idempotencyAnahtariUretici.uret(uygulama.getOrganizasyonId(), yeniTeslimatId);
        UUID gorevId = gorevGonderici.gonder(endpoint.getRetryProfili().getGorevTipi(),
                new TeslimatPayload(yeniTeslimatId), new GorevOpsiyonlari(idempotencyAnahtari, null, null));

        Teslimat yeniTeslimat = new Teslimat(yeniTeslimatId, eskiTeslimat.getOlayId(), endpoint.getId(),
                uygulama.getOrganizasyonId(), gorevId, eskiTeslimat.getId());
        return teslimatRepository.save(yeniTeslimat);
    }
}
