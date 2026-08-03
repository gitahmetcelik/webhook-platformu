package com.webhookplatformu.api.dto;

import com.gorevplatformu.motorcekirdek.GorevDurumu;
import com.gorevplatformu.motorspringstarter.GorevOzeti;

public record MotorGorevOzetiYaniti(GorevDurumu durum, int denemeSayisi, String sonHata, String traceId) {

    public static MotorGorevOzetiYaniti of(GorevOzeti ozet) {
        return new MotorGorevOzetiYaniti(ozet.durum(), ozet.denemeSayisi(), ozet.sonHata(), ozet.traceId());
    }
}
