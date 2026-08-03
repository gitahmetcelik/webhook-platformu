package com.webhookplatformu.servis;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Giris tarafi rate limiting (bkz Faz 4.3) - org basina saniyede N event. Plan tek instance icin
 * in-memory kabul edilebilir diyor ("Redis eklemek istemiyorsan in-memory + tek instance kabul
 * edilebilir, README'de belirt") - bu urun henuz tek instance calisiyor (bkz README).
 */
@Component
public class GirisHizSiniricisi {

    private record Kova(long doluAnMs, double jeton) {
    }

    private final Map<UUID, Kova> kovalar = new ConcurrentHashMap<>();
    private final double saniyedeIzinVerilen;

    public GirisHizSiniricisi(@Value("${webhook.giris-hiz-siniri-sn:20}") double saniyedeIzinVerilen) {
        this.saniyedeIzinVerilen = saniyedeIzinVerilen;
    }

    public synchronized boolean izinVer(UUID organizasyonId) {
        long simdi = System.currentTimeMillis();
        Kova mevcut = kovalar.get(organizasyonId);
        double jeton;
        if (mevcut == null) {
            jeton = saniyedeIzinVerilen;
        } else {
            double gecenSaniye = (simdi - mevcut.doluAnMs()) / 1000.0;
            jeton = Math.min(saniyedeIzinVerilen, mevcut.jeton() + gecenSaniye * saniyedeIzinVerilen);
        }
        if (jeton < 1.0) {
            kovalar.put(organizasyonId, new Kova(simdi, jeton));
            return false;
        }
        kovalar.put(organizasyonId, new Kova(simdi, jeton - 1.0));
        return true;
    }
}
