package com.webhookplatformu.servis;

import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.motor.IdempotencyAnahtariUretici;
import com.webhookplatformu.motor.TeslimatPayload;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import com.webhookplatformu.varlik.Uygulama;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Devre acikken {@link TeslimatDurumu#BEKLEMEDE} olarak biriken teslimatlari motora gonderir.
 *
 * <p><b>Neden ayri bir bilesen:</b> devre iki farkli yoldan kapanabiliyor — saglik sondasi
 * (otomatik) ve {@code POST /v1/endpointler/{id}/devre-sifirla} (elle). Bu mantik once sadece
 * saglik sondasinin icinde private bir metottu, dolayisiyla devre ELLE sifirlandiginda biriken
 * teslimatlar hic kuyruga alinmiyordu. Daha kotusu kalici bir kayipti: saglik sondasi yalnizca
 * devresi ACIK endpoint'leri tariyor, elle kapatilan endpoint bir daha hic taranmiyor ve
 * musteriye 201 ile kabul edildigi soylenmis event'ler sonsuza kadar teslim edilmeden kaliyordu
 * (Faz 5.6 kapi testinde, canli prod yiginda gercekten gozlemlendi).
 */
@Component
public class BekleyenTeslimatKuyruklayici {

    private static final Logger log = LoggerFactory.getLogger(BekleyenTeslimatKuyruklayici.class);

    private final TeslimatRepository teslimatRepository;
    private final UygulamaRepository uygulamaRepository;
    private final GorevGonderici gorevGonderici;
    private final IdempotencyAnahtariUretici idempotencyAnahtariUretici;

    public BekleyenTeslimatKuyruklayici(TeslimatRepository teslimatRepository,
                                         UygulamaRepository uygulamaRepository, GorevGonderici gorevGonderici,
                                         IdempotencyAnahtariUretici idempotencyAnahtariUretici) {
        this.teslimatRepository = teslimatRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.gorevGonderici = gorevGonderici;
        this.idempotencyAnahtariUretici = idempotencyAnahtariUretici;
    }

    /** @return kuyruga alinan teslimat sayisi. */
    public int birikenleriKuyrugaAl(Endpoint endpoint) {
        List<Teslimat> bekleyenler =
                teslimatRepository.findByEndpointIdAndDurum(endpoint.getId(), TeslimatDurumu.BEKLEMEDE);
        if (bekleyenler.isEmpty()) {
            return 0;
        }

        Uygulama uygulama = uygulamaRepository.findById(endpoint.getUygulamaId()).orElseThrow();
        for (Teslimat teslimat : bekleyenler) {
            String idempotencyAnahtari =
                    idempotencyAnahtariUretici.uret(uygulama.getOrganizasyonId(), teslimat.getId());
            UUID gorevId = gorevGonderici.gonder(endpoint.getRetryProfili().getGorevTipi(),
                    new TeslimatPayload(teslimat.getId()),
                    new GorevOpsiyonlari(idempotencyAnahtari, null, null));
            teslimat.gorevGonderildi(gorevId);
            teslimatRepository.save(teslimat);
        }

        log.info("Devre kapandi, bekleyen {} teslimat kuyruga alindi: endpoint={}",
                bekleyenler.size(), endpoint.getId());
        return bekleyenler.size();
    }
}
