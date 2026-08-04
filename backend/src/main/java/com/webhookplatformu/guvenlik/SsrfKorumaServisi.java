package com.webhookplatformu.guvenlik;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Endpoint URL'i musteri tarafindan girilir ve worker o adrese istek atar - urunun en yuksek
 * riskli yuzeyi (bkz SPEC SS13.C). Iki noktada cagrilir: kayit/guncelleme aninda (EndpointController)
 * VE her gonderim oncesi (TeslimatGonderimYardimcisi) - kayit anindaki kontrol DNS rebinding'i
 * yakalayamaz, o yuzden gonderim aninda YENIDEN cozumlenip dogrulanir (SS13.C MUST).
 *
 * <p>VARSAYIM: JDK 21'in {@code java.net.http.HttpClient}'i ozel bir DNS resolver enjeksiyonuna
 * izin vermiyor (bu API daha sonraki JDK surumlerinde preview olarak geldi) - yani "cozumle,
 * dogrula, TAM O IP'YE baglan" ile TOCTOU'yu sifira indirmek bu JDK'da mumkun degil. Burada
 * yapilan, pencereyi kayit-aninda-bir-kez'den her-gonderim-oncesi'ne indirerek daraltmak;
 * rebinding'i saniyeler mertebesinde bir TTL ile hala teorik olarak mumkun kilar. Tam cozum icin
 * ozel bir DNS cozumleyici + baglanti-seviyesi IP sabitleme gerekir (SS18'e eklenecek acik karar).
 */
@Component
public class SsrfKorumaServisi {

    public static class SsrfIhlali extends RuntimeException {
        public SsrfIhlali(String mesaj) {
            super(mesaj);
        }
    }

    private final boolean httpIzinli;
    private final boolean icAgIzinli;

    public SsrfKorumaServisi(@Value("${webhook.ssrf.http-izinli:false}") boolean httpIzinli,
                              // SADECE test/geliştirme için: e2e suite test-alici'ye Docker'ın
                              // kendi özel ağı üzerinden bağlanıyor (bkz UctanUcaOrtakAyarlar),
                              // yani "abone" IP'si üretimde asla kabul edilmeyecek bir aralıkta.
                              // Prod'da bu bayrak HİÇBİR ZAMAN true olmamalı (SS13.C).
                              @Value("${webhook.ssrf.ic-ag-izinli:false}") boolean icAgIzinli) {
        this.httpIzinli = httpIzinli;
        this.icAgIzinli = icAgIzinli;
    }

    /** URL kaydı/güncellemesi anında ve her gönderimden hemen önce çağrılır. */
    public void dogrula(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new SsrfIhlali("Geçersiz URL");
        }

        String sema = uri.getScheme();
        if (sema == null || !(sema.equalsIgnoreCase("https") || (httpIzinli && sema.equalsIgnoreCase("http")))) {
            throw new SsrfIhlali("Yalnız https şeması kabul edilir");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SsrfIhlali("Geçersiz host");
        }

        InetAddress[] adresler;
        try {
            adresler = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new SsrfIhlali("Host çözümlenemedi: " + host);
        }
        if (adresler.length == 0) {
            throw new SsrfIhlali("Host hiçbir IP'ye çözümlenmedi: " + host);
        }

        if (icAgIzinli) {
            return;
        }
        for (InetAddress adres : adresler) {
            if (izinliDegilMi(adres)) {
                throw new SsrfIhlali("Hedef IP izinli aralıkların dışında: " + adres.getHostAddress());
            }
        }
    }

    /** Dönen TÜM IP'ler reddedilir — loopback, private, link-local, metadata, multicast (SS13.C). */
    private boolean izinliDegilMi(InetAddress adres) {
        if (adres.isLoopbackAddress() || adres.isLinkLocalAddress() || adres.isSiteLocalAddress()
                || adres.isMulticastAddress() || adres.isAnyLocalAddress()) {
            return true;
        }
        if (adres instanceof Inet4Address v4) {
            byte[] b = v4.getAddress();
            int birinciOktet = b[0] & 0xFF;
            int ikinciOktet = b[1] & 0xFF;
            // 169.254.0.0/16 (AWS/GCP/Azure metadata servisi dahil) — isLinkLocalAddress zaten
            // bunu kapsar ama acikca de belirtiyoruz.
            if (birinciOktet == 169 && ikinciOktet == 254) return true;
            if (birinciOktet == 0) return true; // 0.0.0.0/8
            return false;
        }
        if (adres instanceof Inet6Address v6) {
            byte[] b = v6.getAddress();
            // fc00::/7 (unique local) — isSiteLocalAddress IPv6'da bunu kapsamayabiliyor.
            if ((b[0] & 0xFE) == 0xFC) return true;
            // IPv4-mapped IPv6 (::ffff:a.b.c.d) — gomulu IPv4 adresi ayrica dogrulanir.
            if (v6.isIPv4CompatibleAddress() || ipv4EslenmisMi(b)) {
                InetAddress gomulu;
                try {
                    gomulu = InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]});
                } catch (UnknownHostException e) {
                    return true;
                }
                return izinliDegilMi(gomulu);
            }
            return false;
        }
        return true;
    }

    private boolean ipv4EslenmisMi(byte[] b) {
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) return false;
        }
        return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
    }
}
