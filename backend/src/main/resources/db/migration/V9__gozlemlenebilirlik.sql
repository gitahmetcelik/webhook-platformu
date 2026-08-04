-- Faz 5.2: trace id'yi olay -> teslimat zincirinde tasi.
--
-- Motor gorev basina bir trace_id zaten tutuyor (bkz gorev tablosu / GorevOzeti.traceId) ama
-- bu URUN tarafi icin iki noktada yetersiz kaliyordu:
--   1. Devre acikken olusan teslimatlarin gorev_id'si NULL (motora hic gonderilmiyor), yani
--      motordan okunacak bir trace de yok.
--   2. Bir olaydan dogan N teslimat motorda N AYRI gorev - "bu event'e ne oldu" sorusunu tek
--      bir id ile cevaplamak icin olayin kendi trace'i gerekiyor.
-- Bu yuzden trace id giris istegi sirasinda bir kez uretilip hem olaya hem ondan dogan tum
-- teslimatlara yaziliyor (ayni deger) - yeniden gonderim yeni bir istek oldugu icin yeni bir
-- trace alir, bu bilincli.
ALTER TABLE webhook.olay ADD COLUMN trace_id TEXT;
ALTER TABLE webhook.teslimat ADD COLUMN trace_id TEXT;

-- Teslimat detay/timeline sayfasi trace id ile arama yapabilsin diye.
CREATE INDEX idx_teslimat_trace ON webhook.teslimat (trace_id);
CREATE INDEX idx_olay_trace ON webhook.olay (trace_id);
