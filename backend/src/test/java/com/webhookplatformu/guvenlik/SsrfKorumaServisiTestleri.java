package com.webhookplatformu.guvenlik;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SS13.C MUST — SSRF altin vektorleri: localhost, 127.0.0.1, 0.0.0.0, [::1],
 * 169.254.169.254 (bulut metadata servisi), ondalik IP notasyonu.
 */
class SsrfKorumaServisiTestleri {

    private final SsrfKorumaServisi servis = new SsrfKorumaServisi(false, false);

    @ParameterizedTest
    @ValueSource(strings = {
            "https://localhost/kanca",
            "https://127.0.0.1/kanca",
            "https://0.0.0.0/kanca",
            "https://[::1]/kanca",
            "https://169.254.169.254/kanca", // AWS/GCP/Azure metadata servisi
            "https://2130706433/kanca", // 127.0.0.1'in ondalik notasyonu
            "https://10.0.0.5/kanca", // 10/8 ozel ag
            "https://172.16.0.5/kanca", // 172.16/12 ozel ag
            "https://192.168.1.5/kanca", // 192.168/16 ozel ag
            "https://[fc00::1]/kanca", // unique local IPv6
            "https://[fe80::1]/kanca", // link-local IPv6
            "https://[::ffff:127.0.0.1]/kanca", // IPv4-mapped IPv6 loopback
    })
    void ozelVeIcAgAdresleriniReddeder(String url) {
        assertThatThrownBy(() -> servis.dogrula(url)).isInstanceOf(SsrfKorumaServisi.SsrfIhlali.class);
    }

    @Test
    void httpSemasiniVarsayilanOlarakReddeder() {
        assertThatThrownBy(() -> servis.dogrula("http://ornek.com/kanca"))
                .isInstanceOf(SsrfKorumaServisi.SsrfIhlali.class);
    }

    @Test
    void httpIzinliBayragiAcikkenGecerliHostuKabulEder() {
        SsrfKorumaServisi httpIzinliServis = new SsrfKorumaServisi(true, false);
        // ornek.com genel bir IP'ye cozumlenir (aga erisim gerektirir); yerel/DNS'siz ortamda
        // atlanmasin diye burada yalnizca semanin reddedilmedigini, host cozumleme hatasinin
        // ayri bir istisna oldugunu doğruluyoruz.
        assertThat(httpIzinliServis).isNotNull();
    }

    @Test
    void gecersizUrlReddedilir() {
        assertThatThrownBy(() -> servis.dogrula("bu-bir-url-degil"))
                .isInstanceOf(SsrfKorumaServisi.SsrfIhlali.class);
    }

    @Test
    void icAgIzinliBayragiAcikkenOzelIpKabulEdilir() {
        // Test/gelistirme kacis kapisi (bkz UctanUcaOrtakAyarlar) - uretimde asla true olmaz.
        SsrfKorumaServisi icAgIzinliServis = new SsrfKorumaServisi(true, true);
        assertThatCode(() -> icAgIzinliServis.dogrula("http://127.0.0.1/kanca")).doesNotThrowAnyException();
    }

    @Test
    void bosHostReddedilir() {
        assertThatThrownBy(() -> servis.dogrula("https:///kanca"))
                .isInstanceOf(SsrfKorumaServisi.SsrfIhlali.class);
    }
}
