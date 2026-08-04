# Fly.io'ya dağıtım

> **Durum: yazıldı ama canlıda doğrulanmadı.** Bu repodaki her şey gerçekten çalıştırılarak
> test edilir; bu dosya istisna — Fly hesabı gerektirdiği için adımlar canlı ortamda
> koşulmadı. Aynı imajlar `docker-compose.prod.yml` ile yerelde uçtan uca doğrulandı.
> Takıldığınız yeri bildirin, düzeltelim.

## Mimari

Fly'da dört kaynak:

| Kaynak | Ne | Neden ayrı |
|---|---|---|
| `webhook-platformu-backend` | `api` + `worker` process grupları | **Tek imaj**, iki process — giriş trafiği ile teslimat yükü ayrı makinelerde ölçeklenir |
| `webhook-platformu-rabbitmq` | RabbitMQ + delayed-message plugin | Fly'ın yönetilen RabbitMQ'su yok; motorun gecikmeli retry'ı bu plugin'e bağlı |
| `webhook-platformu-dashboard` | Next.js | Statik/SSR yükü backend'den bağımsız |
| Fly Postgres | Veritabanı | Yönetilen |

Servisler birbirine Fly özel ağı (`<app>.internal`) üzerinden konuşur; yalnızca backend'in
`api` grubu ve dashboard internete açıktır.

## Ön koşullar

```bash
fly auth login
```
`~/.m2/settings.xml` dosyanız GitHub Packages için yapılandırılmış olmalı (bkz ana README).

## 1. RabbitMQ

```bash
fly apps create webhook-platformu-rabbitmq
fly volumes create rabbitmq_data --size 1 -a webhook-platformu-rabbitmq -r fra
fly secrets set RABBITMQ_DEFAULT_USER=webhook RABBITMQ_DEFAULT_PASS="$(openssl rand -hex 24)" \
  -a webhook-platformu-rabbitmq
fly deploy -c dagitim/fly/fly.rabbitmq.toml
```

Yönetim arayüzüne bakmak isterseniz (dışarı açık değil):
`fly proxy 15672:15672 -a webhook-platformu-rabbitmq`

## 2. Postgres

```bash
fly postgres create --name webhook-platformu-db --region fra
```

Fly `attach` komutu `DATABASE_URL`'i `postgres://...` biçiminde verir; Spring ise
`jdbc:postgresql://...` bekler. Bu yüzden bağlantıyı **elle** set edin — `fly postgres create`
çıktısındaki kullanıcı/şifreyi kullanarak:

```bash
fly secrets set \
  SPRING_DATASOURCE_URL="jdbc:postgresql://webhook-platformu-db.internal:5432/webhook_platformu" \
  SPRING_DATASOURCE_USERNAME="postgres" \
  SPRING_DATASOURCE_PASSWORD="<create ciktisindaki sifre>" \
  -a webhook-platformu-backend
```

> Veritabanı henüz yoksa: `fly postgres connect -a webhook-platformu-db` → `CREATE DATABASE
> webhook_platformu;`. Şema Flyway tarafından ilk açılışta kurulur.

## 3. Backend

```bash
fly apps create webhook-platformu-backend

fly secrets set \
  SPRING_RABBITMQ_USERNAME=webhook \
  SPRING_RABBITMQ_PASSWORD="<1. adimdaki RABBITMQ_DEFAULT_PASS>" \
  WEBHOOK_SIFRELEME_ANAHTARI="$(openssl rand -base64 32)" \
  -a webhook-platformu-backend

fly deploy -c dagitim/fly/fly.backend.toml --local-only
```

`--local-only` bilinçli: imaj yerel Docker'da build edilip Fly registry'sine push edilir,
böylece GitHub Packages token'ınız Fly'ın uzak builder'ına hiç gitmez. Uzak builder
kullanmak isterseniz `--build-secret maven_settings="$(cat ~/.m2/settings.xml)"` ekleyin.

> **`WEBHOOK_SIFRELEME_ANAHTARI` bir kez belirlenir ve değiştirilmez** — endpoint imza
> secret'ları bu anahtarla şifrelenir, değişirse kayıtlı secret'lar çözülemez hale gelir.

Process gruplarını ölçekleyin (varsayılan olarak her gruptan bir makine açılır):
```bash
fly scale count api=1 worker=1 -a webhook-platformu-backend
```

## 4. Dashboard

```bash
fly apps create webhook-platformu-dashboard
fly deploy -c dagitim/fly/fly.dashboard.toml --local-only
```

Backend adresiniz farklıysa hem `fly.dashboard.toml`'daki `build.args.NEXT_PUBLIC_API_URL`'i
hem `fly.backend.toml`'daki `WEBHOOK_IZINLI_ORIGINLER`'i güncelleyin — ikisi birbiriyle
tutarlı olmalı, aksi halde tarayıcı CORS'tan dolayı tüm istekleri bloklar.

## 5. Demo verisi

```bash
fly ssh console -a webhook-platformu-backend -C \
  "java -jar /app/app.jar --spring.profiles.active=api,seed --server.port=8090"
```

Çıktıdaki `TOHUM ... API anahtari` satırındaki anahtarı dashboard'un `/giris` sayfasına
yapıştırın. Seed'in ürettiği endpoint `http://localhost:4000/webhook`'u gösterir; canlıda
gerçek bir alıcı kullanacaksanız `/endpointler` sayfasından URL'i düzenleyin veya deploy
öncesi `WEBHOOK_TOHUM_ENDPOINT_URL` secret'ını set edin.

## Doğrulama

```bash
curl https://webhook-platformu-backend.fly.dev/actuator/health     # {"status":"UP"}
curl -o /dev/null -w "%{http_code}\n" https://webhook-platformu-backend.fly.dev/v1/uygulamalar  # 401
```
Ardından dashboard'u açıp [`DEMO.md`](../../DEMO.md) senaryosunu koşun.

## Bilinen tuzaklar

- **`auto_stop_machines`:** `worker` grubunun durmaması gerekir (HTTP servisi olmadığı için
  Fly onu "boşta" sanabilir). Sorun yaşarsanız `fly scale count worker=1` ile sabitleyin ve
  `min_machines_running`'i kontrol edin.
- **Bölge tutarlılığı:** backend, rabbitmq ve Postgres aynı `primary_region`'da olmalı —
  aksi halde her teslimat kıtalar arası gidip gelir.
- **IPv6:** Fly `.internal` DNS'i IPv6 döndürür. JVM bunu destekler; `preferIPv4Stack`
  ayarını **açmayın**, açarsanız iç ağ çözümlemesi kırılır.
- **Ücret:** Bu kurulum birkaç makine + bir volume + yönetilen Postgres kullanır. Demo
  bittiğinde `fly apps destroy <app>` ile temizleyin.
