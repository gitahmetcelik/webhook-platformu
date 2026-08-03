package com.webhookplatformu.api.dto;

import java.util.UUID;

/** {@code anahtar} sadece bu yanitta bir kez doner, sonra bir daha gosterilmez. */
public record ApiAnahtariUretimYaniti(UUID id, String anahtar) {
}
