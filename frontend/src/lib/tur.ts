const ESKI_ANAHTAR = "webhook-platformu-tur-tamamlandi";
const ANAHTAR = (organizasyonId: string) => `webhook-platformu:tur:${organizasyonId}`;

export type TurDurumBilgisi = { durum: "tamamlandi" | "birakildi" | "devam"; adimIndeksi: number };
type TurDepo = Record<string, TurDurumBilgisi>;

function depoyuOku(organizasyonId: string): TurDepo {
  if (typeof window === "undefined") return {};
  try {
    const ham = window.localStorage.getItem(ANAHTAR(organizasyonId));
    return ham ? (JSON.parse(ham) as TurDepo) : {};
  } catch {
    return {};
  }
}

function depoyaYaz(organizasyonId: string, depo: TurDepo) {
  window.localStorage.setItem(ANAHTAR(organizasyonId), JSON.stringify(depo));
}

/** Eski tek-bayrak formatından geriye uyum: `tanitim` turu görülmüş sayılır (§10.5 MUST). */
function eskiBayrakGoruldu(): boolean {
  if (typeof window === "undefined") return false;
  return window.localStorage.getItem(ESKI_ANAHTAR) === "1";
}

export function turDurumu(organizasyonId: string, turKimligi: string): TurDurumBilgisi | undefined {
  const depo = depoyuOku(organizasyonId);
  if (depo[turKimligi]) return depo[turKimligi];
  if (turKimligi === "tanitim" && eskiBayrakGoruldu()) {
    return { durum: "tamamlandi", adimIndeksi: 0 };
  }
  return undefined;
}

export function turDurumunuKaydet(organizasyonId: string, turKimligi: string, bilgi: TurDurumBilgisi) {
  const depo = depoyuOku(organizasyonId);
  depo[turKimligi] = bilgi;
  depoyaYaz(organizasyonId, depo);
}

export function turTamamlandiMi(organizasyonId: string, turKimligi: string): boolean {
  return turDurumu(organizasyonId, turKimligi)?.durum === "tamamlandi";
}

export function turuSifirla(organizasyonId: string, turKimligi: string) {
  const depo = depoyuOku(organizasyonId);
  delete depo[turKimligi];
  depoyaYaz(organizasyonId, depo);
  if (turKimligi === "tanitim") {
    window.localStorage.removeItem(ESKI_ANAHTAR);
  }
}
