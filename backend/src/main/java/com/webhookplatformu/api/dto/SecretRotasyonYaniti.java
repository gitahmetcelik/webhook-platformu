package com.webhookplatformu.api.dto;

import java.time.Instant;

/** {@code yeniSecret} sadece bu yanitta bir kez doner, sonra bir daha gosterilmez. */
public record SecretRotasyonYaniti(String yeniSecret, Instant eskiSecretGecerlilikBitis) {
}
