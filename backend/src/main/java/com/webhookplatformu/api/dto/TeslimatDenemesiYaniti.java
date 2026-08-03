package com.webhookplatformu.api.dto;

import com.webhookplatformu.varlik.TeslimatDenemesi;
import java.time.Instant;

public record TeslimatDenemesiYaniti(int denemeNo, Instant istekZamani, Integer sureMs, Integer httpDurum,
                                      String yanitGovdesi, String hata) {

    public static TeslimatDenemesiYaniti of(TeslimatDenemesi deneme) {
        return new TeslimatDenemesiYaniti(deneme.getDenemeNo(), deneme.getIstekZamani(), deneme.getSureMs(),
                deneme.getHttpDurum(), deneme.getYanitGovdesi(), deneme.getHata());
    }
}
