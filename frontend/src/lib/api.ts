import { apiAnahtariniOku, apiAnahtariniTemizle } from "./auth";
import type {
  ApiAnahtari,
  ApiAnahtariUretimYaniti,
  AuditKaydi,
  Endpoint,
  EndpointOlusturmaYaniti,
  KullanimGunluk,
  OlayOzeti,
  OrganizasyonBen,
  RetryProfili,
  Sayfa,
  SecretRotasyonYaniti,
  TeslimatDetay,
  TeslimatOzeti,
  Uygulama,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function istek<T>(yol: string, secenekler?: RequestInit): Promise<T> {
  const anahtar = apiAnahtariniOku();
  const yanit = await fetch(`${API_URL}${yol}`, {
    ...secenekler,
    headers: {
      "Content-Type": "application/json",
      ...(anahtar ? { Authorization: `Bearer ${anahtar}` } : {}),
      ...secenekler?.headers,
    },
  });
  if (yanit.status === 401) {
    apiAnahtariniTemizle();
    if (typeof window !== "undefined" && window.location.pathname !== "/giris") {
      window.location.href = "/giris";
    }
    throw new Error("Yetkisiz - API anahtari gecersiz veya eksik");
  }
  if (!yanit.ok) {
    const govde = await yanit.text().catch(() => "");
    throw new Error(`API hatasi (${yanit.status}): ${govde || yanit.statusText}`);
  }
  if (yanit.status === 204) {
    return undefined as T;
  }
  return (await yanit.json()) as T;
}

function sorguDizesi(params: Record<string, string | number | undefined>): string {
  const gecerliler = Object.entries(params).filter(([, deger]) => deger !== undefined && deger !== "");
  if (gecerliler.length === 0) {
    return "";
  }
  return "?" + gecerliler.map(([anahtar, deger]) => `${anahtar}=${encodeURIComponent(String(deger))}`).join("&");
}

export const api = {
  organizasyon: {
    ben: () => istek<OrganizasyonBen>("/v1/organizasyon/ben"),
    apiAnahtarlariListele: () => istek<ApiAnahtari[]>("/v1/organizasyon/api-anahtarlari"),
    apiAnahtariUret: () =>
      istek<ApiAnahtariUretimYaniti>("/v1/organizasyon/api-anahtarlari", { method: "POST" }),
    apiAnahtariIptalEt: (id: string) =>
      istek<void>(`/v1/organizasyon/api-anahtarlari/${id}/iptal`, { method: "POST" }),
  },

  kullanim: {
    listele: () => istek<KullanimGunluk[]>("/v1/kullanim"),
  },

  audit: {
    listele: (params: { sayfa?: number; boyut?: number } = {}) =>
      istek<Sayfa<AuditKaydi>>(`/v1/audit${sorguDizesi({ page: params.sayfa, size: params.boyut })}`),
  },

  uygulamalar: {
    listele: () => istek<Uygulama[]>("/v1/uygulamalar"),
  },

  olaylar: {
    listele: (uygulamaId: string, params: { tip?: string; sayfa?: number; boyut?: number } = {}) =>
      istek<Sayfa<OlayOzeti>>(
        `/v1/uygulamalar/${uygulamaId}/olaylar${sorguDizesi({
          tip: params.tip,
          page: params.sayfa,
          size: params.boyut,
        })}`,
      ),
    olustur: (uygulamaId: string, istegi: { tip: string; payload: unknown }, idempotencyKey: string) =>
      istek<{ olayId: string; teslimatSayisi: number; teslimatIdleri: string[] }>(
        `/v1/uygulamalar/${uygulamaId}/olaylar`,
        {
          method: "POST",
          headers: { "Idempotency-Key": idempotencyKey },
          body: JSON.stringify(istegi),
        },
      ),
  },

  teslimatlar: {
    listele: (params: { sayfa?: number; boyut?: number } = {}) =>
      istek<Sayfa<TeslimatOzeti>>(`/v1/teslimatlar${sorguDizesi({ page: params.sayfa, size: params.boyut })}`),
    detay: (id: string) => istek<TeslimatDetay>(`/v1/teslimatlar/${id}`),
    yenidenGonder: (id: string) => istek<TeslimatOzeti>(`/v1/teslimatlar/${id}/yeniden-gonder`, { method: "POST" }),
  },

  endpointler: {
    listele: (uygulamaId: string) => istek<Endpoint[]>(`/v1/uygulamalar/${uygulamaId}/endpointler`),
    detay: (id: string) => istek<Endpoint>(`/v1/endpointler/${id}`),
    olustur: (
      uygulamaId: string,
      istegi: { url: string; olayFiltresi: string[]; retryProfili: RetryProfili },
    ) =>
      istek<EndpointOlusturmaYaniti>(`/v1/uygulamalar/${uygulamaId}/endpointler`, {
        method: "POST",
        body: JSON.stringify(istegi),
      }),
    guncelle: (id: string, istegi: { url: string; olayFiltresi: string[]; retryProfili: RetryProfili }) =>
      istek<Endpoint>(`/v1/endpointler/${id}`, { method: "PATCH", body: JSON.stringify(istegi) }),
    devreSifirla: (id: string) => istek<void>(`/v1/endpointler/${id}/devre-sifirla`, { method: "POST" }),
    secretRotasyonuBaslat: (id: string) =>
      istek<SecretRotasyonYaniti>(`/v1/endpointler/${id}/secret-rotasyon`, { method: "POST" }),
  },
};
