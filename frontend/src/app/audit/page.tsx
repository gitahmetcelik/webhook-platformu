"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ClipboardList } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PageHeader } from "@/components/page-header";
import { Sayfalama } from "@/components/sayfalama";
import { api } from "@/lib/api";

export default function AuditSayfasi() {
  const [sayfa, setSayfa] = useState(0);
  const { data, isLoading, isError } = useQuery({
    queryKey: ["audit", sayfa],
    queryFn: () => api.audit.listele({ sayfa }),
  });

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        icon={ClipboardList}
        title="Audit Log"
        description="Operasyonel her aksiyonun izi"
        dataTur="audit-baslik"
      />

      {isError && <p className="text-sm text-destructive">Audit kayıtları yüklenemedi.</p>}

      <Card size="sm">
        <CardContent className="px-0">
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
                    Henüz audit kaydı yok.
                  </TableCell>
                </TableRow>
              )}
              {data?.content.map((kayit) => (
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
        </CardContent>
      </Card>

      {data && <Sayfalama sayfa={sayfa} toplamSayfa={data.totalPages} onDegistir={setSayfa} />}
    </div>
  );
}
