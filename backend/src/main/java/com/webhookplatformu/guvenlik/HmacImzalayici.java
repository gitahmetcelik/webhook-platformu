package com.webhookplatformu.guvenlik;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * X-Webhook-Id / X-Webhook-Timestamp / X-Webhook-Signature başlıklarını üretir.
 * İmzalanan içerik: {@code {webhookId}.{timestamp}.{rawBody}} (HMAC-SHA256, base64).
 * test-alici bu şemayı birebir doğrular (bkz test-alici/server.js).
 */
@Component
public class HmacImzalayici {

    private static final String HMAC_ALGORITMASI = "HmacSHA256";

    public record ImzaBasliklari(String webhookId, String zamanDamgasi, String imza) {
    }

    public ImzaBasliklari imzala(String secret, byte[] govde) {
        String webhookId = "msg_" + UUID.randomUUID();
        String zamanDamgasi = String.valueOf(Instant.now().getEpochSecond());
        String imzalananIcerik = webhookId + "." + zamanDamgasi + "." + new String(govde, StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITMASI);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITMASI));
            byte[] imzaBaytlari = mac.doFinal(imzalananIcerik.getBytes(StandardCharsets.UTF_8));
            String imza = "v1," + Base64.getEncoder().encodeToString(imzaBaytlari);
            return new ImzaBasliklari(webhookId, zamanDamgasi, imza);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Imzalama basarisiz", e);
        }
    }
}
