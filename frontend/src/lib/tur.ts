const ANAHTAR = "webhook-platformu-tur-tamamlandi";

export function turTamamlandiMi(): boolean {
  if (typeof window === "undefined") return true;
  return window.localStorage.getItem(ANAHTAR) === "1";
}

export function turuTamamla() {
  window.localStorage.setItem(ANAHTAR, "1");
}

export function turuSifirla() {
  window.localStorage.removeItem(ANAHTAR);
}
