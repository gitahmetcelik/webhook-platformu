-- Faz 5.3: endpoint saglik skoru esik altina DUSTUGU AN bir kez uyari uretmek icin.
--
-- Skorun kendisi saklanmiyor (her istekte son 24 saatten canli hesaplaniyor, bkz
-- EndpointSaglikHesaplayici) - saklanan sey sadece "su an uyari durumunda miyiz" bayragi.
-- Bu bayrak olmadan periyodik kontrol her calistiginda ayni uyariyi tekrar tekrar yazardi;
-- bayrak sayesinde sadece GECIS anlarinda (saglikli -> bozuk, bozuk -> saglikli) audit
-- kaydi olusuyor.
ALTER TABLE webhook.endpoint ADD COLUMN saglik_uyarisi_aktif BOOLEAN NOT NULL DEFAULT FALSE;
