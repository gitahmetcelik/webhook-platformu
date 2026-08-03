-- Faz 4: kimlik/izolasyon (API anahtari), kullanim olcumu+kota, rate limiting destegi
-- (endpoint hiz siniri icin planlama alani), audit genisletme, secret rotasyonu.
-- default-schema "motor" oldugu icin (bkz V6 notu) her tablo acikca "webhook." ile nitelenir.

-- api_anahtari tablosu V6'da onceden olusturulmustu (Faz 1 planlamasi) ama hic kullanilmadi -
-- goruntulemede kullanilacak onek + iptal alani eksikti.
ALTER TABLE webhook.api_anahtari
    ADD COLUMN anahtar_onek TEXT NOT NULL DEFAULT '',
    ADD COLUMN iptal_edilme TIMESTAMPTZ;
ALTER TABLE webhook.api_anahtari ALTER COLUMN anahtar_onek DROP DEFAULT;
CREATE UNIQUE INDEX idx_api_anahtari_hash ON webhook.api_anahtari (anahtar_hash);

-- Plan-bazli kademeli kota yerine dogrudan ayarlanabilir bir sayi tercih edildi (RetryProfili'nin
-- jsonb yerine sabit profil olmasiyla ayni gerekce: basitlik, henuz gercek bir faturalama yok).
ALTER TABLE webhook.organizasyon ADD COLUMN aylik_kota INT NOT NULL DEFAULT 10000;

CREATE TABLE webhook.kullanim_sayaci (
    organizasyon_id  UUID NOT NULL REFERENCES webhook.organizasyon (id),
    gun              DATE NOT NULL,
    teslimat_sayisi  INT NOT NULL DEFAULT 0,
    basarili         INT NOT NULL DEFAULT 0,
    basarisiz        INT NOT NULL DEFAULT 0,
    PRIMARY KEY (organizasyon_id, gun)
);

-- Secret rotasyonu: ayri bir endpoint_secret tablosu yerine (plandaki oneri) dogrudan
-- endpoint uzerinde iki kolon - mevcut secret HER ZAMAN aktif secret ile imzalar (biz gonderen
-- taraf oldugumuz icin gecikmeli kesime gerek yok), eski secret sadece "hala imza dogrulamada
-- kabul edilir mi" sorusuna cevap vermek icin 24 saatlik bir pencerede saklanir (bkz
-- EndpointController.imzaDogrula). Hiz siniri icin de motorun kendi planlananZaman
-- zamanlamasi yeniden kullanildi (bkz gorev-motoru GorevOpsiyonlari) - ayri bir kuyruklama
-- mekanizmasi eklenmedi.
ALTER TABLE webhook.endpoint
    ADD COLUMN eski_imza_secret TEXT,
    ADD COLUMN eski_secret_gecerlilik_bitis TIMESTAMPTZ,
    ADD COLUMN hiz_siniri_sn INT,
    ADD COLUMN son_planlanan_zaman TIMESTAMPTZ;

-- audit_kaydi/olay/teslimat'a organizasyon_id denormalize edildi - bu kod tabaninda hicbir
-- yerde gercek JPA iliskisi (@ManyToOne) yok, hepsi ham UUID kolonlari - Hibernate @Filter
-- kullanmak (plandaki oneri) bu yuzden orantisiz karmasik olurdu. Dogrudan kolon + her
-- controller'da acik organizasyon kontrolu tercih edildi (bkz Faz 4 planlama notu).
ALTER TABLE webhook.audit_kaydi ADD COLUMN organizasyon_id UUID REFERENCES webhook.organizasyon (id);
UPDATE webhook.audit_kaydi SET organizasyon_id = (SELECT id FROM webhook.organizasyon ORDER BY olusturulma LIMIT 1)
    WHERE organizasyon_id IS NULL;
ALTER TABLE webhook.audit_kaydi ALTER COLUMN organizasyon_id SET NOT NULL;

ALTER TABLE webhook.olay ADD COLUMN organizasyon_id UUID REFERENCES webhook.organizasyon (id);
UPDATE webhook.olay o SET organizasyon_id = u.organizasyon_id
    FROM webhook.uygulama u WHERE o.uygulama_id = u.id AND o.organizasyon_id IS NULL;
ALTER TABLE webhook.olay ALTER COLUMN organizasyon_id SET NOT NULL;
CREATE INDEX idx_olay_org ON webhook.olay (organizasyon_id);

ALTER TABLE webhook.teslimat ADD COLUMN organizasyon_id UUID REFERENCES webhook.organizasyon (id);
UPDATE webhook.teslimat t SET organizasyon_id = o.organizasyon_id
    FROM webhook.olay o WHERE t.olay_id = o.id AND t.organizasyon_id IS NULL;
ALTER TABLE webhook.teslimat ALTER COLUMN organizasyon_id SET NOT NULL;
CREATE INDEX idx_teslimat_org ON webhook.teslimat (organizasyon_id);
