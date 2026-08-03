# Webhook imza doğrulama

Her webhook isteğinde üç başlık gönderilir:

- `X-Webhook-Id`: bu teslimatın benzersiz kimliği (`msg_...`)
- `X-Webhook-Timestamp`: gönderim anı, Unix epoch saniye
- `X-Webhook-Signature`: `v1,<base64 HMAC-SHA256>`

İmzalanan içerik tam olarak şu formattadır (üç parça `.` ile birleştirilir):

```
{X-Webhook-Id}.{X-Webhook-Timestamp}.{ham istek gövdesi}
```

Secret, endpoint oluşturulurken bir kez gösterilir; rotasyon sonrası eski secret
**24 saat** boyunca da geçerli kabul edilir (bkz Faz 4.1) — iki tarafın da aynı anda
kesmesi gerekmez.

**Replay koruması / zaman toleransı:** `X-Webhook-Timestamp`'i kendi saatinizle
karşılaştırıp **5 dakikadan** eski istekleri reddetmeniz önerilir.

## curl (manuel doğrulama, hata ayıklama için)

```bash
computed=$(printf '%s.%s.%s' "$WEBHOOK_ID" "$TIMESTAMP" "$BODY" \
  | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -binary | base64)
[ "v1,$computed" = "$X_WEBHOOK_SIGNATURE" ] && echo "gecerli" || echo "gecersiz"
```

## Node.js

```javascript
const crypto = require("crypto");

function dogrula(secret, webhookId, timestamp, rawBody, signatureHeader) {
  const signed = `${webhookId}.${timestamp}.${rawBody}`;
  const expected = "v1," + crypto.createHmac("sha256", secret).update(signed).digest("base64");
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signatureHeader));
}
```

## Python

```python
import hmac, hashlib, base64

def dogrula(secret: str, webhook_id: str, timestamp: str, raw_body: str, signature_header: str) -> bool:
    signed = f"{webhook_id}.{timestamp}.{raw_body}".encode()
    digest = hmac.new(secret.encode(), signed, hashlib.sha256).digest()
    expected = "v1," + base64.b64encode(digest).decode()
    return hmac.compare_digest(expected, signature_header)
```

## Java

```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
String signed = webhookId + "." + timestamp + "." + rawBody;
String expected = "v1," + Base64.getEncoder().encodeToString(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
boolean gecerli = MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
        signatureHeader.getBytes(StandardCharsets.UTF_8));
```

Tüm örneklerde sabit-zamanlı karşılaştırma kullanılır (`timingSafeEqual` /
`compare_digest` / `MessageDigest.isEqual`) — normal `==`/`.equals()` zamanlama
yan-kanalıyla secret sızıntısına açıktır.
