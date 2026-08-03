package com.webhookplatformu.guvenlik;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** Endpoint için `whsec_` önekli, tek seferlik gösterilen imza secret'ı üretir. */
@Component
public class SecretUretici {

    private final SecureRandom secureRandom = new SecureRandom();

    public String uret() {
        byte[] rastgele = new byte[24];
        secureRandom.nextBytes(rastgele);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(rastgele);
    }
}
