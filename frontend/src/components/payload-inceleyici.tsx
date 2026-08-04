"use client";

import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { curlOlustur } from "@/lib/curl";

function guzelJson(ham: string): string {
  try {
    return JSON.stringify(JSON.parse(ham), null, 2);
  } catch {
    return ham;
  }
}

async function panoyaKopyala(metin: string, basariMesaji: string) {
  await navigator.clipboard.writeText(metin);
  toast.success(basariMesaji);
}

export function PayloadInceleyici({ payload, curlHedefUrl }: { payload: string; curlHedefUrl?: string }) {
  const guzel = guzelJson(payload);

  // Kabuk metakarakteri escape'li (§13.F/13.D — komut enjeksiyonu). Payload üçüncü taraf
  // içeriği (abone endpoint'ine gönderilecek), ham birleştirme yasak.
  const curlKomutu = curlHedefUrl
    ? curlOlustur({
        url: curlHedefUrl,
        basliklar: { "Content-Type": "application/json" },
        govde: payload,
      })
    : null;

  return (
    <div className="rounded-md border bg-muted/30">
      <div className="flex items-center justify-between border-b px-3 py-2">
        <span className="text-xs font-medium text-muted-foreground">Payload</span>
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={() => panoyaKopyala(guzel, "Payload kopyalandı")}>
            Kopyala
          </Button>
          {curlKomutu && (
            <Button
              size="sm"
              variant="ghost"
              data-tur="teslimat-curl-kopyala"
              onClick={() => panoyaKopyala(curlKomutu, "curl komutu kopyalandı")}
            >
              curl olarak kopyala
            </Button>
          )}
        </div>
      </div>
      <pre className="overflow-x-auto p-3 text-xs">{guzel}</pre>
    </div>
  );
}
