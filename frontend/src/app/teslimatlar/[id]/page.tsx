"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import { HttpDurumRozeti } from "@/components/http-durum-rozeti";
import { PayloadInceleyici } from "@/components/payload-inceleyici";
import { TeslimatDurumRozeti } from "@/components/teslimat-durum-rozeti";
import { api } from "@/lib/api";

const YENIDEN_GONDERILEBILIR_DURUMLAR = new Set(["DLQ", "KALICI_HATA"]);

function saniyeFarki(a: string, b: string): string {
  const fark = (new Date(b).getTime() - new Date(a).getTime()) / 1000;
  return `${fark.toFixed(1)}sn`;
}

export default function TeslimatDetaySayfasi() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["teslimat", params.id],
    queryFn: () => api.teslimatlar.detay(params.id),
    refetchInterval: (query) =>
      query.state.data?.teslimat.durum === "KUYRUKTA" || query.state.data?.teslimat.durum === "HATALI"
        ? 3000
        : false,
  });

  const yenidenGonderMutasyonu = useMutation({
    mutationFn: () => api.teslimatlar.yenidenGonder(params.id),
    onSuccess: (yeniTeslimat) => {
      toast.success("Yeniden gönderildi");
      queryClient.invalidateQueries({ queryKey: ["olaylar"] });
      router.push(`/teslimatlar/${yeniTeslimat.id}`);
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Yükleniyor…</p>;
  }
  if (isError || !data) {
    return <p className="text-sm text-destructive">Teslimat bulunamadı.</p>;
  }

  const { teslimat, olayTipi, olayPayload, endpointUrl, denemeler, motorGorevOzeti } = data;
  const toplamSureMs = denemeler.reduce((toplam, d) => toplam + (d.sureMs ?? 0), 0);
  const yenidenGonderilebilir = YENIDEN_GONDERILEBILIR_DURUMLAR.has(teslimat.durum);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-3">
            <TeslimatDurumRozeti durum={teslimat.durum} />
            <span className="font-mono text-sm text-muted-foreground">{olayTipi}</span>
          </div>
          <p className="text-sm text-muted-foreground">
            Endpoint: <span className="font-mono">{endpointUrl}</span>
          </p>
          <p className="text-xs text-muted-foreground">
            Toplam işlem süresi: {toplamSureMs}ms · {denemeler.length} deneme
          </p>
          {teslimat.anaTeslimatId && (
            <p className="text-xs text-muted-foreground">
              Yeniden gönderim —{" "}
              <a href={`/teslimatlar/${teslimat.anaTeslimatId}`} className="underline">
                orijinal teslimat
              </a>
            </p>
          )}
          {motorGorevOzeti && (
            <p className="text-xs text-muted-foreground">
              Motor: {motorGorevOzeti.durum} · deneme {motorGorevOzeti.denemeSayisi}
            </p>
          )}
          {/* Teslimatin kendi trace id'si (motorunki degil): devre acikken motor gorevi hic
              olusmadigi icin oradan okunamiyor, ayrica ayni olaydan dogan tum teslimatlarda
              AYNI oldugu icin loglarda tek sorguyla hepsi bulunabiliyor (bkz Faz 5.2). */}
          {teslimat.traceId && (
            <p className="text-xs text-muted-foreground">
              Trace:{" "}
              <button
                type="button"
                className="cursor-pointer font-mono underline"
                title="Panoya kopyala"
                onClick={() => {
                  navigator.clipboard.writeText(teslimat.traceId!);
                  toast.success("Trace id kopyalandı");
                }}
              >
                {teslimat.traceId}
              </button>
            </p>
          )}
        </div>
        {yenidenGonderilebilir && (
          <Button onClick={() => yenidenGonderMutasyonu.mutate()} disabled={yenidenGonderMutasyonu.isPending}>
            {yenidenGonderMutasyonu.isPending ? "Gönderiliyor…" : "Yeniden Gönder"}
          </Button>
        )}
      </div>

      <PayloadInceleyici payload={olayPayload} curlHedefUrl={endpointUrl} />

      <div>
        <h2 className="mb-2 text-sm font-medium">Deneme Timeline&apos;ı</h2>
        {denemeler.length === 0 ? (
          <p className="text-sm text-muted-foreground">Henüz bir deneme yapılmadı.</p>
        ) : (
          <Accordion className="rounded-md border">
            {denemeler.map((deneme, i) => (
              <AccordionItem key={deneme.denemeNo} value={String(deneme.denemeNo)}>
                <AccordionTrigger className="px-3 hover:no-underline">
                  <div className="flex flex-1 items-center gap-4 text-sm">
                    <span className="w-16 text-muted-foreground">#{deneme.denemeNo}</span>
                    <span className="w-40 text-muted-foreground">
                      {new Date(deneme.istekZamani).toLocaleString("tr-TR")}
                    </span>
                    <HttpDurumRozeti durum={deneme.httpDurum} />
                    <span className="text-muted-foreground">{deneme.sureMs ?? "—"}ms</span>
                    {i < denemeler.length - 1 && (
                      <span className="text-xs text-muted-foreground">
                        sonraki denemeye kadar: {saniyeFarki(deneme.istekZamani, denemeler[i + 1].istekZamani)}
                      </span>
                    )}
                  </div>
                </AccordionTrigger>
                <AccordionContent className="px-3">
                  <div className="flex flex-col gap-2 text-xs">
                    {deneme.hata && (
                      <p className="text-destructive">Hata: {deneme.hata}</p>
                    )}
                    {deneme.yanitGovdesi && (
                      <div>
                        <p className="mb-1 font-medium text-muted-foreground">Alınan yanıt gövdesi</p>
                        <pre className="overflow-x-auto rounded bg-muted/50 p-2">{deneme.yanitGovdesi}</pre>
                      </div>
                    )}
                    <p className="text-muted-foreground">
                      İstek, endpoint&apos;in imza secret&apos;ıyla HMAC-SHA256 imzalanarak gönderildi
                      (X-Webhook-Id/Timestamp/Signature başlıkları — her deneme için yeniden üretilir, geçmiş
                      değerleri saklanmaz).
                    </p>
                  </div>
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>
        )}
      </div>
    </div>
  );
}
