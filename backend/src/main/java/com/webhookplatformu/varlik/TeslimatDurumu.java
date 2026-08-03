package com.webhookplatformu.varlik;

public enum TeslimatDurumu {
    KUYRUKTA,
    BASARILI,
    HATALI,
    /** Motorun retry bütçesi tükendi, {@code olu_mektup_kutusu}na düştü (bkz Faz 2.3). */
    DLQ,
    /** Kalıcı (4xx tipi) hata — motor hiç retry etmedi, tek denemede kesin başarısız. */
    KALICI_HATA,
    /** Endpoint'in devresi açık (bkz Faz 2.4) — teslimat kuyruğa hiç girmedi. */
    BEKLEMEDE
}
