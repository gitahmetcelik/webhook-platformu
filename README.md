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

`motor-spring-starter` GitHub Packages'tan çekilir (bkz `gorev-motoru` Faz 5.0) — local
`.m2`'ye elle kurmaya gerek yok, ama Maven'in GitHub Packages'a erişebilmesi için bir kereye
mahsus kimlik doğrulama gerekir:

1. https://github.com/settings/tokens/new → **Classic token**, scope: `read:packages`
   (+ `repo`, `gorev-motoru` private olduğu için gerekebilir).
2. `~/.m2/settings.xml` (yoksa oluştur):
   ```xml
   <settings>
     <servers>
       <server>
         <id>github-gorev-motoru</id>
         <username>KENDI_GITHUB_KULLANICI_ADIN</username>
         <password>ghp_...</password>
       </server>
     </servers>
   </settings>
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

## Arayüz (dashboard) ne işe yarar, ne test eder

Dashboard bu üründe bir demo aracı değil, ürünün kendisidir — Faz 2'de curl ile yapılan her
şeyin arayüzden yapılabilmesi hedeflendi (bkz Faz 3 planı). Her sayfa, motorun/backend'in
belirli bir dayanıklılık davranışını gerçekten tetikleyip gözlemlemek için var:

- **`/giris`** — API anahtarını yapıştırıp organizasyon kimliğini doğrular
  (`GET /v1/organizasyon/ben`), `localStorage`'a kaydeder. Bundan sonraki her istek bu
  anahtarla imzalanır; anahtar yoksa veya geçersizse otomatik buraya yönlendirilir. **Test
  ettiği şey:** API-anahtarlı kimlik doğrulama + kiracı (organizasyon) izolasyonunun
  çalıştığı — başka bir organizasyonun anahtarıyla giriş yapılınca sadece o organizasyonun
  verisi görünür.

- **`/olaylar`** — Bir uygulamaya ait tüm gelen event'lerin (webhook girişlerinin) 5
  saniyede bir kendini yenileyen listesi, her satırda o event'in tetiklediği teslimatların
  özeti (`n/m başarılı`). **Test ettiği şey:** giriş API'sinin (`POST .../olaylar`) event'i
  doğru kaydettiği, filtreye uyan endpoint'ler için doğru sayıda teslimat ürettiği.

- **`/teslimatlar/{id}`** — Ürünün vitrini: tek bir teslimatın durum rozetini, motor
  tarafındaki gerçek durumunu (`GorevYonetimServisi.ozet()` ile), ve **deneme timeline'ını**
  gösterir — her deneme için zaman, HTTP durum kodu, süre, bir sonraki denemeye kalan
  backoff. Payload/başlık/yanıt gövdesi incelenebilir, "curl olarak kopyala" ile tekrar
  denenebilir. **Devre açıksa** veya **DLQ'ya düştüyse** buradan tek tıkla **Yeniden Gönder**
  edilebilir. **Test ettiği şey:** motorun retry+backoff'unun, DLQ'ya düşüşün ve manuel
  yeniden-gönderim akışının (yeni teslimat satırı üretip eski teslimata bağlaması dahil)
  gerçekten çalıştığı.

- **`/endpointler`** — Bir uygulamaya bağlı tüm endpoint'lerin listesi: URL, event filtresi,
  retry profili, son 24 saatlik başarı oranı, devre kesici durumu (Sağlıklı/Yarı Açık/Açık).
  Buradan endpoint oluşturulur (secret bir kez gösterilir), düzenlenir, hız sınırı
  ayarlanır, secret rotasyonu yapılır, devre elle sıfırlanır. **Test ettiği şey:** devre
  kesicinin (ardışık kalıcı hatadan sonra devrenin açılması, sağlık sondasının kapatması),
  endpoint bazlı rate limiting'in (motorun `planlananZaman` zamanlamasıyla teslimatları
  yaymasının) ve secret rotasyonunun (eski secret'ın grace penceresinde hâlâ geçerli
  kalmasının) gerçekten işlediği.

- **`/test`** — Hazır senaryolardan (`siparis.olusturuldu`, `odeme.basarili`,
  `iade.talep-edildi`) bir event tipi seçip payload'ı düzenleyerek gerçek bir webhook
  girişini tetikler, oluşan teslimatın timeline'ına yönlendirir. **Test ettiği şey:** uçtan
  uca akışın (giriş → endpoint eşleştirme → motora gönderim → gerçek HTTP POST →
  `test-alici`'ye ulaşma) tamamının, terminale hiç dokunmadan arayüzden tetiklenebildiği.

- **`/kullanim`** — Bu ayki teslimat kullanımını aylık kotaya karşı bir çubukla gösterir,
  günlük başarılı/başarısız dökümü listeler, API anahtarı üretir/iptal eder (üretilen
  anahtar bir kez gösterilir). **Test ettiği şey:** kullanım sayacının doğru arttığı, kota
  aşımının event kabulünü gerçekten `429` ile reddettiği, API anahtarı yaşam döngüsünün
  (üret/iptal) çalıştığı.

- **`/audit`** — Organizasyona ait tüm denetim kayıtlarının (devre sıfırlama, secret
  rotasyonu, API anahtarı üretme/iptal) sayfalı listesi. **Test ettiği şey:** hassas/
  operasyonel her aksiyonun gerçekten iz bıraktığı — kimin ne zaman ne yaptığının sonradan
  denetlenebildiği.

Ortak payda: her sayfa arkasındaki API çağrısı `Authorization: Bearer <api-anahtarı>` ile
gider ve backend'de organizasyon sınırını aşan hiçbir okuma/yazma **404** (varlık
sızdırmadan) döner — bu, dashboard'un kendisinin de kiracı izolasyonunu ihlal edemeyeceği
anlamına gelir.

## Durum

Faz 4 (ticari katman) tamamlandı ve `master`'a merge edildi. Detaylı plan ve faz kapı
testleri için proje sahibinin kendi planlama notlarına bakınız.
