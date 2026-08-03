"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { api } from "@/lib/api";

export default function AuditSayfasi() {
  const [sayfa, setSayfa] = useState(0);
  const { data, isLoading, isError } = useQuery({
    queryKey: ["audit", sayfa],
    queryFn: () => api.audit.listele({ sayfa }),
  });

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">Audit Log</h1>

      {isError && <p className="text-sm text-destructive">Audit kayıtları yüklenemedi.</p>}
      {isLoading && <p className="text-sm text-muted-foreground">Yükleniyor…</p>}

      {data && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Zaman</TableHead>
              <TableHead>Tür</TableHead>
              <TableHead>Hedef ID</TableHead>
              <TableHead>Detay</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Henüz audit kaydı yok.
                </TableCell>
              </TableRow>
            )}
            {data.content.map((kayit) => (
              <TableRow key={kayit.id}>
                <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                  {new Date(kayit.olusturulma).toLocaleString("tr-TR")}
                </TableCell>
                <TableCell className="font-mono text-sm">{kayit.tur}</TableCell>
                <TableCell className="font-mono text-xs text-muted-foreground">{kayit.hedefId}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{kayit.detay ?? "—"}</TableCell>
              </TableRow>
            ))}
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
