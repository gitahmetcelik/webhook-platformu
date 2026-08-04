import type { TurAnchorId } from "@/lib/tur-anchorlari";

// VARSAYIM: §10.2'deki tam sözleşmeden `hazirlik` (async dialog/sekme açma), `sorgu` ve
// `ilerleme: {tip:'olay'}` bilinçli olarak çıkarıldı — bu revizyondaki 7 tur bunlara ihtiyaç
// duymuyor (dialog açan tek adım endpoint-yonetimi#4, onu `onkosul` ile ele alıyoruz). Gerçek
// bir dialog-açma adımı gerektiğinde `hazirlik?: (b: TurBaglami) => Promise<void>` eklenir.

export type TurBaglami = {
  /** Aktif organizasyonun/uygulamanın turun önkoşullarını değerlendirmesi için basit erişim. */
  endpointSayisi: number;
  olaySayisi: number;
};

export type TurAdimi = {
  kimlik: string;
  /** `null` = zorla yönlendirme yapma; önceki adımın anchor-tikla aksiyonu uygulamayı zaten
   * doğru (genelde dinamik, ör. /teslimatlar/[id]) sayfaya taşıdı, motor sadece anchor'ı bekler. */
  rota: string | null;
  anchor: TurAnchorId;
  yerlesim?: "top" | "bottom" | "left" | "right";
  baslik: string;
  /** DÜZ METİN — HTML değil (§13.J). */
  metin: string;

  /** Sağlanmazsa adım her zaman gösterilir. */
  onkosul?: (b: TurBaglami) => boolean;

  ilerleme?:
    | "ileri-butonu"
    | { tip: "anchor-tikla" }
    | { tip: "rota"; rota: string };

  spotlight?: "eleman" | "eleman-bosluklu" | "yok";
  etkilesim?: "engelli" | "sadece-anchor" | "serbest";
};

export type Tur = {
  kimlik: string;
  baslik: string;
  aciklama: string;
  adimlar: TurAdimi[];
};
