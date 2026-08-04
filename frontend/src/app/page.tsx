"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Activity, AlertTriangle, Radio, Send, ShieldCheck, Webhook } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useAuth } from "@/components/auth-provider";
import { useAktifUygulama } from "@/components/uygulama-provider";
import { api } from "@/lib/api";

function IstatistikKarti({
  icon: Icon,
  etiket,
  deger,
  vurgu,
}: {
  icon: React.ComponentType<{ className?: string }>;
  etiket: string;
  deger: React.ReactNode;
  vurgu?: "iyi" | "kotu";
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4">
        <div
          className={
            "flex size-11 shrink-0 items-center justify-center rounded-lg " +
            (vurgu === "kotu"
              ? "bg-destructive/10 text-destructive"
              : vurgu === "iyi"
                ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                : "bg-primary/10 text-primary")
          }
        >
          <Icon className="size-5" />
        </div>
        <div>
          <p className="text-2xl font-semibold tabular-nums leading-tight">{deger}</p>
          <p className="text-sm text-muted-foreground">{etiket}</p>
        </div>
      </CardContent>
    </Card>
  );
}

export default function AnaSayfa() {
  const { organizasyon } = useAuth();
  const { uygulama } = useAktifUygulama();

  const { data: endpointler } = useQuery({
    queryKey: ["endpointler", uygulama?.id],
    queryFn: () => api.endpointler.listele(uygulama!.id),
    enabled: !!uygulama,
  });

  const { data: sonOlaylar } = useQuery({
    queryKey: ["olaylar-ozet", uygulama?.id],
    queryFn: () => api.olaylar.listele(uygulama!.id, { boyut: 5 }),
    enabled: !!uygulama,
  });

  const acikDevreSayisi = endpointler?.filter((e) => e.devreDurumu === "ACIK").length ?? 0;
  const uyariliEndpointSayisi = endpointler?.filter((e) => e.saglikUyarisiAktif).length ?? 0;
  const kotaOrani = organizasyon
    ? Math.min(100, Math.round((organizasyon.buAyKullanim / organizasyon.aylikKota) * 100))
    : 0;

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center gap-3">
        <span className="flex size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          <Webhook className="size-5" />
        </span>
        <div>
          <h1 className="text-xl font-semibold tracking-tight">
            {organizasyon ? `Merhaba, ${organizasyon.ad}` : "Genel Bakış"}
          </h1>
          <p className="text-sm text-muted-foreground">
            {uygulama ? uygulama.ad : "—"} uygulamasının teslimat sağlığı
          </p>
        </div>
        <Link href="/test" className={cn(buttonVariants(), "ml-auto")}>
          <Send className="size-4" />
          Test Event Gönder
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <IstatistikKarti
          icon={Activity}
          etiket="Bu ay teslimat"
          deger={organizasyon ? `${organizasyon.buAyKullanim} / ${organizasyon.aylikKota}` : "—"}
          vurgu={kotaOrani >= 90 ? "kotu" : undefined}
        />
        <IstatistikKarti icon={Radio} etiket="Toplam endpoint" deger={endpointler?.length ?? "—"} />
        <IstatistikKarti
          icon={ShieldCheck}
          etiket="Açık devre"
          deger={acikDevreSayisi}
          vurgu={acikDevreSayisi > 0 ? "kotu" : "iyi"}
        />
        <IstatistikKarti
          icon={AlertTriangle}
          etiket="Sağlık uyarısı"
          deger={uyariliEndpointSayisi}
          vurgu={uyariliEndpointSayisi > 0 ? "kotu" : "iyi"}
        />
      </div>

      <Card>
        <CardHeader className="flex-row items-center justify-between">
          <CardTitle>Son olaylar</CardTitle>
          <Link href="/olaylar" className={cn(buttonVariants({ variant: "ghost", size: "sm" }))}>
            Tümünü gör →
          </Link>
        </CardHeader>
        <CardContent className="flex flex-col divide-y px-0">
          {(!sonOlaylar || sonOlaylar.content.length === 0) && (
            <p className="px-4 py-6 text-center text-sm text-muted-foreground">Henüz olay yok.</p>
          )}
          {sonOlaylar?.content.map((olay) => {
            const basarili = olay.teslimatlar.filter((t) => t.durum === "BASARILI").length;
            const toplam = olay.teslimatlar.length;
            return (
              <div key={olay.id} className="flex items-center justify-between px-4 py-3 text-sm">
                <div className="flex flex-col gap-0.5">
                  <span className="font-mono">{olay.tip}</span>
                  <span className="text-xs text-muted-foreground">
                    {new Date(olay.olusturulma).toLocaleString("tr-TR")}
                  </span>
                </div>
                <span
                  className={
                    basarili === toplam ? "text-emerald-600 dark:text-emerald-400" : "text-muted-foreground"
                  }
                >
                  {basarili}/{toplam} başarılı
                </span>
              </div>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}
