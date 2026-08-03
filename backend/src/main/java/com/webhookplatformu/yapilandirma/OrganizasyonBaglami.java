package com.webhookplatformu.yapilandirma;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** Istek-kapsamli: ApiAnahtariFiltresi tarafindan Authorization basligindan cozulup doldurulur. */
@Component
@RequestScope
public class OrganizasyonBaglami {

    private UUID organizasyonId;

    public UUID getOrganizasyonId() {
        return organizasyonId;
    }

    public void setOrganizasyonId(UUID organizasyonId) {
        this.organizasyonId = organizasyonId;
    }
}
