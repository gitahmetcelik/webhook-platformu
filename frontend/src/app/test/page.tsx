"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAktifUygulama } from "@/components/uygulama-provider";
import { api } from "@/lib/api";

const HAZIR_SENARYOLAR: Record<string, object> = {
  "siparis.olusturuldu": { siparisNo: "A-1001", tutar: 249.9, para_birimi: "TRY" },
  "odeme.basarili": { odemeId: "P-5521", siparisNo: "A-1001", tutar: 249.9 },
  "iade.talep-edildi": { iadeId: "R-77", siparisNo: "A-1001", neden: "musteri-vazgecti" },
};

export default function TestAraciSayfasi() {
  const { uygulama } = useAktifUygulama();
  const router = useRouter();
  const [tip, setTip] = useState("siparis.olusturuldu");
  const [payload, setPayload] = useState(JSON.stringify(HAZIR_SENARYOLAR["siparis.olusturuldu"], null, 2));

  const gonderMutasyonu = useMutation({
    mutationFn: () => {
      let ayrismisPayload: unknown;
      try {
        ayrismisPayload = JSON.parse(payload);
      } catch {
        throw new Error("Payload geçerli bir JSON değil");
      }
      return api.olaylar.olustur(uygulama!.id, { tip, payload: ayrismisPayload }, crypto.randomUUID());
    },
    onSuccess: (yanit) => {
      if (yanit.teslimatIdleri.length === 0) {
        toast.warning("Bu olay tipine abone bir endpoint yok — olay kaydedildi ama teslimat oluşmadı.");
        return;
      }
      toast.success(`${yanit.teslimatSayisi} teslimat oluşturuldu`);
      router.push(`/teslimatlar/${yanit.teslimatIdleri[0]}`);
    },
    onError: (hata: Error) => toast.error(hata.message),
  });

  return (
    <div className="flex max-w-xl flex-col gap-4">
      <h1 className="text-xl font-semibold">Test Aracı</h1>
      <p className="text-sm text-muted-foreground">
        Bir olay tipi seçip payload&apos;ı düzenleyin, göndererek gerçek bir teslimatı tetikleyin.
      </p>

      <div className="flex flex-col gap-1.5">
        <Label>Olay tipi</Label>
        <Select
          value={tip}
          onValueChange={(v) => {
            if (!v) return;
            setTip(v);
            setPayload(JSON.stringify(HAZIR_SENARYOLAR[v] ?? {}, null, 2));
          }}
        >
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {Object.keys(HAZIR_SENARYOLAR).map((s) => (
              <SelectItem key={s} value={s}>
                {s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="payload">Payload (JSON)</Label>
        <Textarea
          id="payload"
          value={payload}
          onChange={(e) => setPayload(e.target.value)}
          rows={10}
          className="font-mono text-sm"
        />
      </div>

      <Button onClick={() => gonderMutasyonu.mutate()} disabled={gonderMutasyonu.isPending || !uygulama}>
        {gonderMutasyonu.isPending ? "Gönderiliyor…" : "Gönder"}
      </Button>
    </div>
  );
}
