"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
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
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Olaylar</h1>
        <Input
          placeholder="Olay tipine göre filtrele (örn. siparis.olusturuldu)"
          value={tipFiltresi}
          onChange={(e) => {
            setTipFiltresi(e.target.value);
            setSayfa(0);
          }}
          className="max-w-sm"
        />
      </div>

      {isError && <p className="text-sm text-destructive">Olaylar yüklenemedi.</p>}
      {isLoading && <p className="text-sm text-muted-foreground">Yükleniyor…</p>}

      {data && (
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
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Henüz olay yok.
                </TableCell>
              </TableRow>
            )}
            {data.content.map((olay) => {
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
                          className="text-xs text-muted-foreground underline hover:text-foreground"
                        >
                          teslimat →
                        </Link>
                      ))}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button variant="outline" size="sm" disabled={sayfa === 0} onClick={() => setSayfa((s) => s - 1)}>
            Önceki
          </Button>
          <span className="text-sm text-muted-foreground">
            {sayfa + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={sayfa + 1 >= data.totalPages}
            onClick={() => setSayfa((s) => s + 1)}
          >
            Sonraki
          </Button>
        </div>
      )}
    </div>
  );
}
