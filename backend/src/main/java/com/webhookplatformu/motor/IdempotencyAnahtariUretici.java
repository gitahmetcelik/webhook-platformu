package com.webhookplatformu.motor;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Motorun {@code idempotency_anahtari} kolonu global UNIQUE olduğu için {@code org:} öneki
 * zorunlu. Anahtar {@code teslimat} id'sine bağlanır, {@code olay} id'sine değil — böylece
 * manuel yeniden-gönderim yeni bir teslimat satırı → yeni id → yeni anahtar → gerçekten
 * yeniden çalışır. Başka hiçbir yerde bu string birleştirmesi tekrarlanmamalı.
 */
@Component
public class IdempotencyAnahtariUretici {

    public String uret(UUID organizasyonId, UUID teslimatId) {
        return "org:" + organizasyonId + ":teslimat:" + teslimatId;
    }
}
