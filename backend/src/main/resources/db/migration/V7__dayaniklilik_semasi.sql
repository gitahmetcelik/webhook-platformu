-- Faz 2: retry profili, devre kesici, DLQ/kalici-hata/beklemede durumlari, yeniden-gonderim
-- zinciri, audit kaydi. default-schema "motor" oldugu icin (bkz V6 notu) her tablo acikca
-- "webhook." ile nitelenir.

ALTER TABLE webhook.endpoint
    ADD COLUMN retry_profili TEXT NOT NULL DEFAULT 'STANDART'
        CHECK (retry_profili IN ('HIZLI', 'STANDART', 'UZUN')),
    ADD COLUMN ardisik_hata_sayisi INT NOT NULL DEFAULT 0;

ALTER TABLE webhook.teslimat
    ADD COLUMN ana_teslimat_id UUID REFERENCES webhook.teslimat (id);

-- Devre acikken (BEKLEMEDE) teslimat hic motora gonderilmiyor - gorev_id olmuyor. Devre
-- kapanip kuyruga alinirken motora gonderilir ve gorev_id sonradan doldurulur.
ALTER TABLE webhook.teslimat ALTER COLUMN gorev_id DROP NOT NULL;

-- durum CHECK genisletiliyor: KUYRUKTA/BASARILI/HATALI (V6) + DLQ/KALICI_HATA/BEKLEMEDE (V7).
-- "migration'lar duzenlenmez, yenisi eklenir" kurali: V6'daki CHECK'i ALTER ile degistiriyoruz
-- (bu, V6'nin SQL metnini degil, DB semasini degistiriyor - checksum'i bozmuyor).
ALTER TABLE webhook.teslimat DROP CONSTRAINT teslimat_durum_check;
ALTER TABLE webhook.teslimat ADD CONSTRAINT teslimat_durum_check
    CHECK (durum IN ('KUYRUKTA', 'BASARILI', 'HATALI', 'DLQ', 'KALICI_HATA', 'BEKLEMEDE'));

CREATE TABLE webhook.audit_kaydi (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tur          TEXT NOT NULL,
    hedef_id     UUID NOT NULL,
    detay        TEXT,
    olusturulma  TIMESTAMPTZ NOT NULL DEFAULT now()
);
