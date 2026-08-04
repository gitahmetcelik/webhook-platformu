import { tanitimTuru } from "./tanitim";
import { endpointYonetimiTuru } from "./endpoint-yonetimi";
import { teslimatHataAyiklaTuru } from "./teslimat-hata-ayikla";
import { secretRotasyonuTuru } from "./secret-rotasyonu";
import { testAraciTuru } from "./test-araci";
import { kullanimVeKotaTuru } from "./kullanim-ve-kota";
import { auditIziTuru } from "./audit-izi";
import type { Tur } from "./tipler";

export const TUM_TURLAR: Tur[] = [
  tanitimTuru,
  endpointYonetimiTuru,
  teslimatHataAyiklaTuru,
  secretRotasyonuTuru,
  testAraciTuru,
  kullanimVeKotaTuru,
  auditIziTuru,
];

export function turBul(kimlik: string): Tur | undefined {
  return TUM_TURLAR.find((t) => t.kimlik === kimlik);
}

export * from "./tipler";
