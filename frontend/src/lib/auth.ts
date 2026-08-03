const ANAHTAR_KEY = "webhook_api_anahtari";

export function apiAnahtariniOku(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ANAHTAR_KEY);
}

export function apiAnahtariniKaydet(anahtar: string) {
  window.localStorage.setItem(ANAHTAR_KEY, anahtar);
}

export function apiAnahtariniTemizle() {
  window.localStorage.removeItem(ANAHTAR_KEY);
}
