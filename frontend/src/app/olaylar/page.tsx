"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Activity, ArrowRight } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PageHeader } from "@/components/page-header";
import { Sayfalama } from "@/components/sayfalama";
import { useAktifUygulama } from "@/components/uygulama-provider";
import { api } from "@/lib/api";

const YENILEME_ARALIGI_MS = 5000;

export default function OlaylarSayfasi() {
  const { uygulama } = useAktifUygulama();
  const [tipFiltresi, setTipFiltresi] = useState("");
  const [sayfa, setSayfa] = useState(0);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["olaylar", uygulama?.id, tipFiltresi, sayfa],
    queryFn: () => api.olaylar.listele(uygulama!.id, { tip: tipFiltresi || undefined, sayfa }),
    enabled: !!uygulama,
    refetchInterval: YENILEME_ARALIGI_MS,
  });

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        icon={Activity}
        title="Olaylar"
        description="Uygulamanıza gelen event'ler ve tetiklediği teslimatlar"
        action={
          <Input
            data-tur="olaylar-filtre"
            placeholder="Olay tipine göre filtrele…"
            value={tipFiltresi}
            onChange={(e) => {
              setTipFiltresi(e.target.value);
              setSayfa(0);
            }}
            className="w-64"
          />
        }
      />

      {isError && <p className="text-sm text-destructive">Olaylar yüklenemedi.</p>}

      <Card size="sm">
        <CardContent className="px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Zaman</TableHead>
                <TableHead>Olay Tipi</TableHead>
                <TableHead>Dış Kaynak ID</TableHead>
                <TableHead>Teslimat Özeti</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground">
                    Yükleniyor…
                  </TableCell>
                </TableRow>
              )}
              {data?.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground">
                    Henüz olay yok.
                  </TableCell>
                </TableRow>
              )}
              {data?.content.map((olay) => {
                const basarili = olay.teslimatlar.filter((t) => t.durum === "BASARILI").length;
                const toplam = olay.teslimatlar.length;
                return (
                  <TableRow key={olay.id}>
                    <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                      {new Date(olay.olusturulma).toLocaleString("tr-TR")}
                    </TableCell>
                    <TableCell className="font-mono text-sm">{olay.tip}</TableCell>
                    <TableCell className="font-mono text-xs text-muted-foreground">{olay.disKaynakId}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap items-center gap-2">
                        <Badge variant={basarili === toplam ? "default" : "secondary"}>
                          {basarili}/{toplam} başarılı
                        </Badge>
                        {olay.teslimatlar.map((teslimat) => (
                          <Link
                            key={teslimat.id}
                            href={`/teslimatlar/${teslimat.id}`}
                            className="flex items-center gap-0.5 text-xs text-muted-foreground underline-offset-2 hover:text-foreground hover:underline"
                          >
                            teslimat <ArrowRight className="size-3" />
                          </Link>
                        ))}
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {data && <Sayfalama sayfa={sayfa} toplamSayfa={data.totalPages} onDegistir={setSayfa} />}
    </div>
  );
}
