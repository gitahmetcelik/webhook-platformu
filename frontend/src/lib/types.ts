export type TeslimatDurumu = "KUYRUKTA" | "BASARILI" | "HATALI" | "DLQ" | "KALICI_HATA" | "BEKLEMEDE";
export type DevreDurumu = "KAPALI" | "YARI_ACIK" | "ACIK";
export type RetryProfili = "HIZLI" | "STANDART" | "UZUN";

export interface Uygulama {
  id: string;
  organizasyonId: string;
  ad: string;
  ortam: string;
  olusturulma: string;
}

export interface TeslimatOzeti {
  id: string;
  olayId: string;
  endpointId: string;
  durum: TeslimatDurumu;
  anaTeslimatId: string | null;
  olusturulma: string;
  guncellenme: string;
}

export interface OlayOzeti {
  id: string;
  uygulamaId: string;
  tip: string;
  disKaynakId: string;
  olusturulma: string;
  teslimatlar: TeslimatOzeti[];
}

export interface TeslimatDenemesi {
  denemeNo: number;
  istekZamani: string;
  sureMs: number | null;
  httpDurum: number | null;
  yanitGovdesi: string | null;
  hata: string | null;
}

export interface MotorGorevOzeti {
  durum: string;
  denemeSayisi: number;
  sonHata: string | null;
  traceId: string | null;
}

export interface TeslimatDetay {
  teslimat: TeslimatOzeti;
  olayTipi: string;
  olayPayload: string;
  endpointUrl: string;
  denemeler: TeslimatDenemesi[];
  motorGorevOzeti: MotorGorevOzeti | null;
}

export interface Endpoint {
  id: string;
  uygulamaId: string;
  url: string;
  olayFiltresi: string[];
  devreDurumu: DevreDurumu;
  retryProfili: RetryProfili;
  ardisikHataSayisi: number;
  basariOraniSon24Saat: number | null;
  olusturulma: string;
}

export interface EndpointOlusturmaYaniti {
  id: string;
  url: string;
  secret: string;
}

export interface Sayfa<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
