import type { Tur } from "./tipler";

export const kullanimVeKotaTuru: Tur = {
  kimlik: "kullanim-ve-kota",
  baslik: "Kullanım ve kota",
  aciklama: "Aylık kota ve API anahtarı yönetimi.",
  adimlar: [
    {
      kimlik: "kullanim.kota",
      rota: "/kullanim",
      anchor: "kullanim-kota-cubugu",
      yerlesim: "bottom",
      baslik: "Aylık kota",
      metin: "Çubuk dolduğunda yeni olay girişleri 429 ile reddedilir.",
      ilerleme: "ileri-butonu",
      etkilesim: "engelli",
    },
    {
      kimlik: "kullanim.gunluk-dokum",
      rota: "/kullanim",
      anchor: "kullanim-gunluk-dokum",
      yerlesim: "top",
      baslik: "Günlük döküm",
      metin: "Başarılı/başarısız ayrımı — bir günde sıçrama varsa o günün teslimatlarına bakın.",
      ilerleme: "ileri-butonu",
      etkilesim: "engelli",
    },
    {
      kimlik: "kullanim.anahtar-uret",
      rota: "/kullanim",
      anchor: "kullanim-anahtar-buton",
      yerlesim: "bottom",
      baslik: "API anahtarı üretin",
      metin: "Anahtar bir kez gösterilir; kaybederseniz yenisini üretip eskisini iptal edin.",
      ilerleme: "ileri-butonu",
      etkilesim: "engelli",
    },
    {
      kimlik: "kullanim.anahtar-iptal",
      rota: "/kullanim",
      anchor: "kullanim-anahtar-iptal",
      yerlesim: "bottom",
      baslik: "İptal",
      metin: "İptal anında etkilidir. O anahtarla giden istekler hemen reddedilir.",
      ilerleme: "ileri-butonu",
      etkilesim: "engelli",
    },
  ],
};
