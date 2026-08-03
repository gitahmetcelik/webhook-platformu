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

## Geliştirme ortamı

Önce `gorev-motoru`'nu local `.m2`'ye kurun (bir kere yeterli):
```bash
cd ../gorev-motoru && mvn clean install -DskipTests
```

Sonra:
```bash
cp .env.example .env
docker compose up -d postgres rabbitmq test-alici
# postgres: localhost:5433, rabbitmq: localhost:5673 (15673 yönetim arayüzü) —
# gorev-motoru'nun kendi geliştirme container'larıyla (5432/5672) çakışmasın diye farklı port.

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=api     # 8080
mvn spring-boot:run -Dspring-boot.run.profiles=worker   # 8081, ayrı terminalde
```

### Neden `backend` Docker'da değil

`motor-spring-starter:0.1.0-SNAPSHOT` henüz hiçbir uzak Maven repository'sinde publish
edilmedi (bilinçli, `gorev-motoru` Faz 0.6 kararı) — sadece geliştirenin local `.m2`'sinde.
Bir Docker build container'ı buna erişemiyor, bu yüzden `backend-api`/`backend-worker`
şimdilik `docker-compose.yml`'de yok; host üzerinde `mvn spring-boot:run` ile çalıştırılıyor
(tıpkı `gorev-motoru`'nun kendi geliştirme döngüsünde olduğu gibi). GitHub Packages'a publish
edilince (ileri bir faz) tam containerization'a geçilebilir — `backend/Dockerfile` bu geçişe
hazır olarak duruyor.

## Durum

Faz 1 (headless teslimat çekirdeği) üzerinde çalışılıyor. Detaylı plan ve faz kapı testleri
için proje sahibinin kendi planlama notlarına bakınız.
