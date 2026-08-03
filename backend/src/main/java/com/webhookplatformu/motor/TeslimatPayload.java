package com.webhookplatformu.motor;

import java.util.UUID;

/** {@code webhook.teslimat} görev tipinin payload'ı — handler bununla teslimat kaydını bulur. */
public record TeslimatPayload(UUID teslimatId) {
}
