/**
 * §9'daki tüm data-tur anchor kimlikleri. Tur adımları CSS seçicisiyle değil yalnız bu
 * birleşim tipinden alınan değerlerle hedeflenir — yanlış yazım derleme zamanında yakalanır.
 * `scripts/tur-anchor-denetimi.ts` bu listeyi kod tabanındaki gerçek `data-tur` kullanımıyla
 * karşılaştırır.
 */
export type TurAnchorId =
  // §9.1 genel çerçeve — nav-<href> üst menü bağlantıları (dinamik, mevcut) arasından turlarda
  // kullanılanlar burada ayrıca literal olarak listelenir
  | "nav-/olaylar"
  | "menu-tema-anahtari"
  | "menu-yardim"
  | "menu-organizasyon"
  | "kontrol-listesi"
  // §9.2 ana sayfa /
  | "anasayfa-istatistikler"
  | "anasayfa-test-buton"
  | "anasayfa-kota-karti"
  | "anasayfa-devre-karti"
  | "anasayfa-son-teslimatlar"
  // §9.3 olaylar /olaylar
  | "olaylar-filtre"
  | "olaylar-tarih-araligi"
  | "olaylar-satir"
  | "olaylar-teslimat-ozeti"
  | "olaylar-yardim"
  // §9.4 endpoint'ler /endpointler
  | "endpoint-yeni-buton"
  | "endpoint-satir"
  | "endpoint-saglik-skoru"
  | "endpoint-devre-rozeti"
  | "endpoint-devre-sifirla"
  | "endpoint-secret-rotasyon"
  | "endpoint-duzenle"
  | "endpoint-yardim"
  | "endpoint-form-url"
  | "endpoint-form-filtre"
  | "endpoint-form-retry-profili"
  | "endpoint-form-hiz-siniri"
  | "endpoint-form-kaydet"
  | "endpoint-secret-goster"
  // §9.5 teslimat detayı /teslimatlar/[id]
  | "teslimat-durum-rozeti"
  | "teslimat-motor-ozeti"
  | "teslimat-trace-id"
  | "teslimat-timeline"
  | "teslimat-deneme-satiri"
  | "teslimat-backoff-bilgisi"
  | "teslimat-payload"
  | "teslimat-yanit-govdesi"
  | "teslimat-curl-kopyala"
  | "teslimat-yeniden-gonder"
  | "teslimat-ana-teslimat"
  // §9.6 test aracı /test
  | "test-gonder-buton"
  | "test-senaryo-secimi"
  | "test-payload-editoru"
  // §9.7 kullanım /kullanim ve audit /audit
  | "kullanim-anahtar-buton"
  | "kullanim-kota-cubugu"
  | "kullanim-gunluk-dokum"
  | "kullanim-anahtar-iptal"
  | "audit-baslik"
  | "audit-filtre"
  | "audit-satir";
