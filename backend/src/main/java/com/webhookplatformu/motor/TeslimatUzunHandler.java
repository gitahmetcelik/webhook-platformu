package com.webhookplatformu.motor;

import com.gorevplatformu.motorcekirdek.GorevTipi;
import com.webhookplatformu.depo.EndpointRepository;
import com.webhookplatformu.depo.OlayRepository;
import com.webhookplatformu.depo.TeslimatDenemesiRepository;
import com.webhookplatformu.depo.TeslimatRepository;
import com.webhookplatformu.servis.DevreKesiciYardimcisi;
import com.webhookplatformu.servis.TeslimatGonderimYardimcisi;
import org.springframework.stereotype.Component;

@Component
@GorevTipi(value = "webhook.teslimat.uzun", maxDeneme = 15, timeoutSaniye = 15)
public class TeslimatUzunHandler extends TeslimatHandlerTemel {

    public TeslimatUzunHandler(TeslimatRepository teslimatRepository,
                                TeslimatDenemesiRepository teslimatDenemesiRepository,
                                EndpointRepository endpointRepository, OlayRepository olayRepository,
                                TeslimatGonderimYardimcisi gonderimYardimcisi,
                                DevreKesiciYardimcisi devreKesiciYardimcisi) {
        super(teslimatRepository, teslimatDenemesiRepository, endpointRepository, olayRepository,
                gonderimYardimcisi, devreKesiciYardimcisi);
    }
}
