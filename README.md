# Webhook Platformu

Müşteri sistemlerinin gönderdiği event'leri, abonelerin endpoint'lerine güvenilir şekilde
teslim eden bir webhook teslimat platformu (Svix/Hookdeck/Convoy tarzı).

Ürünün değeri teslimatın kendisi değil, **teslimatın başarısız olduğu anda ne olduğudur**:
imzalama (HMAC), yapılandırılabilir retry merdiveni, ölü mektup kutusu, devre kesici, tek
tıkla yeniden gönderim ve her denemenin görülebildiği bir timeline.

Yürütme motoru olarak [`gorev-motoru`](https://github.com/gitahmetcelik/gorev-motoru)
(`motor-spring-starter`) kullanılır — retry+backoff, DLQ, idempotency, öncelik kuyrukları
buradan gelir; bu repo motoru bir Maven bağımlılığı olarak tüketir, motor koduna dokunmaz.

## Repo düzeni

```
/backend        Maven, Spring Boot (Java 21) — teslimat API'si + worker
/frontend       Next.js — dashboard
/test-alici     Kontrol edilebilir webhook alıcısı (kapı testleri için)
docker-compose.yml
```

## Durum

Faz 1 (headless teslimat çekirdeği) üzerinde çalışılıyor. Detaylı plan ve faz kapı testleri
için proje sahibinin kendi planlama notlarına bakınız.
