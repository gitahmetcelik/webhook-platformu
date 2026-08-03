package com.webhookplatformu.guvenlik;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Endpoint imza secret'ını DB'de dinlendirmeden (plaintext) saklamamak için AES/GCM ile
 * simetrik şifreleme. Secret'ı geri okumak (HMAC imzalamak için) gerektiğinden hash değil,
 * çözülebilir şifreleme kullanılıyor.
 */
@Component
public class SifrelemeServisi {

    private static final int IV_UZUNLUK_BAYT = 12;
    private static final int TAG_UZUNLUK_BIT = 128;
    private static final String DONUSUM = "AES/GCM/NoPadding";

    private final SecretKeySpec anahtar;
    private final SecureRandom secureRandom = new SecureRandom();

    public SifrelemeServisi(@Value("${webhook.sifreleme.anahtar}") String base64Anahtar) {
        byte[] anahtarBaytlari = Base64.getDecoder().decode(base64Anahtar);
        this.anahtar = new SecretKeySpec(anahtarBaytlari, "AES");
    }

    public String sifrele(String duzMetin) {
        try {
            byte[] iv = new byte[IV_UZUNLUK_BAYT];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(DONUSUM);
            cipher.init(Cipher.ENCRYPT_MODE, anahtar, new GCMParameterSpec(TAG_UZUNLUK_BIT, iv));
            byte[] sifreliMetin = cipher.doFinal(duzMetin.getBytes(StandardCharsets.UTF_8));
            ByteBuffer tampon = ByteBuffer.allocate(iv.length + sifreliMetin.length);
            tampon.put(iv).put(sifreliMetin);
            return Base64.getEncoder().encodeToString(tampon.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sifreleme basarisiz", e);
        }
    }

    public String cozumle(String sifreliBase64) {
        try {
            byte[] tumVeri = Base64.getDecoder().decode(sifreliBase64);
            byte[] iv = Arrays.copyOfRange(tumVeri, 0, IV_UZUNLUK_BAYT);
            byte[] sifreliMetin = Arrays.copyOfRange(tumVeri, IV_UZUNLUK_BAYT, tumVeri.length);
            Cipher cipher = Cipher.getInstance(DONUSUM);
            cipher.init(Cipher.DECRYPT_MODE, anahtar, new GCMParameterSpec(TAG_UZUNLUK_BIT, iv));
            byte[] duzMetin = cipher.doFinal(sifreliMetin);
            return new String(duzMetin, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sifre cozme basarisiz", e);
        }
    }
}
