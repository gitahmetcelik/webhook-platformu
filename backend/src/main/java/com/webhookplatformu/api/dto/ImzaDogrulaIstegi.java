package com.webhookplatformu.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Musteri tarafinin gercekte gordugu imza dogrulama girdisiyle birebir - X-Webhook-Id/
 * X-Webhook-Timestamp/X-Webhook-Signature basliklari + ham govde (bkz Faz 4.1 secret
 * rotasyonu test/debug araci).
 */
public record ImzaDogrulaIstegi(@NotBlank String webhookId, @NotBlank String zamanDamgasi, @NotBlank String govde,
                                 @NotBlank String imza) {
}
