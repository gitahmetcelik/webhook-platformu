package com.webhookplatformu.guvenlik;

import com.webhookplatformu.depo.ApiAnahtariRepository;
import com.webhookplatformu.varlik.ApiAnahtari;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** API anahtari uretimi/dogrulamasi (bkz Faz 4.1). */
@Component
public class ApiAnahtariServisi {

    private static final String ONEK = "whsk_live_";
    private final SecureRandom secureRandom = new SecureRandom();
    private final ApiAnahtariRepository apiAnahtariRepository;

    public ApiAnahtariServisi(ApiAnahtariRepository apiAnahtariRepository) {
        this.apiAnahtariRepository = apiAnahtariRepository;
    }

    public record UretilenAnahtar(UUID id, String duzAnahtar) {
    }

    public UretilenAnahtar uret(UUID organizasyonId) {
        byte[] rastgele = new byte[24];
        secureRandom.nextBytes(rastgele);
        String duzAnahtar = ONEK + Base64.getUrlEncoder().withoutPadding().encodeToString(rastgele);
        String hash = hashla(duzAnahtar);
        String onek = duzAnahtar.substring(0, ONEK.length() + 6) + "..." + duzAnahtar.substring(duzAnahtar.length() - 4);
        ApiAnahtari anahtar = new ApiAnahtari(organizasyonId, hash, onek);
        apiAnahtariRepository.save(anahtar);
        return new UretilenAnahtar(anahtar.getId(), duzAnahtar);
    }

    public Optional<UUID> dogrula(String duzAnahtar) {
        return apiAnahtariRepository.findByAnahtarHash(hashla(duzAnahtar))
                .filter(ApiAnahtari::gecerliMi)
                .map(ApiAnahtari::getOrganizasyonId);
    }

    private String hashla(String duzAnahtar) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBaytlari = digest.digest(duzAnahtar.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBaytlari);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 kullanilamiyor", e);
        }
    }
}
