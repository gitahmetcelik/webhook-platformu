"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { api } from "@/lib/api";
import type { Endpoint, RetryProfili } from "@/lib/types";

const RETRY_PROFILLERI: { deger: RetryProfili; etiket: string }[] = [
  { deger: "HIZLI", etiket: "Hızlı (3 deneme)" },
  { deger: "STANDART", etiket: "Standart (8 deneme)" },
  { deger: "UZUN", etiket: "Uzun (15 deneme)" },
];

interface Props {
  uygulamaId: string;
  duzenlenecekEndpoint?: Endpoint;
  acik: boolean;
  onOpenChange: (acik: boolean) => void;
  onOlusturuldu?: (secret: string) => void;
}

export function EndpointFormDialog({ uygulamaId, duzenlenecekEndpoint, acik, onOpenChange, onOlusturuldu }: Props) {
  const queryClient = useQueryClient();
  const [url, setUrl] = useState("");
  const [olayFiltresi, setOlayFiltresi] = useState("");
  const [retryProfili, setRetryProfili] = useState<RetryProfili>("STANDART");

  useEffect(() => {
    if (duzenlenecekEndpoint) {
      setUrl(duzenlenecekEndpoint.url);
      setOlayFiltresi(duzenlenecekEndpoint.olayFiltresi.join(", "));
      setRetryProfili(duzenlenecekEndpoint.retryProfili);
    } else {
      setUrl("");
      setOlayFiltresi("");
      setRetryProfili("STANDART");
    }
  }, [duzenlenecekEndpoint, acik]);

  const filtreDizisi = () =>
    olayFiltresi
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);

  const olusturMutasyonu = useMutation({
    mutationFn: () => api.endpointler.olustur(uygulamaId, { url, olayFiltresi: filtreDizisi(), retryProfili }),
    onSuccess: (yanit) => {
      queryClient.invalidateQueries({ queryKey: ["endpointler"] });
      onOpenChange(false);
      onOlusturuldu?.(yanit.secret);
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  const guncelleMutasyonu = useMutation({
    mutationFn: () =>
      api.endpointler.guncelle(duzenlenecekEndpoint!.id, { url, olayFiltresi: filtreDizisi(), retryProfili }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["endpointler"] });
      toast.success("Endpoint güncellendi");
      onOpenChange(false);
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  const gonderiliyor = olusturMutasyonu.isPending || guncelleMutasyonu.isPending;

  return (
    <Dialog open={acik} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{duzenlenecekEndpoint ? "Endpoint Düzenle" : "Yeni Endpoint"}</DialogTitle>
          <DialogDescription>
            {duzenlenecekEndpoint
              ? "URL, event filtresi ve retry profilini güncelleyin."
              : "Event'lerin gönderileceği hedef URL'i tanımlayın."}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5" data-tur="endpoint-form-url">
            <Label htmlFor="url">Hedef URL</Label>
            <Input id="url" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://..." />
          </div>
          <div className="flex flex-col gap-1.5" data-tur="endpoint-form-filtre">
            <Label htmlFor="olay-filtresi">Olay filtresi (virgülle ayrılmış, boş = tümü)</Label>
            <Input
              id="olay-filtresi"
              value={olayFiltresi}
              onChange={(e) => setOlayFiltresi(e.target.value)}
              placeholder="siparis.olusturuldu, odeme.basarili"
            />
          </div>
          <div className="flex flex-col gap-1.5" data-tur="endpoint-form-retry-profili">
            <Label>Retry profili</Label>
            <Select value={retryProfili} onValueChange={(v) => setRetryProfili(v as RetryProfili)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {RETRY_PROFILLERI.map((p) => (
                  <SelectItem key={p.deger} value={p.deger}>
                    {p.etiket}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button
            disabled={!url || gonderiliyor}
            onClick={() => (duzenlenecekEndpoint ? guncelleMutasyonu.mutate() : olusturMutasyonu.mutate())}
          >
            {gonderiliyor ? "Kaydediliyor…" : duzenlenecekEndpoint ? "Güncelle" : "Oluştur"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
