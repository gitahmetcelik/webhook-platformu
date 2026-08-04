/**
 * Abone endpoint'lerinin döndürdüğü yanıt gövdeleri ve payload'lar üçüncü taraf içeriğidir
 * (§13.D). Panelde ayrıştırılmadan önce boyut/derinlik sınırlanır, `__proto__`/`constructor`/
 * `prototype` anahtarları reddedilir (prototype pollution).
 */
const AZAMI_BOYUT_BYTE = 256 * 1024;
const AZAMI_DERINLIK = 64;
const YASAKLI_ANAHTARLAR = new Set(["__proto__", "constructor", "prototype"]);

export type GuvenliAyristirmaSonucu =
  | { durum: "basarili"; deger: unknown }
  | { durum: "cok-buyuk"; boyutByte: number }
  | { durum: "cok-derin" }
  | { durum: "yasakli-anahtar"; anahtar: string }
  | { durum: "gecersiz-json" };

function derinlikVeAnahtarDenetle(deger: unknown, derinlik: number): string | null {
  if (derinlik > AZAMI_DERINLIK) return "cok-derin";
  if (deger === null || typeof deger !== "object") return null;

  if (Array.isArray(deger)) {
    for (const eleman of deger) {
      const hata = derinlikVeAnahtarDenetle(eleman, derinlik + 1);
      if (hata) return hata;
    }
    return null;
  }

  for (const anahtar of Object.keys(deger as Record<string, unknown>)) {
    if (YASAKLI_ANAHTARLAR.has(anahtar)) return `yasakli:${anahtar}`;
    const hata = derinlikVeAnahtarDenetle((deger as Record<string, unknown>)[anahtar], derinlik + 1);
    if (hata) return hata;
  }
  return null;
}

export function guvenliAyristir(ham: string): GuvenliAyristirmaSonucu {
  const boyutByte = new TextEncoder().encode(ham).length;
  if (boyutByte > AZAMI_BOYUT_BYTE) {
    return { durum: "cok-buyuk", boyutByte };
  }

  let deger: unknown;
  try {
    deger = JSON.parse(ham);
  } catch {
    return { durum: "gecersiz-json" };
  }

  const hata = derinlikVeAnahtarDenetle(deger, 0);
  if (hata === "cok-derin") return { durum: "cok-derin" };
  if (hata?.startsWith("yasakli:")) return { durum: "yasakli-anahtar", anahtar: hata.slice(8) };

  return { durum: "basarili", deger };
}

export { AZAMI_BOYUT_BYTE, AZAMI_DERINLIK };
