"use client";

import { useMemo } from "react";
import { Download, TriangleAlert } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { curlOlustur } from "@/lib/curl";
import { AZAMI_BOYUT_BYTE, guvenliAyristir } from "@/lib/guvenli-json";

async function panoyaKopyala(metin: string, basariMesaji: string) {
  await navigator.clipboard.writeText(metin);
  toast.success(basariMesaji);
}

/** Ham gövdeyi `.json` olarak indirir — dosya adı sanitize edilir, tip her zaman octet-stream
 * (§13.D — tip karışması). Kullanıcı çok büyük/şüpheli bir gövdeyi ayrıştırmadan inceleyebilir. */
function hamIndir(ham: string, dosyaAdi: string) {
  const guvenliAd = dosyaAdi.replace(/[^a-zA-Z0-9_.-]/g, "_");
  const blob = new Blob([ham], { type: "application/octet-stream" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = guvenliAd;
  a.click();
  URL.revokeObjectURL(url);
}

export function PayloadInceleyici({ payload, curlHedefUrl }: { payload: string; curlHedefUrl?: string }) {
  // VARSAYIM: §13.D "ayrıştırmayı Web Worker'a taşı" bilinçli olarak ertelendi — boyut
  // (256 KB) ve derinlik (64) sınırları zaten ana thread'i tıkayacak pathological girdiyi
  // engelliyor; kalan risk (256 KB'a kadar meşru ama karmaşık JSON'un senkron parse süresi)
  // gözlemlenebilir bir sorun çıkarsa Worker'a taşınır.
  const sonuc = useMemo(() => guvenliAyristir(payload), [payload]);

  const guzel = sonuc.durum === "basarili" ? JSON.stringify(sonuc.deger, null, 2) : null;

  // Kabuk metakarakteri escape'li (§13.F/13.D — komut enjeksiyonu). Payload üçüncü taraf
  // içeriği (abone endpoint'ine gönderilecek), ham birleştirme yasak.
  const curlKomutu =
    curlHedefUrl && guzel !== null
      ? curlOlustur({ url: curlHedefUrl, basliklar: { "Content-Type": "application/json" }, govde: payload })
      : null;

  return (
    <div className="rounded-md border bg-muted/30">
      <div className="flex items-center justify-between border-b px-3 py-2">
        <span className="text-xs font-medium text-muted-foreground">Payload</span>
        <div className="flex gap-2">
          {guzel !== null && (
            <Button size="sm" variant="ghost" onClick={() => panoyaKopyala(guzel, "Payload kopyalandı")}>
              Kopyala
            </Button>
          )}
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
          <Button size="sm" variant="ghost" onClick={() => hamIndir(payload, "payload.json")}>
            <Download className="size-3.5" />
            Ham indir
          </Button>
        </div>
      </div>

      {sonuc.durum === "basarili" && <pre className="overflow-x-auto p-3 text-xs">{guzel}</pre>}

      {sonuc.durum === "cok-buyuk" && (
        <PayloadUyarisi
          mesaj={`Payload ${Math.round(sonuc.boyutByte / 1024)} KB — görüntülemek için azami boyut ${AZAMI_BOYUT_BYTE / 1024} KB. Ham indirerek inceleyin.`}
        />
      )}
      {sonuc.durum === "cok-derin" && (
        <PayloadUyarisi mesaj="Payload çok derin iç içe geçmiş — görüntülenmiyor. Ham indirerek inceleyin." />
      )}
      {sonuc.durum === "yasakli-anahtar" && (
        <PayloadUyarisi mesaj={`Payload güvenli olmayan bir anahtar içeriyor ("${sonuc.anahtar}") — görüntülenmiyor.`} />
      )}
      {sonuc.durum === "gecersiz-json" && (
        <pre className="overflow-x-auto p-3 text-xs text-muted-foreground">{payload}</pre>
      )}
    </div>
  );
}

function PayloadUyarisi({ mesaj }: { mesaj: string }) {
  return (
    <div className="flex items-start gap-2 p-3 text-xs text-muted-foreground">
      <TriangleAlert className="size-3.5 shrink-0 text-durum-uyari" />
      {mesaj}
    </div>
  );
}
