"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { DevreDurumRozeti } from "@/components/devre-durum-rozeti";
import { EndpointFormDialog } from "@/components/endpoint-form-dialog";
import { SecretGosterDialog } from "@/components/secret-goster-dialog";
import { useAktifUygulama } from "@/components/uygulama-provider";
import { api } from "@/lib/api";
import type { Endpoint } from "@/lib/types";

export default function EndpointlerSayfasi() {
  const { uygulama } = useAktifUygulama();
  const queryClient = useQueryClient();
  const [formAcik, setFormAcik] = useState(false);
  const [duzenlenecek, setDuzenlenecek] = useState<Endpoint | undefined>(undefined);
  const [gosterilecekSecret, setGosterilecekSecret] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["endpointler", uygulama?.id],
    queryFn: () => api.endpointler.listele(uygulama!.id),
    enabled: !!uygulama,
  });

  const devreSifirlaMutasyonu = useMutation({
    mutationFn: (id: string) => api.endpointler.devreSifirla(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["endpointler"] });
      toast.success("Devre sıfırlandı");
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Endpoint&apos;ler</h1>
        <Button
          onClick={() => {
            setDuzenlenecek(undefined);
            setFormAcik(true);
          }}
        >
          Yeni Endpoint
        </Button>
      </div>

      {isLoading && <p className="text-sm text-muted-foreground">Yükleniyor…</p>}

      {data && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>URL</TableHead>
              <TableHead>Olay Filtresi</TableHead>
              <TableHead>Retry Profili</TableHead>
              <TableHead>Son 24sa Başarı</TableHead>
              <TableHead>Devre</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  Henüz endpoint yok.
                </TableCell>
              </TableRow>
            )}
            {data.map((endpoint) => (
              <TableRow key={endpoint.id}>
                <TableCell className="font-mono text-sm">{endpoint.url}</TableCell>
                <TableCell>
                  {endpoint.olayFiltresi.length === 0 ? (
                    <span className="text-xs text-muted-foreground">tümü</span>
                  ) : (
                    <div className="flex flex-wrap gap-1">
                      {endpoint.olayFiltresi.map((f) => (
                        <Badge key={f} variant="outline" className="font-mono text-xs">
                          {f}
                        </Badge>
                      ))}
                    </div>
                  )}
                </TableCell>
                <TableCell className="text-sm">{endpoint.retryProfili}</TableCell>
                <TableCell className="text-sm">
                  {endpoint.basariOraniSon24Saat === null ? "—" : `%${endpoint.basariOraniSon24Saat.toFixed(0)}`}
                </TableCell>
                <TableCell>
                  <DevreDurumRozeti durum={endpoint.devreDurumu} />
                </TableCell>
                <TableCell className="flex justify-end gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      setDuzenlenecek(endpoint);
                      setFormAcik(true);
                    }}
                  >
                    Düzenle
                  </Button>
                  {endpoint.devreDurumu === "ACIK" && (
                    <Button
                      size="sm"
                      variant="destructive"
                      disabled={devreSifirlaMutasyonu.isPending}
                      onClick={() => devreSifirlaMutasyonu.mutate(endpoint.id)}
                    >
                      Devreyi Sıfırla
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {uygulama && (
        <EndpointFormDialog
          uygulamaId={uygulama.id}
          duzenlenecekEndpoint={duzenlenecek}
          acik={formAcik}
          onOpenChange={setFormAcik}
          onOlusturuldu={(secret) => setGosterilecekSecret(secret)}
        />
      )}

      <SecretGosterDialog secret={gosterilecekSecret} onOpenChange={() => setGosterilecekSecret(null)} />
    </div>
  );
}
