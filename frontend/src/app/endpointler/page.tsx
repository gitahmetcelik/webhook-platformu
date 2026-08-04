"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { KeyRound, Plus, RotateCcw, Radio, SquarePen } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PageHeader } from "@/components/page-header";
import { DevreDurumRozeti } from "@/components/devre-durum-rozeti";
import { EndpointFormDialog } from "@/components/endpoint-form-dialog";
import { SaglikSkoru } from "@/components/saglik-skoru";
import { SecretGosterDialog } from "@/components/secret-goster-dialog";
import { useAktifUygulama } from "@/components/uygulama-provider";
import { useAuth } from "@/components/auth-provider";
import { api } from "@/lib/api";
import { turTamamlandiMi } from "@/lib/tur";
import type { Endpoint } from "@/lib/types";

export default function EndpointlerSayfasi() {
  const { uygulama } = useAktifUygulama();
  const { organizasyon } = useAuth();
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
      toast.success("Devre sıfırlandı, bekleyen teslimatlar kuyruğa alındı");
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  const secretRotasyonMutasyonu = useMutation({
    mutationFn: (id: string) => api.endpointler.secretRotasyonuBaslat(id),
    onSuccess: (yanit) => {
      setGosterilecekSecret(yanit.secret);
      // §10.6 — "ilk tıklamada" görev tetikli tur: daha önce görülmediyse başlatılır.
      if (organizasyon && !turTamamlandiMi(organizasyon.id, "secret-rotasyonu")) {
        window.dispatchEvent(new CustomEvent("webhook-platformu:tur-baslat", { detail: "secret-rotasyonu" }));
      }
      toast.success("Yeni secret üretildi — eski secret 24 saat daha geçerli");
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        icon={Radio}
        title="Endpoint'ler"
        description="Abone endpoint'leriniz, sağlık durumları ve devre kesici"
        action={
          <Button
            data-tur="endpoint-yeni-buton"
            onClick={() => {
              setDuzenlenecek(undefined);
              setFormAcik(true);
            }}
          >
            <Plus className="size-4" />
            Yeni Endpoint
          </Button>
        }
      />

      <Card size="sm">
        <CardContent className="overflow-x-auto px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>URL</TableHead>
                <TableHead>Olay Filtresi</TableHead>
                <TableHead>Retry Profili</TableHead>
                <TableHead>Son 24sa Başarı</TableHead>
                <TableHead>Sağlık Skoru</TableHead>
                <TableHead>Devre</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground">
                    Yükleniyor…
                  </TableCell>
                </TableRow>
              )}
              {data?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-muted-foreground">
                    Henüz endpoint yok.
                  </TableCell>
                </TableRow>
              )}
              {data?.map((endpoint, i) => (
                <TableRow key={endpoint.id} data-tur={i === 0 ? "endpoint-satir" : undefined}>
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
                  <TableCell className="text-sm">
                    <Badge variant="secondary">{endpoint.retryProfili}</Badge>
                  </TableCell>
                  <TableCell className="text-sm tabular-nums">
                    {endpoint.basariOraniSon24Saat === null ? "—" : `%${endpoint.basariOraniSon24Saat.toFixed(0)}`}
                  </TableCell>
                  <TableCell className="text-sm" data-tur={i === 0 ? "endpoint-saglik-skoru" : undefined}>
                    <SaglikSkoru
                      skor={endpoint.saglikSkoru}
                      uyariAktif={endpoint.saglikUyarisiAktif}
                      ortalamaGecikmeMs={endpoint.ortalamaGecikmeMs}
                    />
                  </TableCell>
                  <TableCell data-tur={i === 0 ? "endpoint-devre-rozeti" : undefined}>
                    <DevreDurumRozeti durum={endpoint.devreDurumu} />
                  </TableCell>
                  <TableCell className="flex justify-end gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      data-tur={i === 0 ? "endpoint-duzenle" : undefined}
                      onClick={() => {
                        setDuzenlenecek(endpoint);
                        setFormAcik(true);
                      }}
                    >
                      <SquarePen className="size-4" />
                      Düzenle
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      data-tur={i === 0 ? "endpoint-secret-rotasyon" : undefined}
                      disabled={secretRotasyonMutasyonu.isPending}
                      onClick={() => secretRotasyonMutasyonu.mutate(endpoint.id)}
                    >
                      <KeyRound className="size-4" />
                      Secret Rotasyonu
                    </Button>
                    {endpoint.devreDurumu === "ACIK" && (
                      <Button
                        size="sm"
                        variant="destructive"
                        data-tur={i === 0 ? "endpoint-devre-sifirla" : undefined}
                        disabled={devreSifirlaMutasyonu.isPending}
                        onClick={() => devreSifirlaMutasyonu.mutate(endpoint.id)}
                      >
                        <RotateCcw className="size-4" />
                        Devreyi Sıfırla
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

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
