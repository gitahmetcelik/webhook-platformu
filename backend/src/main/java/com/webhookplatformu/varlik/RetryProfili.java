package com.webhookplatformu.varlik;

/**
 * Motor {@code maxDeneme}'yi runtime'da değil {@code @GorevTipi} anotasyonundan sabit okuyor
 * (bkz gorev-motoru Faz 0.4 bulgusu) — bu yüzden endpoint'e özgü, sürekli değişebilen bir
 * retry politikası yerine 3 sabit profil var, her biri ayrı bir görev tipine (ve dolayısıyla
 * ayrı bir {@code GorevHandler} sınıfına) karşılık geliyor.
 */
public enum RetryProfili {
    HIZLI("webhook.teslimat.hizli"),
    STANDART("webhook.teslimat.standart"),
    UZUN("webhook.teslimat.uzun");

    private final String gorevTipi;

    RetryProfili(String gorevTipi) {
        this.gorevTipi = gorevTipi;
    }

    public String getGorevTipi() {
        return gorevTipi;
    }
}
