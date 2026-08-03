package com.webhookplatformu.api.dto;

import com.webhookplatformu.servis.KullanimSayaciServisi.GunlukKullanim;
import java.time.LocalDate;

public record KullanimGunlukYaniti(LocalDate gun, int teslimatSayisi, int basarili, int basarisiz) {

    public static KullanimGunlukYaniti of(GunlukKullanim g) {
        return new KullanimGunlukYaniti(g.gun(), g.teslimatSayisi(), g.basarili(), g.basarisiz());
    }
}
