import type { Tur } from "./tipler";

export const testAraciTuru: Tur = {
  kimlik: "test-araci",
  baslik: "Test aracı",
  aciklama: "Gerçek bir olay göndermenin en hızlı yolu.",
  adimlar: [
    {
      kimlik: "test-araci.senaryo",
      rota: "/test",
      anchor: "test-senaryo-secimi",
      yerlesim: "bottom",
      baslik: "Hazır senaryolar",
      metin: "siparis.olusturuldu, odeme.basarili, iade.talep-edildi — gerçekçi payload'larla gelir.",
      ilerleme: "ileri-butonu",
      etkilesim: "serbest",
    },
    {
      kimlik: "test-araci.payload",
      rota: "/test",
      anchor: "test-payload-editoru",
      yerlesim: "top",
      baslik: "Payload'ı değiştirin",
      metin: "Endpoint filtrenizin gerçekten eşleştiğini görmek için alanları serbestçe düzenleyin.",
      ilerleme: "ileri-butonu",
      etkilesim: "serbest",
    },
    {
      kimlik: "test-araci.gonder",
      rota: "/test",
      anchor: "test-gonder-buton",
      yerlesim: "top",
      baslik: "Gönderin",
      metin: "Gerçek bir giriş isteği atılır ve oluşan teslimatın timeline'ına yönlendirilirsiniz.",
      ilerleme: "ileri-butonu",
      etkilesim: "serbest",
    },
  ],
};
