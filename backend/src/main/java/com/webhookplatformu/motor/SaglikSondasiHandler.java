package com.webhookplatformu.motor;

import com.gorevplatformu.motorcekirdek.GorevBaglami;
import com.gorevplatformu.motorcekirdek.GorevGonderici;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevOpsiyonlari;
import com.gorevplatformu.motorcekirdek.GorevTipi;
import com.webhookplatformu.depo.AuditKaydiRepository;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OlayRepository;
import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.depo.UygulamaRepository;
import com.webhookplatformu.servis.TeslimatGonderimYardimcisi;
import com.webhookplatformu.servis.TeslimatGonderimYardimcisi.Sonuc;
import com.webhookplatformu.varlik.AuditKaydi;
import com.webhookplatformu.varlik.DevreDurumu;
import com.webhookplatformu.varlik.Endpoint;
import com.webhookplatformu.varlik.Olay;
import com.webhookplatformu.varlik.Teslimat;
import com.webhookplatformu.varlik.TeslimatDurumu;
import com.webhookplatformu.varlik.Uygulama;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Devresi açık (ACIK) her endpoint için, bekleyen (BEKLEMEDE) bir teslimatı "yoklama" olarak
 * kullanıp gerçekten dener. Başarılıysa devre kapanır ve birikmiş BEKLEMEDE teslimatlar
 * sırayla motora gönderilir; başarısızsa devre tekrar ACIK'a döner (bkz Faz 2.4).
 */
@Component
@GorevTipi(value = "webhook.saglik-sondasi", maxDeneme = 1, timeoutSaniye = 30)
public class SaglikSondasiHandler implements GorevHandler<SaglikSondasiPayload> {

    private static final Logger log = LoggerFactory.getLogger(SaglikSondasiHandler.class);

    private final EndpointRepository endpointRepository;
    private final TeslimatRepository teslimatRepository;
    private final TeslimatDenemesiRepository teslimatDenemesiRepository;
    private final OlayRepository olayRepository;
    private final UygulamaRepository uygulamaRepository;
    private final AuditKaydiRepository auditKaydiRepository;
    private final TeslimatGonderimYardimcisi gonderimYardimcisi;
    private final GorevGonderici gorevGonderici;
    private final IdempotencyAnahtariUretici idempotencyAnahtariUretici;

    public SaglikSondasiHandler(EndpointRepository endpointRepository, TeslimatRepository teslimatRepository,
                                 TeslimatDenemesiRepository teslimatDenemesiRepository, OlayRepository olayRepository,
                                 UygulamaRepository uygulamaRepository, AuditKaydiRepository auditKaydiRepository,
                                 TeslimatGonderimYardimcisi gonderimYardimcisi, GorevGonderici gorevGonderici,
                                 IdempotencyAnahtariUretici idempotencyAnahtariUretici) {
        this.endpointRepository = endpointRepository;
        this.teslimatRepository = teslimatRepository;
        this.teslimatDenemesiRepository = teslimatDenemesiRepository;
        this.olayRepository = olayRepository;
        this.uygulamaRepository = uygulamaRepository;
        this.auditKaydiRepository = auditKaydiRepository;
        this.gonderimYardimcisi = gonderimYardimcisi;
        this.gorevGonderici = gorevGonderici;
        this.idempotencyAnahtariUretici = idempotencyAnahtariUretici;
    }

    @Override
    public Class<SaglikSondasiPayload> payloadTipi() {
        return SaglikSondasiPayload.class;
    }

    @Override
    public Object calistir(SaglikSondasiPayload payload, GorevBaglami baglam) throws Exception {
        List<Endpoint> acikEndpointler = endpointRepository.findByDevreDurumu(DevreDurumu.ACIK);
        for (Endpoint endpoint : acikEndpointler) {
            yoklaVeKarariUygula(endpoint);
        }
        return null;
    }

    private void yoklaVeKarariUygula(Endpoint endpoint) throws Exception {
        List<Teslimat> bekleyenler = teslimatRepository.findByEndpointIdAndDurum(endpoint.getId(), TeslimatDurumu.BEKLEMEDE);
        if (bekleyenler.isEmpty()) {
            return;
        }
        Teslimat yoklamaTeslimati = bekleyenler.get(0);
        Olay olay = olayRepository.findById(yoklamaTeslimati.getOlayId()).orElseThrow();

        endpoint.yariAcikOlarakIsaretle();
        endpointRepository.save(endpoint);

        int denemeNo = (int) teslimatDenemesiRepository.countByTeslimatId(yoklamaTeslimati.getId()) + 1;
        Sonuc sonuc = gonderimYardimcisi.gonderVeKaydet(yoklamaTeslimati, endpoint, olay.getPayload(), denemeNo);

        if (sonuc.tur() == TeslimatGonderimYardimcisi.SonucTuru.BASARILI) {
            yoklamaTeslimati.durumGuncelle(TeslimatDurumu.BASARILI);
            teslimatRepository.save(yoklamaTeslimati);

            endpoint.devreyiKapat();
            endpointRepository.save(endpoint);
            auditKaydiRepository.save(new AuditKaydi(yoklamaTeslimati.getOrganizasyonId(), "DEVRE_KAPANDI",
                    endpoint.getId(), "saglik sondasi basarili"));
            log.info("Endpoint devresi kapandi (saglik sondasi basarili): {}", endpoint.getId());

            birikenleriKuyrugaAl(endpoint);
        } else {
            endpoint.devreyiAc();
            endpointRepository.save(endpoint);
            log.info("Endpoint devresi acik kaliyor (saglik sondasi basarisiz): {}", endpoint.getId());
        }
    }

    private void birikenleriKuyrugaAl(Endpoint endpoint) {
        List<Teslimat> bekleyenler = teslimatRepository.findByEndpointIdAndDurum(endpoint.getId(), TeslimatDurumu.BEKLEMEDE);
        for (Teslimat teslimat : bekleyenler) {
            Uygulama uygulama = uygulamaRepository.findById(endpoint.getUygulamaId()).orElseThrow();
            String idempotencyAnahtari = idempotencyAnahtariUretici.uret(uygulama.getOrganizasyonId(), teslimat.getId());
            UUID gorevId = gorevGonderici.gonder(endpoint.getRetryProfili().getGorevTipi(),
                    new TeslimatPayload(teslimat.getId()), new GorevOpsiyonlari(idempotencyAnahtari, null, null));
            teslimat.gorevGonderildi(gorevId);
            teslimatRepository.save(teslimat);
        }
    }
}
