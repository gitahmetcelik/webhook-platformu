-- webhook semasi V1 (motorun V1-V5'i kullandigi icin numaralandirma V6'dan basliyor -
-- tek Flyway zinciri iki lokasyonu (motor-migration + migration) birlikte yonetiyor,
-- bkz gorev-motoru README-motor.md).
--
-- Tablolar ACIKCA "webhook." ile nitelenir: default-schema "motor" (motorun kendi V1-V5'i
-- sema nitelemesi yapmadigi icin buna muhtac), bu yuzden bizim kendi tablolarimizin dogru
-- semaya gitmesi icin biz nitelemek zorundayiz.

CREATE TABLE webhook.organizasyon (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad           TEXT NOT NULL,
    olusturulma  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhook.api_anahtari (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizasyon_id   UUID NOT NULL REFERENCES webhook.organizasyon (id),
    anahtar_hash      TEXT NOT NULL,
    olusturulma       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhook.uygulama (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizasyon_id   UUID NOT NULL REFERENCES webhook.organizasyon (id),
    ad                TEXT NOT NULL,
    ortam             TEXT NOT NULL DEFAULT 'prod',
    olusturulma       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhook.endpoint (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uygulama_id    UUID NOT NULL REFERENCES webhook.uygulama (id),
    url            TEXT NOT NULL,
    -- AES/GCM ile sifreli saklanir (bkz SifrelemeServisi), duz metin degil.
    imza_secret    TEXT NOT NULL,
    olay_filtresi  TEXT[] NOT NULL DEFAULT '{}',
    devre_durumu   TEXT NOT NULL DEFAULT 'KAPALI'
                       CHECK (devre_durumu IN ('KAPALI', 'YARI_ACIK', 'ACIK')),
    olusturulma    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhook.olay (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uygulama_id    UUID NOT NULL REFERENCES webhook.uygulama (id),
    tip            TEXT NOT NULL,
    payload        JSONB NOT NULL,
    dis_kaynak_id  TEXT NOT NULL,
    olusturulma    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (uygulama_id, dis_kaynak_id)
);

CREATE TABLE webhook.teslimat (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    olay_id      UUID NOT NULL REFERENCES webhook.olay (id),
    endpoint_id  UUID NOT NULL REFERENCES webhook.endpoint (id),
    -- motordaki gorevin UUID'si - urun ile motor arasindaki tek bag.
    gorev_id     UUID NOT NULL,
    durum        TEXT NOT NULL DEFAULT 'KUYRUKTA'
                     CHECK (durum IN ('KUYRUKTA', 'BASARILI', 'HATALI')),
    olusturulma  TIMESTAMPTZ NOT NULL DEFAULT now(),
    guncellenme  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_teslimat_olay ON webhook.teslimat (olay_id);
CREATE INDEX idx_teslimat_endpoint_durum ON webhook.teslimat (endpoint_id, durum);

-- motorun gorev_denemeleri tablosunu kopyalamaz, HTTP'ye ozgu alanlari tutar.
CREATE TABLE webhook.teslimat_denemesi (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teslimat_id    UUID NOT NULL REFERENCES webhook.teslimat (id),
    deneme_no      INT NOT NULL,
    istek_zamani   TIMESTAMPTZ NOT NULL DEFAULT now(),
    sure_ms        INT,
    http_durum     INT,
    -- ilk 4KB'a kirpilir, uygulama katmaninda.
    yanit_govdesi  TEXT,
    hata           TEXT,
    UNIQUE (teslimat_id, deneme_no)
);
