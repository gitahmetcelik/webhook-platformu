import type { Tur } from "./tipler";

// VARSAYIM: §9.7'deki `audit-filtre` anchor'ı (kayıt tipi filtresi) atlandı — /audit
// sayfasında henüz bir filtre kontrolü yok (backend'in `GET /v1/audit` ucu da filtre
// parametresi almıyor). Bu turun 3 değil 2 adımı var; filtre eklendiğinde ilk adım geri konur.
export const auditIziTuru: Tur = {
  kimlik: "audit-izi",
  baslik: "Denetim izi",
  aciklama: "Kim, ne zaman, hangi kaynakta.",
  adimlar: [
    {
      kimlik: "audit.ne-yaziyor",
      rota: "/audit",
      anchor: "audit-satir",
      yerlesim: "bottom",
      baslik: "Ne yazıyor",
      metin: "Kim, ne zaman, hangi kaynakta. Sonradan denetlenebilirlik bunun içindir.",
      ilerleme: "ileri-butonu",
      etkilesim: "engelli",
    },
    {
      kimlik: "audit.saglik-uyarilari",
      rota: "/audit",
      anchor: "audit-satir",
      yerlesim: "bottom",
      baslik: "Sağlık uyarıları",
      metin:
        "SAGLIK_SKORU_DUSTU / DUZELDI yalnız geçiş anlarında yazılır — aynı uyarı tekrar tekrar birikmez.",
      ilerleme: "ileri-butonu",
      etkilesim: "engelli",
    },
  ],
};
